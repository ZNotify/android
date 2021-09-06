package top.learningman.mipush.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DatabaseMessage(
    var content: String,
    var longMSg: String,
    var msgId: String,
    var title: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
