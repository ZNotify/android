package top.learningman.push.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
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
import top.learningman.push.Constant
import top.learningman.push.data.Repo
import top.learningman.push.entity.JSONMessageItem
import top.learningman.push.entity.Message
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class ReceiverService : NotificationListenerService() {
    init {
        Log.d("ReceiverService", "ReceiverService init")
    }

    private lateinit var manager: WebsocketSessionManager
    private val id = UUID.randomUUID().toString()

    @Suppress("PrivatePropertyName")
    private val TAG
        get() = "Recv-${id.substring(0, 8)}"

    override fun onCreate() {
        super.onCreate()
        manager = WebsocketSessionManager(this)
        Log.i(TAG, "ReceiverService $id create")

        services.add(id)
        if (services.size > 1) {
            Log.e(TAG, "ReceiverService $id create more than once")
            for (service in services) {
                Log.e(TAG, "ReceiverService $service exists")
            }
        }
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

//    override fun onTaskRemoved(rootIntent: Intent) {
//        val restartServiceIntent = Intent(applicationContext, ReceiverService::class.java).also {
//            it.setPackage(packageName)
//        }
//        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(
//            this,
//            1,
//            restartServiceIntent,
//            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
//        )
//        applicationContext.getSystemService(Context.ALARM_SERVICE)
//        val alarmService =
//            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        alarmService.set(
//            AlarmManager.ELAPSED_REALTIME,
//            SystemClock.elapsedRealtime() + 1000,
//            restartServicePendingIntent
//        )
//    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "onListenerDisconnected in $id")
        requestRebind(
            ComponentName(
                applicationContext,
                NotificationListenerService::class.java
            )
        )
        manager.tryResume()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected")
        manager.tryResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.stop()
        // Maybe useless but just in case
        manager.cancel()

        Log.i(TAG, "ReceiverService $id destroyed")

        services.remove(id)
        if (services.size > 0) {
            Log.e(TAG, "ReceiverService $id destroy but still exists")
            for (service in services) {
                Log.e(TAG, "ReceiverService $service exists")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private class WebsocketSessionManager(private val service: ReceiverService) :
        CoroutineScope by CoroutineScope(context = newSingleThreadContext("WebsocketSessionManager")) {

        private val TAG
            get() = "Recv-${service.id.substring(0, 8)}-Mgr"

        private var job: Job? = null
        private var jobLock = Mutex()
        private val repo by lazy { Repo.getInstance(service) }
        private var currentUserID = repo.getUser()
        private val client by lazy {
            HttpClient(OkHttp) {
                install(WebSockets)
                install(HttpRequestRetry)
            }
        }
        private val connectivityManager by lazy {
            service.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }

        private var status = AtomicInteger(Status.WAIT_START)


        object Status {
            const val WAIT_START = 0
            const val CONNECTING = 1
            const val RUNNING = 2
            const val WAIT_RECONNECT = 3
            const val NETWORK_LOST = 4
            const val INVALID = 5
            const val STOP = 6

            fun valueOf(value: Int): String {
                return when (value) {
                    WAIT_START -> "WAIT_START"
                    CONNECTING -> "CONNECTING"
                    RUNNING -> "RUNNING"
                    WAIT_RECONNECT -> "WAIT_RECONNECT"
                    NETWORK_LOST -> "NETWORK_LOST"
                    INVALID -> "INVALID"
                    STOP -> "STOP"
                    else -> "UNKNOWN"
                }
            }
        }

        private val networkCallback = object : ConnectivityManager.NetworkCallback() {
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
                if (status.get() == Status.STOP) {
                    return
                }
                runBlocking {
                    tryCancelJob()
                }
            }
        }

        init {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }

        fun stop() {
            status.set(Status.STOP)
            runBlocking {
                tryCancelJob()
            }
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }

        suspend fun tryCancelJob() {
            jobLock.lock()
            try {
                if (job != null) {
                    job?.cancelAndJoin()
                    job = null
                    Log.i(TAG, "job cancelled in ${service.id}")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "tryCancelJob: ", e)
            } finally {
                jobLock.unlock()
            }
        }

        private fun diagnose(msg: String = "") {
            Log.d(TAG, "current thread ${Thread.currentThread().id}")
            if (msg.isNotEmpty()) {
                Log.d(TAG, msg)
            }
        }

        fun tryResume() {
            Log.d(TAG, "tryResume with status ${Status.valueOf(status.get())} at ${Date()}")
            if (status.compareAndSet(Status.WAIT_START, Status.CONNECTING) || status.compareAndSet(
                    Status.WAIT_RECONNECT,
                    Status.CONNECTING
                )
            ) {
                launch { connect() }
                Log.d(
                    TAG,
                    "tryResume: start websocket from ${Status.valueOf(status.get())}, initial startup"
                )
            } else {
                Log.d(
                    TAG,
                    "tryResume: not start websocket from status ${Status.valueOf(status.get())}"
                )
            }
        }

        fun updateUserID(nextUserID: String) {
            if (currentUserID != nextUserID) {
                currentUserID = nextUserID
                launch {
                    tryCancelJob()
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
                    tryCancelJob()
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
                Log.d(TAG, "job is not null, should cancel it before connect")
                jobLock.unlock()
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
                try {
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
                        Log.i(TAG, "job is active, start websocket success in ${service.id}")
                    } else {
                        Log.e(TAG, "job is not active, start websocket failed")
                    }
                } finally {
                    jobLock.unlock()
                }
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
                        notifyMessage(notificationMessage, from = service.id)
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
            Utils.notifyMessage(service, message)
        }
    }


    enum class Action {
        UPDATE
    }

    companion object {

        const val INTENT_USERID_KEY = "nextUserID"

        var services = mutableListOf<String>()
    }
}
