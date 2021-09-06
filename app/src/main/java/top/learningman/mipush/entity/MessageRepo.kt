@file:Suppress("RedundantSuspendModifier")

package top.learningman.mipush.entity

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import top.learningman.mipush.dao.MessageDao

class MessageRepo(private val messageDao: MessageDao) {

    val size: Flow<Int> = messageDao.countAllMessages()
    val messages: Flow<List<Message>> = messageDao.getAllMessages()

    @WorkerThread
    suspend fun insertMessage(msg: Message) {
        messageDao.insertMessage(msg)
    }
}