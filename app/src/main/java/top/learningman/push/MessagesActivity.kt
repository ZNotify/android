package top.learningman.push

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_messages.*
import top.learningman.push.utils.MessageAdapter
import kotlin.concurrent.thread


class MessagesActivity : AppCompatActivity() {
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var mAdapter: MessageAdapter

    private val mViewModel: MessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        mLayoutManager = LinearLayoutManager(this)
        mAdapter = MessageAdapter(mViewModel)

        val dividerItemDecoration = DividerItemDecoration(this, mLayoutManager.orientation)

        messages_list.layoutManager = mLayoutManager
        messages_list.adapter = mAdapter
        messages_list.addItemDecoration(dividerItemDecoration)

        mViewModel.message.observe(this) {
            mAdapter.submitList(it)
        }

        // get userid from preference
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val userid = pref.getString("user_id", "none")!!

        thread { mViewModel.loadMessages(userid) }
    }

}