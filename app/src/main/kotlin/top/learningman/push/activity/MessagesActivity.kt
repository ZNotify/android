package top.learningman.push.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import top.learningman.push.application.MainApplication
import top.learningman.push.data.Repo
import top.learningman.push.databinding.ActivityMessagesBinding
import top.learningman.push.entity.MessageAdapter
import top.learningman.push.viewModel.MessageViewModel
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

        mViewModel.isError.observe(this) {
            binding.warnNoMessage.visibility = if (it) {
                binding.messagesList.visibility = android.view.View.GONE
                android.view.View.VISIBLE
            } else {
                binding.messagesList.visibility = android.view.View.VISIBLE
                android.view.View.GONE
            }
        }

        val userid = (application as MainApplication).repo.getUser()

        if (userid != Repo.PREF_USER_DEFAULT) {
            thread { mViewModel.loadMessages() }
        }
    }

}