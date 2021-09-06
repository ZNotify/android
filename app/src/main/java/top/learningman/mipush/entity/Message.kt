package top.learningman.mipush.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
    var content: String,
    var longMSg: String,
    var msgId: String,
    var title: String,
    var time: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
