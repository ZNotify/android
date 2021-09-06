package top.learningman.mipush.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import top.learningman.mipush.entity.Message

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMessage(msg: Message)

    @Query("select * from Message")
    fun getAllMessages(): Flow<List<Message>>

    @Query("select count(*) from Message")
    fun countAllMessages(): Flow<Int>
}