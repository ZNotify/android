package top.learningman.push.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import top.learningman.push.databinding.ActivityTranslucentBinding
import top.learningman.push.utils.fromRFC3339
import top.learningman.push.view.MessageDialog

class TranslucentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("TranslucentActivity", "onCreate")
        super.onCreate(savedInstanceState)
        val binding = ActivityTranslucentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val long = intent.getStringExtra(LONG_INTENT_KEY)
        val msgID = intent.getStringExtra(MSGID_INTENT_KEY)
        val title = intent.getStringExtra(TITLE_INTENT_KEY)
        val content = intent.getStringExtra(CONTENT_INTENT_KEY)
        val time = intent.getStringExtra(TIME_INTENT_KEY)

        if (long == null
            || msgID == null
            || title == null
            || content == null
            || time == null
        ) {
            Toast.makeText(this, "intent missing field", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val message = MessageDialog.Message(
            title,
            content,
            long,
            time.fromRFC3339(),
            msgID
        )
        MessageDialog.show(message, this) {
            finish()
        }
    }

    companion object {
        const val LONG_INTENT_KEY = "long"
        const val MSGID_INTENT_KEY = "msg_id"
        const val TITLE_INTENT_KEY = "title"
        const val CONTENT_INTENT_KEY = "content"
        const val TIME_INTENT_KEY = "created_at"
    }
}