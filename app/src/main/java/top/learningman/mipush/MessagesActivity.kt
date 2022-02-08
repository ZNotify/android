package top.learningman.mipush

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_messages.*
import top.learningman.mipush.utils.MessageAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import kotlin.concurrent.thread


class MessagesActivity : AppCompatActivity() {
    lateinit var mLayoutManager: LinearLayoutManager
    lateinit var mAdapter: MessageAdapter

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