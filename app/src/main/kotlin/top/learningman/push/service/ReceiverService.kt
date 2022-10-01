package top.learningman.push.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.util.Log
import com.microsoft.appcenter.crashes.Crashes
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.serialization.json.Json
import top.learningman.push.BuildConfig
import top.learningman.push.Constant
import top.learningman.push.data.Repo
import top.learningman.push.entity.JSONMessageItem
import top.learningman.push.entity.Message
import java.util.*


class ReceiverService : NotificationListenerService() {
    init {
        Log.d(TAG, "ReceiverService init")
    }

    private lateinit var manager: WebsocketSessionManager

    override fun onCreate() {
        super.onCreate()
        manager = WebsocketSessionManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand executed with startId: $startId")
        if (intent != null) {
            Log.d(TAG, "using an intent with action ${intent.action}")
            when (intent.action) {
                Action.UPDATE.name -> {
                    val nextUserID =
                        intent.getStringExtra(INTENT_USERID_KEY)
                    if (!nextUserID.isNullOrEmpty()) {
                        manager.updateUserID(nextUserID)
                    }
                }
                else -> {
                    Log.d("ReceiverService", "Unknown action ${intent.action}")
                    manager.tryResume()
                }
            }
        } else {
            Log.d(TAG, "with a null intent. It has been probably restarted by the system.")
            manager.tryResume()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, ReceiverService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        applicationContext.getSystemService(Context.ALARM_SERVICE)
        val alarmService =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "onListenerDisconnected")
        if (!BuildConfig.DEBUG) {
            requestRebind(
                ComponentName(
                    applicationContext,
                    NotificationListenerService::class.java
                )
            )
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private class WebsocketSessionManager(private val context: Context) :
        CoroutineScope by CoroutineScope(context = newSingleThreadContext("WebsocketSessionManager")) {

        private var job: Job? = null
        private val repo by lazy { Repo.getInstance(context) }
        private var currentUserID = repo.getUser()
        private val client by lazy {
            HttpClient(CIO) {
                install(WebSockets)
                install(HttpRequestRetry)
            }
        }

        private var status = Status.WAIT_START

        object Status {
            const val WAIT_START = 0
            const val RUNNING = 1
            const val WAIT_RECONNECT = 2
            const val NETWORK_LOST = 3
        }

        init {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    if (status == Status.NETWORK_LOST) {
                        Log.d(TAG, "network available, try start websocket")
                        Log.d(TAG, "resume from network lost")
                        status = Status.WAIT_RECONNECT
                        tryResume()
                    }
                }

                override fun onLost(network: android.net.Network) {
                    super.onLost(network)
                    Log.d(TAG, "network lost, stop websocket")
                    status = Status.NETWORK_LOST
                    runBlocking {
                        job?.cancelAndJoin()
                        job = null
                    }
                }
            })
        }

        fun tryResume() {
            Log.d(TAG, "tryResume with status $status at ${Date()}")
            if (status != Status.RUNNING) {
                launch { connect() }
            }
        }

        private fun recover() {
            Log.d(TAG, "recover at ${Date()}")
            status = Status.WAIT_RECONNECT
            launch { connect() }
        }

        private suspend fun connect() {
            if (job != null) {
                Log.d(TAG, "job is not null, cancel it")
                job?.cancelAndJoin()
                job = null
            }

            val session = runCatching {
                client.webSocketSession(urlString = "${Constant.API_WS_ENDPOINT}/${currentUserID}/host/conn")
                {
                    header("X-Message-Since", repo.getLastMessageTime())
                }
            }.also {
                if (it.isFailure) {
                    Log.e(TAG, "getSession:", it.exceptionOrNull())
                }
            }.getOrNull()
            if (session != null) {
                Log.d(TAG, "session is not null, launch WebSocket")
                job = session.launch(coroutineContext) {
                    while (true) {
                        val frameRet = session.incoming.receiveCatching()
                        frameRet.onClosed {
                            Log.d(TAG, "onClosed")
                            Log.e(TAG, "WebSocket closed", it)
                            recover()
                            return@launch
                        }.onFailure {
                            if (it is CancellationException) {
                                Log.d(TAG, "This job is cancelled")
                                return@launch
                            } else {
                                Log.d(TAG, "onFailure")
                                Log.e(TAG, "WebSocket error", it)
                                recover()
                                return@launch
                            }
                        }.onSuccess {
                            Log.d(TAG, "onSuccess receive frame")
                            handleFrame(it)
                        }
                    }
                }
            }
        }

        private fun handleFrame(frame: Frame) {
            when (frame) {
                is Frame.Text -> {
                    val message = frame.readText()
                    Log.d(TAG, "handleFrame: $message")
                    runCatching {
                        return@runCatching Json.decodeFromString(
                            JSONMessageItem.serializer(),
                            message
                        )
                    }.fold({
                        Log.d(TAG, "prepare to send notification")
                        val notificationMessage = it.toMessage()
                        notifyMessage(notificationMessage)
                        repo.setLastMessageTime(notificationMessage.createdAt)
                    }, {
                        Log.e(TAG, "Error parsing message", it)
                        Crashes.trackError(it)
                    })
                }
                else -> {
                    Log.e(TAG, "Received unexpected frame: ${frame.frameType.name}")
                }
            }
        }

        private fun notifyMessage(message: Message, from: String = "anonymous") {
            Log.d(TAG, "notifyMessage: $message from $from")
            Utils.notifyMessage(context, message)
        }

        fun updateUserID(nextUserID: String) {
            currentUserID = nextUserID
            launch { connect() }
        }
    }


    enum class Action {
        UPDATE
    }

    companion object {
        const val TAG = "ReceiverService"

        const val INTENT_USERID_KEY = "nextUserID"
    }
}
