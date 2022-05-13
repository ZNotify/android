package top.learningman.push

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import top.learningman.push.databinding.ActivityMessagesBinding
import top.learningman.push.utils.MessageAdapter
import kotlin.concurrent.thread


class MessagesActivity : AppCompatActivity() {
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var mAdapter: MessageAdapter
    private lateinit var binding: ActivityMessagesBinding

    private val mViewModel: MessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mLayoutManager = LinearLayoutManager(this)
        mAdapter = MessageAdapter(mViewModel)

        val dividerItemDecoration = DividerItemDecoration(this, mLayoutManager.orientation)

        binding.messagesList.layoutManager = mLayoutManager
        binding.messagesList.adapter = mAdapter
        binding.messagesList.addItemDecoration(dividerItemDecoration)

        mViewModel.message.observe(this) {
            mAdapter.submitList(it)
        }

        // get userid from preference
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val userid = pref.getString("user_id", "none")!!

        thread { mViewModel.loadMessages(userid) }
    }

}