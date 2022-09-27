package top.learningman.push.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
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
import kotlinx.serialization.json.Json
import top.learningman.push.Constant
import top.learningman.push.data.Repo
import top.learningman.push.entity.JSONMessageItem
import top.learningman.push.entity.Message


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
                else -> manager.start()
            }
        } else {
            Log.d(TAG, "with a null intent. It has been probably restarted by the system.")
            manager.start()
        }
        return START_STICKY
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
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
//                install(HttpRequestRetry) {
//                    retryOnServerErrors(maxRetries = 3)
//                    exponentialDelay()
//                    retryIf { _, response ->
//                        response.status != HttpStatusCode.Unauthorized
//                    }
//                }
            }
        }

        private val wakeLock: PowerManager.WakeLock by lazy {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            }
        }

        fun update(userID: String) {
            Log.d(TAG, "update: $userID")
            if (userID == currentUserID) {
                Log.d(TAG, "update: same user, ignore")
                return
            }
            scope.launch {
                if (job != null) {
                    if (job?.isActive == true) {
                        job?.cancel()
                    }
                }
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
            return client.webSocketSession(urlString = "${Constant.API_WS_ENDPOINT}/${nextUserID}/host/conn")
            {
                header("X-Message-Since", repo.getLastMessageTime())
            }
        }

        fun start() {
            Log.d(TAG, "Try to start websocket session")
            if (job != null) {
                if (job?.isActive == true) {
                    return
                }
            }

            scope.launch {
                val session = getSession()
                start(session)
            }
        }

        private fun start(session: WebSocketSession?) {
            if (session == null) {
                return
            }

            fun retry() {
                val userID = repo.getUser()
                update(userID)
            }

            job = scope.launch {
                Log.d(TAG, "Start websocket session")
                while (isActive) {
                    val frameRet = session.incoming.receiveCatching()
                    frameRet.onClosed {
                        Log.d(TAG, "onClosed")
                        Log.e(TAG, "WebSocket closed", it)
                        retry()
                    }.onFailure {
                        if (it is CancellationException) {
                            Log.d(TAG, "This job is cancelled")
                            return@onFailure
                        } else {
                            Log.d(TAG, "onFailure")
                            Log.e(TAG, "WebSocket error", it)
                            retry()
                        }
                    }.onSuccess {
                        Log.d(TAG, "onSuccess receive frame")
                        handleFrame(it)
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
