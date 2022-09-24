package top.learningman.push.utils

import dev.zxilly.notify.sdk.Client
import top.learningman.push.entity.Message
import top.learningman.push.Constant

object APIUtils {
    suspend fun requestDelete(userID: String, msgID: String): Result<Unit> {
        val client =
            Client.create(userID, Constant.API_ENDPOINT).getOrElse { return Result.failure(it) }
        return client.delete(msgID)
    }

    suspend fun check(userID: String): Result<Unit> {
        Client.create(userID, Constant.API_ENDPOINT).getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }

    suspend fun reportFCMToken(userID: String, token: String): Result<Unit> {
        val client =
            Client.create(userID, Constant.API_ENDPOINT).getOrElse { return Result.failure(it) }
        return client.reportFCMToken(token)
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