package top.learningman.push.service.websocket

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.microsoft.appcenter.crashes.Crashes
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.onSuccess
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import top.learningman.push.Constant
import top.learningman.push.data.Repo
import top.learningman.push.entity.JSONMessageItem
import top.learningman.push.entity.Message
import top.learningman.push.service.ReceiverService
import top.learningman.push.service.Utils
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(DelicateCoroutinesApi::class)
class WebSocketSessionManager(private val service: ReceiverService) :
    CoroutineScope by CoroutineScope(context = newSingleThreadContext("WebsocketSessionManager") + SupervisorJob()) {

    private val tag
        get() = "Recv-${service.id.substring(0, 8)}-Mgr"

    private val repo by lazy { Repo.getInstance(service) }
    private var currentUserID = repo.getUser()
    private val client by lazy {
        HttpClient(OkHttp) {
            install(WebSockets)
            install(HttpRequestRetry)
            engine {
                preconfigured = OkHttpClient.Builder()
                    .pingInterval(20, TimeUnit.SECONDS)
                    .build()
            }
        }
    }
    private val connectivityManager by lazy {
        service.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private var status = AtomicInteger(Status.WAIT_START)
    private fun currentStatusString() = Status.valueOf(status.get())

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

    fun tryCancelJob() {
        Log.i(tag, "tryCancelJob")
        // kill all job in current scope
        coroutineContext.ensureActive()
    }

    private fun diagnose(msg: String = "") {
        Log.d(tag, "current thread ${Thread.currentThread().id}")
        if (msg.isNotEmpty()) {
            Log.d(tag, msg)
        }
    }

    fun tryResume() {
        Log.d(tag, "tryResume with status ${currentStatusString()} at ${Date()}")
        if (status.compareAndSet(Status.WAIT_START, Status.CONNECTING) || status.compareAndSet(
                Status.WAIT_RECONNECT,
                Status.CONNECTING
            )
        ) {
            launch { connect() }
        } else {
            Log.d(
                tag,
                "tryResume: not start websocket from status ${currentStatusString()}"
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

    private fun connect() {
        diagnose()

        if (status.get() == Status.INVALID) {
            Log.d(tag, "status is invalid, should not connect")
            return
        }

        if (!status.compareAndSet(Status.CONNECTING, Status.RUNNING)) {
            Log.d(
                tag,
                "connect: not connecting status,is ${currentStatusString()}"
            )
            return
        } else {
            Log.d(tag, "connect: start websocket from CONNECTING")
        }

        launch {
            runCatching {
                client.webSocket({
                    url{
                        this.takeFrom(Constant.API_WS_ENDPOINT)
                        appendEncodedPathSegments(currentUserID, "host", "conn")
                    }
                    request {
                        header("X-Device-ID", repo.getDeviceID())
                    }
                }){
                    while (this.isActive){
                        val frame = incoming.receiveCatching()
                        frame.onSuccess {

                        }
                    }
                }

                client.webSocketSession(urlString = "${Constant.API_WS_ENDPOINT}/${currentUserID}/host/conn")
                {
                    val deviceID = repo.getDeviceID()
                    header("X-Device-ID", deviceID)
                    Log.i(tag, "deviceID: $deviceID")
                }
            }
        }

        fun tryRestart() {
            if (status.compareAndSet(Status.RUNNING, Status.WAIT_RECONNECT)) {
                recover()
            } else {
                Log.d(tag, "connect: not recover from status ${currentStatusString()}")
            }
        }
    }

    private fun handleFrame(frame: Frame) {
        when (frame) {
            is Frame.Text -> {
                val message = frame.readText()
                Log.d(tag, "handleFrame: $message")
                runCatching {
                    Json.decodeFromString(
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