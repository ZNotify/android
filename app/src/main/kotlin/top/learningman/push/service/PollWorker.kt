package top.learningman.push.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.learningman.push.Constant
import top.learningman.push.data.Repo
import top.learningman.push.entity.JSONMessageItem

class PollWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Polling for new notifications")
            val repository = Repo.getInstance(applicationContext)
            val userID = repository.getUser()

            HttpClient(OkHttp) {
                install(WebSockets)
            }.webSocket(urlString = "${Constant.API_WS_ENDPOINT}/${userID}/host/conn",
                request = {
                    header("X-Message-Since", repository.getLastMessageTime())
                }
            ) {
                incoming.consumeEach { frame ->
                    run {
                        when (frame) {
                            is Frame.Text -> {
                                val messageText = frame.readText()
                                val messageJson =
                                    Json.decodeFromString(JSONMessageItem.serializer(), messageText)
                                val message = messageJson.toMessage()
                                Utils.notifyMessage(applicationContext, message)
                                repository.setLastMessageTime(messageJson.created_at)
                            }
                            else -> {
                                Log.d(TAG, "Received frame: $frame")
                            }
                        }
                    }
                }
                close()
            }

            return@withContext Result.success()
        }
    }

    companion object {
        const val TAG = "PollWorker"
        const val WORK_NAME_PERIODIC_ALL = "PollWorkerPeriodic"
    }
}
