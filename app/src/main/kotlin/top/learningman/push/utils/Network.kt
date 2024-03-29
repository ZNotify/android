package top.learningman.push.utils

import android.util.Log
import dev.zxilly.notify.sdk.Client
import dev.zxilly.notify.sdk.entity.Channel
import kotlinx.coroutines.sync.Mutex
import top.learningman.push.Constant
import top.learningman.push.entity.Message

object Network {
    private var client: Client? = null
    private val clientMutex = Mutex()

    private suspend fun <T> sync(block: suspend () -> T): T {
        clientMutex.lock()
        val ret = block()
        clientMutex.unlock()
        return ret
    }

    suspend fun updateClient(userID: String) = sync {
        client = Client.create(userID, Constant.API_ENDPOINT)
            .onFailure {
                Log.e("Network", "Client create failed", it)
            }
            .getOrNull()
    }

    suspend fun requestDelete(msgID: String) = sync {
        return@sync client?.deleteMessage(msgID)
            ?: Result.failure(Exception("Client is null"))
    }

    suspend fun check(userID: String) = sync {
        Log.d("Network", "Checking userID: $userID")
        runCatching {
            return@sync Result.success(Client.check(userID, Constant.API_ENDPOINT))
        }.onFailure {
            return@sync Result.failure(it)
        }
        return@sync Result.failure(Exception("Unknown error"))
    }

    suspend fun register(
        token: String,
        channel: Channel,
        deviceID: String
    ): Result<Boolean> = sync {
        return@sync client?.createDevice(channel, token, deviceID)
            ?: Result.failure(Exception("Client is null"))
    }

    suspend fun fetchMessage(): Result<List<Message>> = sync {
        client?.getMessages()?.onFailure {
            return@sync Result.failure(it)
        }?.getOrNull()?.let { ret ->
            return@sync Result.success(ret.map {
                Message(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    createdAt = it.createdAt,
                    long = it.long,
                )
            })
        } ?: Result.failure(Exception("Client is null"))

    }
}