package top.learningman.mipush.instance

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import top.learningman.mipush.dao.MessageDao
import top.learningman.mipush.entity.Message

@Database(version = 2, entities = [Message::class], exportSchema = false)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var instance: MessageDatabase? = null

        fun getDatabase(context: Context): MessageDatabase {
            instance?.let { return it }
            return Room.databaseBuilder(
                context.applicationContext,
                MessageDatabase::class.java, "MsgDatabase.db"
            ).build().apply {
                instance = this
            }
        }
    }
}
