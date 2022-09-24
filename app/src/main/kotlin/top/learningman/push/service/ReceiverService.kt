package top.learningman.push.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import top.learningman.push.Constant
import top.learningman.push.application.MainApplication
import top.learningman.push.data.Repo
import top.learningman.push.entity.JSONMessageItem
import top.learningman.push.entity.Message

class ReceiverService : NotificationListenerService() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }
    private val repo by lazy { (application as MainApplication).repo }
    private var wsSession: WebSocketSession? = null

    private var errWait = 1
    private fun nextWaitTime(): Int {
        if (errWait < 60) {
            errWait *= 2
        }
        return errWait
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand executed with startId: $startId")
        if (intent != null) {
            Log.d(TAG, "using an intent with action ${intent.action}")
            when (intent.action) {
                Action.UPDATE.name -> receiving(true)
                else -> {}
            }
        } else {
            Log.d(TAG, "with a null intent. It has been probably restarted by the system.")
        }
        return START_STICKY // restart if system kills the service
    }


    override fun onDestroy() {
        Log.d(TAG, "Subscriber service has been destroyed")
        stopService()
        super.onDestroy()
    }

    private fun startService() {
        if (isServiceStarted) {
            receiving()
        }
        Log.d(TAG, "Starting the foreground service task")
        isServiceStarted = true
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        }
        receiving()
        status = ServiceState.RUNNING
    }


    @OptIn(DelicateCoroutinesApi::class)
    private fun receiving(update: Boolean = false) {
        val userID = repo.getUser()
        if (userID == Repo.PREF_USER_DEFAULT) {
            return
        }
        if ((wsSession != null && wsSession?.isActive == true) && !update) {
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            wsSession?.close()
            try {
                wsSession = client
                    .webSocketSession(urlString = "${Constant.API_WS_ENDPOINT}/${userID}/host/conn")
                    {
                        header("X-Message-Since", repo.getLastMessageTime())
                    }
                wsSession?.let {
                    errWait = 1
                    Log.i("WebSocket", "Websocket Connected")
                    while (true) {
                        when (val frame = it.incoming.receive()) {
                            is Frame.Text -> {
                                val rawMessage = frame.readText()
                                Log.d(TAG, "Received: $rawMessage")
                                try {
                                    val jsonMessage = Json.decodeFromString(
                                        JSONMessageItem.serializer(),
                                        rawMessage
                                    )
                                    val message = jsonMessage.toMessage()
                                    notifyMessage(message, "receiverService")
                                    repo.setLastMessageTime(message.createdAt)
                                } catch (e: Exception) {
                                    Log.d(TAG, "Json Decode Error: $e")
                                    continue
                                }
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.stackTraceToString()}")
                Log.d(TAG, "Reconnecting")
                wsSession?.close()
                wsSession = null
                val waitTime = nextWaitTime()
                Log.w(TAG, "Waiting $waitTime seconds to recover from error.")
                delay(waitTime.toLong() * 1000)
                Log.i(TAG, "Recovering from error.")
                receiving()
                return@launch
            }
        }
    }

    private fun stopService() {
        try {
            wakeLock?.let {
                // Release all acquire()
                while (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
            stopSelf()
        } catch (e: Exception) {
            Log.d(TAG, "Service stopped without being started: ${e.message}")
        }

        isServiceStarted = false
        status = ServiceState.STOP
        stopSelf()
    }


    private fun notifyMessage(message: Message, from: String = "anonymous") {
        Log.d(TAG, "notifyMessage: $message from $from")
        wakeLock?.acquire(NOTIFICATION_RECEIVED_WAKELOCK_TIMEOUT_MILLIS)
        Utils.notifyMessage(this, message)
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, ReceiverService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        );
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        );
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "onListenerDisconnected")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected")
    }


    enum class Action {
        UPDATE
    }

    enum class ServiceState {
        RUNNING,
        STOP,
        ERROR,
    }

    companion object {
        const val TAG = "ReceiverService"

        private const val WAKE_LOCK_TAG = "ReceiverService:lock"
        private const val NOTIFICATION_RECEIVED_WAKELOCK_TIMEOUT_MILLIS = 10 * 60 * 1000L
        private const val SHARED_PREFS_ID = "ReceiverService"
        private const val SHARED_PREFS_SERVICE_STATE = "ServiceState"

        var status: ServiceState = ServiceState.STOP

    }
}
