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
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import top.learningman.push.Constant
import top.learningman.push.data.Repo
import top.learningman.push.entity.JSONMessageItem
import top.learningman.push.entity.Message
import top.learningman.push.service.ReceiverService
import top.learningman.push.service.Utils.notifyMessage
import java.net.ProtocolException
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
                    .pingInterval(4, TimeUnit.MINUTES)
                    .build()
            }
        }
    }

    private class ManualCloseException : Exception("Manual Close Session")

    private var errorChannel = Channel<ManualCloseException>(Channel.RENDEZVOUS).also { it.close() }


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
            tryCancelJob("network lost")
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    fun stop() {
        status.set(Status.STOP)

        tryCancelJob("stop", true)

        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun tryCancelJob(reason: String = "unknown", elegant: Boolean = false) {
        Log.i(tag, "tryCancelJob reason: $reason")
        coroutineContext.ensureActive()
        if (elegant) {
            if (!errorChannel.isClosedForSend && !errorChannel.isClosedForReceive) {
                errorChannel.trySend(ManualCloseException())
            }
        } else {
            coroutineContext.cancelChildren()
        }
        Log.d(tag, "tryCancelJob done reason: $reason elegant: $elegant")
    }

    private fun diagnose(msg: String = "") {
        Log.d(tag, "current thread ${Thread.currentThread().id}")
        if (msg.isNotEmpty()) {
            Log.d(tag, msg)
        }
    }

    fun tryResume() {
        Log.d(tag, "tryResume with status ${currentStatusString()} at ${Date()}")
        if (status.compareAndSet(Status.WAIT_START, Status.CONNECTING) ||
            status.compareAndSet(Status.WAIT_RECONNECT, Status.CONNECTING)
        ) {
            connect()
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
            tryCancelJob("user changed", true)
            if (status.compareAndSet(Status.RUNNING, Status.WAIT_RECONNECT)) {
                launch {
                    delay(1000)
                    tryResume()
                }
            }
        } else {
            tryResume()
        }
    }

    private fun recover() {
        Log.i(tag, "recover at ${Date()}")
        if (status.compareAndSet(Status.WAIT_RECONNECT, Status.CONNECTING)) {
            if (retryLimit-- > 0) {
                tryCancelJob("recover")
                launch {
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

        fun restart() {
            if (status.compareAndSet(Status.RUNNING, Status.WAIT_RECONNECT)) {
                recover()
            } else {
                Log.d(tag, "connect: not recover from status ${currentStatusString()}")
            }
        }

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
            errorChannel = Channel(Channel.RENDEZVOUS)
            runCatching {
                client.webSocket({
                    url {
                        this.takeFrom(Constant.API_WS_ENDPOINT)
                        appendEncodedPathSegments(currentUserID, "host", "conn")
                    }
                    header("X-Device-ID", repo.getDeviceID())
                }) {
                    while (this@launch.isActive) {
                        val frame = select<Result<Frame>> {
                            incoming.onReceive { Result.success(it) }
                            errorChannel.onReceive { Result.failure(it) }
                        }
                        frame.fold({
                            handleFrame(it)
                        }, {
                            when (it) {
                                is ClosedReceiveChannelException -> {
                                    Log.d(tag, "websocket closed", it)
                                    restart()
                                }
                                is CancellationException -> {
                                    Log.d(tag, "websocket cancelled")
                                    restart()
                                }
                                is ManualCloseException -> {
                                    Log.d(tag, "websocket closed manually")
                                    close(
                                        CloseReason(
                                            CloseReason.Codes.GOING_AWAY,
                                            "manually close"
                                        )
                                    )
                                    restart()
                                }
                                else -> {
                                    Log.e(tag, "websocket unexpected error", it)
                                    restart()
                                }
                            }
                            return@webSocket
                        })

                    }
                }
            }.onFailure {
                Log.e(tag, "websocket first connect error", it)
                if (it is ProtocolException) {
                    if (it.message?.contains("401") == true) {
                        Log.i(tag, "seems userid not valid")
                        status.set(Status.INVALID)
                        return@onFailure
                    }
                }
                if (it is CancellationException) {
                    return@onFailure
                }
                restart()
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
        notifyMessage(service, message)
    }
}