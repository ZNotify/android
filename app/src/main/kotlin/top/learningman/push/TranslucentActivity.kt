package top.learningman.push

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import top.learningman.push.databinding.ActivityTranslucentBinding
import top.learningman.push.entity.MessageAdapter.MessageHolder.Companion.fromRFC3339
import top.learningman.push.view.MessageDialog


class TranslucentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("TranslucentActivity", "onCreate")
        super.onCreate(savedInstanceState)
        val binding = ActivityTranslucentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userID = intent.getStringExtra("user_id")
        val long = intent.getStringExtra("long")
        val msgID = intent.getStringExtra("msg_id")
        val title = intent.getStringExtra("title")
        val content = intent.getStringExtra("content")
        val time = intent.getStringExtra("created_at")

        if (userID == null
            || long == null
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
            msgID,
            userID
        )
        MessageDialog.show(message, this) {
            finish()
        }
    }
}