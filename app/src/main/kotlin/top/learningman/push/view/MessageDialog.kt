package top.learningman.push.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import kotlinx.coroutines.runBlocking
import top.learningman.push.databinding.MessageDialogBinding
import top.learningman.push.utils.Markwon
import top.learningman.push.utils.Network
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
        cb: ((Boolean) -> Unit)? = null
    ): AlertDialog {
        val binding = MessageDialogBinding.inflate(LayoutInflater.from(context))
        val dialogView = binding.root

        val dialogContent = binding.dialogContent
        val dialogLong = binding.dialogLong
        val dialogTime = binding.dialogTime

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
                cb?.invoke(true)
            }
            .setNeutralButton("删除") { dialog, _ ->
                thread {
                    runBlocking {
                        Network.requestDelete(msg.userID, msg.msgID)
                            .onFailure {
                                Log.e("MessageDialog", "Delete error", it)
                            }
                    }
                }
                dialog.cancel()
                cb?.invoke(false)
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