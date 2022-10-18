package top.learningman.push.utils

import android.util.Log
import dev.zxilly.notify.sdk.Client
import dev.zxilly.notify.sdk.entity.Channel
import top.learningman.push.Constant
import top.learningman.push.entity.Message

object Network {
    var client: Client? = null

    suspend fun updateClient(userID: String) {
        client = Client.create(userID, Constant.API_ENDPOINT).getOrNull()
    }

    suspend fun requestDelete(msgID: String): Result<Unit> {
        return client?.delete(msgID) ?: Result.failure(Exception("Client is null"))
    }

    suspend fun check(userID: String): Result<Boolean> {
        Log.d("Network", "Checking userID: $userID")
        runCatching {
            return Result.success(Client.check(userID, Constant.API_ENDPOINT))
        }.onFailure {
            return Result.failure(it)
        }
        return Result.failure(Exception("Unknown error"))
    }

    suspend fun register(
        token: String,
        channel: Channel,
        deviceID: String
    ): Result<Boolean> {
        return client?.register(channel, token, deviceID)
            ?: Result.failure(Exception("Client is null"))
    }

    suspend fun fetchMessage(): Result<List<Message>> {
        return client?.fetchMessage {
            this.map {
                Message(
                    it.id,
                    it.title,
                    it.content,
                    it.created_at,
                    it.long,
                    it.user_id
                )
            }
        } ?: Result.failure(Exception("Client is null"))
    }
}