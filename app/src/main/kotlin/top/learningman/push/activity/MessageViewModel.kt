package top.learningman.push.activity

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import top.learningman.push.entity.Message
import top.learningman.push.utils.APIUtils

class MessageViewModel : ViewModel() {
    private var _message = MutableLiveData(emptyList<Message>())
    val message: LiveData<List<Message>> = _message

    private var _isError = MutableLiveData(false)
    val isError: LiveData<Boolean> = _isError

    fun loadMessages(userID: String) {
        viewModelScope.launch {
            APIUtils.fetchMessage(userID)
                .onSuccess {
                    _message.postValue(it)
                    _isError.postValue(false)
                }
                .onFailure {
                    Log.e("MessageViewModel", it.message, it)
                    _message.postValue(emptyList())
                    _isError.postValue(true)
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