package top.learningman.push.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.util.Log
import com.microsoft.appcenter.crashes.Crashes
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
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
                        intent.getStringExtra(INTENT_USERID_KEY) ?: Repo.PREF_USER_DEFAULT
                    manager.update(nextUserID)
                }
                else -> {
                    Log.d("ReceiverService", "Unknown action ${intent.action}")
                    manager.start()
                }
            }
        } else {
            Log.d(TAG, "with a null intent. It has been probably restarted by the system.")
            manager.start()
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
        requestRebind(ComponentName(this, NotificationListenerService::class.java))
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected")
    }

    private class WebsocketSessionManager(private val context: Context) {
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        private var job: Job? = null
        private var currentUserID = Repo.PREF_USER_DEFAULT
        private val repo by lazy { Repo.getInstance(context) }
        private val client by lazy {
            HttpClient(OkHttp) {
                install(WebSockets)
            }
        }

        init {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    if (statusLock.compareAndSet(3, 1)) {
                        Log.d(TAG, "network available, try start websocket")
                        Log.d(TAG, "resume from network lost")
                        start()
                    }
                }

                override fun onLost(network: android.net.Network) {
                    super.onLost(network)
                    Log.d(TAG, "network lost, stop websocket")
                    statusLock.set(3)
                    scope.launch {
                        jobLock.lock()
                        job?.cancelAndJoin()
                        jobLock.unlock()
                    }

                }
            })
        }

        private val statusLock = AtomicInteger(0)
        // 0 not started
        // 1 try to start
        // 2 running
        // 3 stop and wait for network

        private val jobLock = Mutex()

        private val wakeLock: PowerManager.WakeLock by lazy {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            }
        }

        fun update(userID: String, isRetry: Boolean = false) {
            Log.d(TAG, "update: $userID")
            if (userID == currentUserID && !isRetry) {
                Log.d(TAG, "update: same user, ignore")
                return
            }
            if (isRetry) {
                Log.d(TAG, "update: retry on ${Date()}")
            }
            scope.launch {
                jobLock.lock()
                if (job != null) {
                    if (job?.isActive == true) {
                        job?.cancelAndJoin()
                    }
                }
                jobLock.unlock()
                val nextSession = getSession(userID)
                start(nextSession)
            }
        }

        suspend fun getSession(userID: String? = null): WebSocketSession? {
            var nextUserID = userID
            if (nextUserID == null) {
                nextUserID = repo.getUser()
            }
            if (nextUserID == Repo.PREF_USER_DEFAULT) {
                return null
            }

            currentUserID = nextUserID
            return runCatching {
                client.webSocketSession(urlString = "${Constant.API_WS_ENDPOINT}/${nextUserID}/host/conn")
                {
                    header("X-Message-Since", repo.getLastMessageTime())
                }
            }.also {
                if (it.isFailure) {
                    Log.e(TAG, "getSession: ", it.exceptionOrNull())
                }
            }.getOrNull()
        }

        fun start() {
            scope.launch {
                jobLock.lock()
                if (job != null) {
                    if (job?.isActive == true) {
                        return@launch
                    }
                }
                jobLock.unlock()

                val session = getSession()
                start(session)
            }
        }

        private suspend fun start(session: WebSocketSession?) {
            if (session == null) {
                return
            }

            if (statusLock.compareAndSet(0, 1)) {
                Log.d(TAG, "start: initial start")
            }

            Log.d(TAG, "Try to start websocket session")

            val status = statusLock.get()
            if (status != 1) {
                Log.d(TAG, "start: status is $status, should not listen")
                return
            }

            suspend fun retry() {
                val userID = repo.getUser()
                if (!statusLock.compareAndSet(2, 1)) {
                    Log.d(TAG, "retry: status is not running, should not retry")
                    return
                }

                Log.d(TAG, "retry: $userID")
                Log.d(TAG, "Wait 5s to retry")
                delay(5000)
                Log.d(TAG, "Retry on ${Date()}")

                update(userID, isRetry = true)
            }

            jobLock.lock()
            job = scope.launch {
                Log.d(TAG, "Start websocket session")
                while (true) {
                    val frameRet = session.incoming.receiveCatching()
                    frameRet.onClosed {
                        Log.d(TAG, "onClosed")
                        Log.e(TAG, "WebSocket closed", it)
                        scope.launch {
                            retry()
                        }
                        return@launch
                    }.onFailure {
                        if (it is CancellationException) {
                            Log.d(TAG, "This job is cancelled")
                            return@launch
                        } else {
                            Log.d(TAG, "onFailure")
                            Log.e(TAG, "WebSocket error", it)
                            scope.launch {
                                retry()
                            }
                            return@launch
                        }
                    }.onSuccess {
                        Log.d(TAG, "onSuccess receive frame")
                        handleFrame(it)
                    }
                }
            }
            statusLock.set(2)
            jobLock.unlock()
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
                    Log.d(TAG, "Received unexpected frame: ${frame.frameType.name}")
                }
            }
        }

        private fun notifyMessage(message: Message, from: String = "anonymous") {
            Log.d(TAG, "notifyMessage: $message from $from")
            wakeLock.acquire(NOTIFICATION_RECEIVED_WAKELOCK_TIMEOUT_MILLIS)
            Utils.notifyMessage(context, message)
            wakeLock.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        }
    }


    enum class Action {
        UPDATE
    }

    companion object {
        const val TAG = "ReceiverService"

        private const val WAKE_LOCK_TAG = "ReceiverService:lock"
        private const val NOTIFICATION_RECEIVED_WAKELOCK_TIMEOUT_MILLIS = 10 * 60 * 1000L
        const val INTENT_USERID_KEY = "nextUserID"
    }
}
