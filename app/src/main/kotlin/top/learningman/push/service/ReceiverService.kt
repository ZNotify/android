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
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class ReceiverService : NotificationListenerService() {
    init {
        Log.d("ReceiverService", "ReceiverService init")
    }

    private lateinit var manager: WebSocketSessionManager
    private val id = UUID.randomUUID().toString()

    @Suppress("PrivatePropertyName")
    private val tag
        get() = "Recv-${id.substring(0, 8)}"

    override fun onCreate() {
        super.onCreate()
        manager = WebSocketSessionManager(this)
        Log.i(tag, "ReceiverService $id create")

        services.add(id)
        if (services.size > 1) {
            Log.e(tag, "ReceiverService $id create more than once")
            for (service in services) {
                Log.e(tag, "ReceiverService $service exists")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand executed with startId: $startId")
        if (intent != null) {
            Log.d(tag, "using an intent with action ${intent.action}")
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
            Log.d(tag, "with a null intent. It has been probably restarted by the system.")
            manager.tryResume()
        }
        return START_STICKY
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(tag, "onListenerDisconnected in $id")
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
        Log.d(tag, "onListenerConnected")
        manager.tryResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.stop()
        // Maybe useless but just in case
        manager.cancel()

        Log.i(tag, "ReceiverService $id destroyed")

        services.remove(id)
        if (services.size > 0) {
            Log.e(tag, "ReceiverService $id destroy but still exists")
            for (service in services) {
                Log.e(tag, "ReceiverService $service exists")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private class WebSocketSessionManager(private val service: ReceiverService) :
        CoroutineScope by CoroutineScope(context = newSingleThreadContext("WebsocketSessionManager") + SupervisorJob()) {

        private val tag
            get() = "Recv-${service.id.substring(0, 8)}-Mgr"

        private var session: DefaultClientWebSocketSession? = null
        private var jobLock = Mutex()
        private val repo by lazy { Repo.getInstance(service) }
        private var currentUserID = repo.getUser()
        private val client by lazy {
            HttpClient(OkHttp) {
                install(WebSockets) {
                    pingInterval = Duration.ofSeconds(30).toMillis()
                }
                install(HttpRequestRetry)
            }
        }
        private val connectivityManager by lazy {
            service.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }

        private var status = AtomicInteger(Status.WAIT_START)

        private var retryLimit = 32


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
                    Log.d(tag, "network available, try start websocket")
                    Log.d(tag, "resume from network lost")
                    tryResume()
                } else {
                    Log.d(tag, "network available, but not in network lost status")
                }
            }

            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                Log.d(tag, "network lost, stop websocket")
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

        @OptIn(ExperimentalCoroutinesApi::class)
        suspend fun tryCancelJob() {
            Log.i(tag, "tryCancelJob")
            jobLock.lock()
            try {
                if (session != null) {
                    session?.cancel()
                    session = null
                    Log.i(tag, "job cancelled in ${service.id}")
                }
            } catch (e: Throwable) {
                Log.e(tag, "tryCancelJob: ", e)
            } finally {
                jobLock.unlock()
            }
        }

        private fun diagnose(msg: String = "") {
            Log.d(tag, "current thread ${Thread.currentThread().id}")
            if (msg.isNotEmpty()) {
                Log.d(tag, msg)
            }
        }

        fun tryResume() {
            Log.d(tag, "tryResume with status ${Status.valueOf(status.get())} at ${Date()}")
            if (status.compareAndSet(Status.WAIT_START, Status.CONNECTING) || status.compareAndSet(
                    Status.WAIT_RECONNECT,
                    Status.CONNECTING
                )
            ) {
                launch { connect() }
                Log.d(
                    tag,
                    "tryResume: start websocket from ${Status.valueOf(status.get())}, initial startup"
                )
            } else {
                Log.d(
                    tag,
                    "tryResume: not start websocket from status ${Status.valueOf(status.get())}"
                )
            }
        }

        fun updateUserID(nextUserID: String) {
            if (currentUserID != nextUserID) {
                currentUserID = nextUserID
                launch {
                    tryCancelJob()
                    delay(1000)
                    connect()
                }
            } else {
                tryResume()
            }
        }

        private fun recover() {
            Log.i(tag, "recover at ${Date()}")
            if (status.compareAndSet(Status.WAIT_RECONNECT, Status.CONNECTING)) {
                if (retryLimit-- > 0) {
                    launch {
                        tryCancelJob()
                        delay(2000)
                        connect()
                    }
                } else {
                    Log.e(tag, "retry limit reached, stop websocket")
                    stop()
                }
            } else {
                Log.d(tag, "recover: not start websocket from status ${status.get()}")
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private suspend fun connect() {
            diagnose()
            jobLock.lock()
            if (session != null) {
                Log.d(tag, "job is not null, should cancel it before connect")
                jobLock.unlock()
                return
            }
            jobLock.unlock()

            if (status.get() == Status.INVALID) {
                Log.d(tag, "status is invalid, should not connect")
                return
            }

            if (!status.compareAndSet(Status.CONNECTING, Status.RUNNING)) {
                Log.d(
                    tag,
                    "connect: not connecting status,is ${Status.valueOf(status.get())}"
                )
                return
            } else {
                Log.d(tag, "connect: start websocket from CONNECTING")
            }

            session = runCatching {
                client.webSocketSession(urlString = "${Constant.API_WS_ENDPOINT}/${currentUserID}/host/conn")
                {
                    val deviceID = repo.getDeviceID()
                    header("X-Device-ID", deviceID)
                    Log.i(tag, "deviceID: $deviceID")
                }
            }.also {
                if (it.isFailure) {
                    Log.e(tag, "getSession:", it.exceptionOrNull())
                    val exception = it.exceptionOrNull() ?: return@also
                    val message = exception.message ?: return@also
                    val longMessage = exception.stackTraceToString()
                    Log.e(tag, "getSession: $longMessage")
                    if (message.contains("401")) {
                        status.set(Status.INVALID)
                        Log.d(tag, "connect: invalid status, seems userid not correct")
                        return@also
                    }
                    if (exception is CancellationException) {
                        Log.d(tag, "connect: cancelled")
                        status.set(Status.STOP)
                        return@also
                    }
                }

            }
                .getOrNull()

            fun tryRestart() {
                if (status.compareAndSet(Status.RUNNING, Status.WAIT_RECONNECT)) {
                    recover()
                } else {
                    Log.d(tag, "connect: not recover from status ${Status.valueOf(status.get())}")
                }
            }

            if (session != null) {
                Log.d(tag, "session is not null, launch WebSocket")
                jobLock.lock()
                try {
                    session?.also {
                        it.outgoing.invokeOnClose {
                            if (status.compareAndSet(Status.RUNNING, Status.WAIT_RECONNECT)) {
                                recover()
                            } else {
                                Log.d(tag, "onClosed: not recover from status ${status.get()}")
                            }
                        }
                    }?.launch(coroutineContext) {
                        while (true) {
                            val frameRet = session?.incoming?.receiveCatching() ?: let {
                                Log.d(tag, "session is null, should not receive")
                                return@launch
                            }
                            frameRet.onClosed {
                                Log.d(tag, "onClosed")
                                Log.e(tag, "WebSocket closed", it)
                                return@launch
                            }.onFailure {
                                if (it is CancellationException) {
                                    Log.d(tag, "This job is cancelled")
                                    return@launch
                                } else {
                                    Log.d(tag, "onFailure")
                                    Log.e(tag, "WebSocket error", it)
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
                                Log.d(tag, "onSuccess receive frame")
                                handleFrame(it)
                            }
                        }
                    }
                    if (session?.isActive == true) {
                        Log.i(tag, "job is active, start websocket success in ${service.id}")
                        retryLimit = 32
                    } else {
                        Log.e(tag, "job is not active, start websocket failed")
                        tryRestart()
                    }
                } finally {
                    jobLock.unlock()
                }
            } else {
                tryRestart()
            }
        }

        private fun handleFrame(frame: Frame) {
            when (frame) {
                is Frame.Text -> {
                    val message = frame.readText()
                    Log.d(tag, "handleFrame: $message")
                    runCatching {
                        return@runCatching Json.decodeFromString(
                            JSONMessageItem.serializer(),
                            message
                        )
                    }.fold({
                        Log.d(tag, "prepare to send notification")
                        val notificationMessage = it.toMessage()
                        notifyMessage(notificationMessage, from = service.id)
                    }, {
                        Log.e(tag, "Error parsing message", it)
                        Crashes.trackError(it)
                    })
                }
                else -> {
                    Log.e(tag, "Received unexpected frame: ${frame.frameType.name}")
                }
            }
        }

        private fun notifyMessage(message: Message, from: String = "anonymous") {
            Log.d(tag, "notifyMessage: $message from $from")
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
