package top.learningman.push

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import top.learningman.push.entity.Message
import kotlin.concurrent.thread


class MessageViewModel : ViewModel() {
    private var _message = MutableLiveData(emptyList<Message>())
    val message: LiveData<List<Message>> = _message

    fun loadMessages(user_id: String) {
        thread {
            val client = OkHttpClient()
            val url = BuildConfig.APIURL.toHttpUrlOrNull()!!.newBuilder()
                .addPathSegment(user_id)
                .addPathSegment("record")
                .build()
            val request = Request.Builder()
                .url(url)
                .build()
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("no body")
                val gson = Gson()
                val messages: List<Message> =
                    gson.fromJson(body, object : TypeToken<List<Message>>() {}.type)
                _message.postValue(messages)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMessage(msg_id: String) {
        _message.value?.let { messageList ->
            val newList = messageList.filter { it.id != msg_id }
            _message.postValue(newList)
        }
    }
}
