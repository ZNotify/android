package top.learningman.push.utils

import Client
import top.learningman.push.entity.Message

object Network {

    suspend fun requestDelete(userID: String, msgID: String): Result<Unit> {
        val client = Client.create(userID).getOrElse { return Result.failure(it) }
        return client.delete(msgID)
    }

    suspend fun check(userID: String): Result<Unit> {
        Client.create(userID).getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }

    suspend fun reportFCMToken(userID: String, token: String): Result<Unit> {
        val client = Client.create(userID).getOrElse { return Result.failure(it) }
        return client.reportFCMToken(token)
    }

    suspend fun fetchMessage(userID: String): Result<List<Message>> {
        return null // FIXME
    }
}