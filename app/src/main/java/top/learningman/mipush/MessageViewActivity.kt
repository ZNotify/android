package top.learningman.mipush

import android.annotation.SuppressLint
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.xiaomi.mipush.sdk.MiPushMessage

class MessageViewActivity : AppCompatActivity() {
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_view)

        Log.d("MessageView", "Enter MessageView Activity")

        val msg = intent.getSerializableExtra("message") as MiPushMessage
        val payload = msg.content
        val content = msg.description
        val title = msg.title

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.message_dialog, null)

        val dialogContent = dialogView.findViewById<TextView>(R.id.dialog_content)
        val dialogLong = dialogView.findViewById<TextView>(R.id.dialog_long)

        dialogContent.text = content

        if (payload != "") {
            dialogLong.text = payload
            dialogLong.movementMethod = ScrollingMovementMethod.getInstance()
            dialogLong.visibility = View.VISIBLE
        }

        val alertDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("关闭") { dialog, _ ->
                dialog.cancel()
            }
            .setOnCancelListener {
                finish()
            }
            .create()
        alertDialog.show()
        Log.d("MessageView", "Left MessageView Activity onCreate")
    }
}