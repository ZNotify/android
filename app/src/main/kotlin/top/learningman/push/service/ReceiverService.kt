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
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import top.learningman.push.BuildConfig
import top.learningman.push.Constant
import top.learningman.push.data.Repo
import top.learningman.push.entity.JSONMessageItem
import top.learningman.push.entity.Message
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


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
        manager.tryResume()
        Log.d(TAG, "onListenerConnected")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private class WebsocketSessionManager(private val context: Context) :
        CoroutineScope by CoroutineScope(context = newSingleThreadContext("WebsocketSessionManager")) {

        private var job: Job? = null
        private var jobLock = Mutex()
        private val repo by lazy { Repo.getInstance(context) }
        private var currentUserID = repo.getUser()
        private val client by lazy {
            HttpClient(OkHttp) {
                install(WebSockets)
                install(HttpRequestRetry)
            }
        }

        private var status = AtomicInteger(Status.WAIT_START)


        object Status {
            const val WAIT_START = 0
            const val CONNECTING = 1
            const val RUNNING = 2
            const val WAIT_RECONNECT = 3
            const val NETWORK_LOST = 4
            const val INVALID = 5

            fun valueOf(value: Int): String {
                return when (value) {
                    WAIT_START -> "WAIT_START"
                    CONNECTING -> "CONNECTING"
                    RUNNING -> "RUNNING"
                    WAIT_RECONNECT -> "WAIT_RECONNECT"
                    NETWORK_LOST -> "NETWORK_LOST"
                    INVALID -> "INVALID"
                    else -> "UNKNOWN"
                }
            }
        }

        init {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    if (status.compareAndSet(Status.NETWORK_LOST, Status.WAIT_RECONNECT)) {
                        Log.d(TAG, "network available, try start websocket")
                        Log.d(TAG, "resume from network lost")
                        tryResume()
                    } else {
                        Log.d(TAG, "network available, but not in network lost status")
                    }
                }

                override fun onLost(network: android.net.Network) {
                    super.onLost(network)
                    Log.d(TAG, "network lost, stop websocket")
                    status.set(Status.NETWORK_LOST)
                    runBlocking {
                        jobLock.lock()
                        job?.cancelAndJoin()
                        job = null
                        jobLock.unlock()
                    }
                }
            })
        }

        private fun diagnose() {
            Log.d(TAG, "current thread ${Thread.currentThread().id}")
        }

        fun tryResume() {
            Log.d(TAG, "tryResume with status ${Status.valueOf(status.get())} at ${Date()}")
            if (status.compareAndSet(Status.WAIT_START, Status.CONNECTING)) {
                launch { connect() }
                Log.d(TAG, "tryResume: start websocket from WAIT_START, initial startup")
            } else if (status.compareAndSet(Status.WAIT_RECONNECT, Status.CONNECTING)) {
                launch { connect() }
                Log.d(TAG, "tryResume: start websocket from WAIT_RECONNECT")
            } else {
                Log.d(TAG, "tryResume: not start websocket from status ${status.get()}")
            }
        }

        fun updateUserID(nextUserID: String) {
            if (currentUserID != nextUserID) {
                currentUserID = nextUserID
                launch {
                    jobLock.lock()
                    job?.cancelAndJoin()
                    job = null
                    jobLock.unlock()
                    connect()
                }
            } else {
                tryResume()
            }
        }

        private fun recover() {
            Log.d(TAG, "call recover at ${Date()}")
            if (status.compareAndSet(Status.WAIT_RECONNECT, Status.CONNECTING)) {
                launch {
                    jobLock.lock()
                    job?.cancelAndJoin()
                    job = null
                    jobLock.unlock()
                    connect()
                }
            } else {
                Log.d(TAG, "recover: not start websocket from status ${status.get()}")
            }
        }

        private suspend fun connect() {
            diagnose()
            jobLock.lock()
            if (job != null) {
                Log.d(TAG, "job is not null, cancel it, not clear")
                return
            }
            jobLock.unlock()

            if (status.get() == Status.INVALID) {
                Log.d(TAG, "status is invalid, should not connect")
                return
            }

            if (!status.compareAndSet(Status.CONNECTING, Status.RUNNING)) {
                Log.d(
                    TAG,
                    "connect: not connecting status,is ${Status.valueOf(status.get())}"
                )
                return
            } else {
                Log.d(TAG, "connect: start websocket from CONNECTING")
            }

            val session = runCatching {
                client.webSocketSession(urlString = "${Constant.API_WS_ENDPOINT}/${currentUserID}/host/conn")
                {
                    header("X-Message-Since", repo.getLastMessageTime())
                }
            }.also {
                if (it.isFailure) {
                    Log.e(TAG, "getSession:", it.exceptionOrNull())
                    val exception = it.exceptionOrNull() ?: return@also
                    val message = exception.message ?: return@also
                    if (message.contains("401")) {
                        status.set(Status.INVALID)
                        Log.d(TAG, "connect: invalid status, seems userid not correct")
                        return@also
                    }
                }

            }
                .getOrNull()

            if (session != null) {
                Log.d(TAG, "session is not null, launch WebSocket")
                jobLock.lock()
                job = session.launch(coroutineContext) {
                    while (true) {
                        val frameRet = session.incoming.receiveCatching()
                        frameRet.onClosed {
                            Log.d(TAG, "onClosed")
                            Log.e(TAG, "WebSocket closed", it)
                            if (status.compareAndSet(Status.RUNNING, Status.WAIT_RECONNECT)) {
                                recover()
                            } else {
                                Log.d(TAG, "onClosed: not recover from status ${status.get()}")
                            }
                            return@launch
                        }.onFailure {
                            if (it is CancellationException) {
                                Log.d(TAG, "This job is cancelled")
                                return@launch
                            } else {
                                Log.d(TAG, "onFailure")
                                Log.e(TAG, "WebSocket error", it)
                                // Failure not means the connection is closed
                                // So we don't need to recover
                                it?.let { e ->
                                    Crashes.trackError(
                                        e,
                                        mutableMapOf("loc" to "Websocket Failure"),
                                        null
                                    )
                                }
                            }
                        }.onSuccess {
                            Log.d(TAG, "onSuccess receive frame")
                            handleFrame(it)
                        }
                    }
                }
                if (job?.isActive == true) {
                    Log.d(TAG, "job is active, start websocket success")
                } else {
                    Log.d(TAG, "job is not active, start websocket failed")
                }
                jobLock.unlock()
            } else {
                delay(5000)
                if (status.compareAndSet(Status.RUNNING, Status.WAIT_RECONNECT)) {
                    recover()
                } else {
                    Log.d(TAG, "connect: not recover from status ${Status.valueOf(status.get())}")
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
    }


    enum class Action {
        UPDATE
    }

    companion object {
        const val TAG = "ReceiverService"

        const val INTENT_USERID_KEY = "nextUserID"
    }
}
