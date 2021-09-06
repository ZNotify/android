package top.learningman.mipush.entity

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class MessageViewModel(private val repository: MessageRepo) : ViewModel() {

    val size: LiveData<Int> = repository.size.asLiveData()

    val messages: LiveData<List<Message>> = repository.messages.asLiveData()

    fun insertMessage(msg: Message) = viewModelScope.launch {
        repository.insertMessage(msg)
    }

}

class MessageViewModelFactory(private val repository: MessageRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}