package top.learningman.push.utils

import dev.zxilly.notify.sdk.Client
import dev.zxilly.notify.sdk.entity.Channel
import top.learningman.push.Constant
import top.learningman.push.entity.Message

object Network {
    suspend fun requestDelete(userID: String, msgID: String): Result<Unit> {
        val client =
            Client.create(userID, Constant.API_ENDPOINT).getOrElse { return Result.failure(it) }
        return client.delete(msgID)
    }

    suspend fun check(userID: String): Result<Boolean> {
        runCatching {
            return Result.success(Client.check(userID, Constant.API_ENDPOINT))
        }.onFailure {
            return Result.failure(it)
        }
        return Result.failure(Exception("Unknown error"))
    }

    suspend fun register(
        userID: String,
        token: String,
        channel: Channel,
        deviceID: String
    ): Result<Boolean> {
        val client =
            Client.create(userID, Constant.API_ENDPOINT).getOrElse { return Result.failure(it) }
        return client.register(channel, token, deviceID)
    }

    suspend fun fetchMessage(userID: String): Result<List<Message>> {
        val client =
            Client.create(userID, Constant.API_ENDPOINT).getOrElse { return Result.failure(it) }
        return client.fetchMessage {
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
        }
    }
}