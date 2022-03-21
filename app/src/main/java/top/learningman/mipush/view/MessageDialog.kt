package top.learningman.mipush.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import top.learningman.mipush.R
import top.learningman.mipush.utils.Markwon
import top.learningman.mipush.utils.Network
import java.util.*
import kotlin.concurrent.thread

object MessageDialog {
    data class Message(
        val title: String,
        val content: String,
        val long: String,
        val time: Date,
        val msgID: String,
        val userID: String
    )

    @SuppressLint("InflateParams")
    fun show(
        msg: Message,
        context: Context,
        immediate: Boolean = true,
        cb: (() -> Unit)? = null
    ): AlertDialog {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.message_dialog, null, false)

        val dialogContent = dialogView.findViewById<TextView>(R.id.dialog_content)
        val dialogLong = dialogView.findViewById<TextView>(R.id.dialog_long)
        val dialogTime = dialogView.findViewById<TextView>(R.id.dialog_time)

        dialogContent.text = msg.content
        if (msg.long.isNotBlank()) {
            val markwon = Markwon.getInstance(context)
            markwon.setMarkdown(dialogLong, msg.long)
            dialogLong.movementMethod = ScrollingMovementMethod.getInstance()
            dialogLong.visibility = View.VISIBLE
        }

        msg.time.let {
            dialogTime.text =
                SimpleDateFormat(
                    "y年M月d日 HH:mm:ss",
                    Locale.getDefault()
                ).format(it.time)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(msg.title)
            .setView(dialogView)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                cb?.invoke()
            }
            .setNeutralButton("删除") { dialog, _ ->
                thread {
                    Network.requestDelete(msg.userID, msg.msgID)
                }.join()
                dialog.cancel()
                cb?.invoke()
            }
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
            }
        if (!immediate) {
            return dialog
        }

        dialog.show()
        val neuButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
        neuButton.setTextColor(Color.RED)
        return dialog
    }
}