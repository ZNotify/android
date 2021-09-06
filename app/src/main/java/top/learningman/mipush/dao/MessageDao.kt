package top.learningman.mipush.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import top.learningman.mipush.entity.DatabaseMessage

@Dao
interface MessageDao {
    @Insert
    fun insertMessage(msg: DatabaseMessage): Long

    @Query("select * from DatabaseMessage")
    fun getAllMessages(): List<DatabaseMessage>

    @Query("select count(*) from DatabaseMessage")
    fun countAllMessages(): Int
}