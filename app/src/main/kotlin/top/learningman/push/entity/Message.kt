package top.learningman.push.entity

import kotlinx.serialization.Serializable

data class Message(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val long: String,
    val userID : String,
)

@Serializable
data class JSONMessageItem(
    val id: String,
    val user_id: String,
    val content: String,
    val title: String,
    val long: String,
    val created_at: String
) {
    fun toMessage(): Message {
        return Message(
            id = id,
            title = title,
            content = content,
            createdAt = created_at,
            long = long,
            userID = user_id
        )
    }
}
