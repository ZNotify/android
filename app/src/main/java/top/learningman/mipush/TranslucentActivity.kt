package top.learningman.mipush

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import top.learningman.mipush.utils.MessageAdapter.MessageHolder.Companion.fromRFC3339
import top.learningman.mipush.utils.Network
import java.util.*
import kotlin.concurrent.thread


class TranslucentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("TranslucentActivity", "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translucent)

        val userID = intent.getStringExtra("userID")!!
        val long = intent.getStringExtra("long")!!
        val msgID = intent.getStringExtra("msgID")!!
        val title = intent.getStringExtra("title")!!
        val content = intent.getStringExtra("content")!!
        val time = intent.getStringExtra("createdAt")!!.fromRFC3339()

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.message_dialog, null, false)

        val dialogContent = dialogView.findViewById<TextView>(R.id.dialog_content)
        val dialogLong = dialogView.findViewById<TextView>(R.id.dialog_long)
        val dialogTime = dialogView.findViewById<TextView>(R.id.dialog_time)

        dialogContent.text = content

        if (long != "") {
            dialogLong.text = long
            dialogLong.movementMethod = ScrollingMovementMethod.getInstance()
            dialogLong.visibility = View.VISIBLE
        }

        time.let {
            dialogTime.text =
                SimpleDateFormat(
                    "y年M月d日 HH:mm:ss",
                    Locale.getDefault()
                ).format(it.time)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setNeutralButton("删除") { dialog, _ ->
                thread {
                    Network.requestDelete(userID, msgID)
                }.join()
                dialog.cancel()
                finish()
            }
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
            }
        dialog.show()
        val neuButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
        neuButton.setTextColor(Color.RED)
    }
}