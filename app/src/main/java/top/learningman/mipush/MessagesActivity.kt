package top.learningman.mipush

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_messages.*
import top.learningman.mipush.entity.MessageViewModel
import top.learningman.mipush.entity.MessageViewModelFactory
import top.learningman.mipush.utils.MessageAdapter
import androidx.recyclerview.widget.DividerItemDecoration


class MessagesActivity : AppCompatActivity() {
    lateinit var mLayoutManager: LinearLayoutManager
    lateinit var mAdapter: MessageAdapter

    private val messageViewModel: MessageViewModel by viewModels {
        MessageViewModelFactory((application as MainApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        mLayoutManager = LinearLayoutManager(this)
        mAdapter = MessageAdapter()

        val dividerItemDecoration = DividerItemDecoration(this, mLayoutManager.orientation)

        messages_list.layoutManager = mLayoutManager
        messages_list.adapter = mAdapter
        messages_list.addItemDecoration(dividerItemDecoration)

        messageViewModel.messages.observe(this) {
            it.let {
                mAdapter.submitList(it)
            }
        }
    }
}