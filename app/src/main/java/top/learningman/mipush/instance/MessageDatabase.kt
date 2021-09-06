package top.learningman.mipush.instance

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import top.learningman.mipush.dao.MessageDao
import top.learningman.mipush.entity.DatabaseMessage

@Database(version = 1, entities = [DatabaseMessage::class], exportSchema = false)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
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
