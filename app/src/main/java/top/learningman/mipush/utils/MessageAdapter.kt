package top.learningman.mipush.utils

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.learningman.mipush.MessageViewModel
import top.learningman.mipush.R
import top.learningman.mipush.entity.Message
import java.time.Instant
import java.util.*
import kotlin.concurrent.thread

class MessageAdapter(val viewModel: MessageViewModel) :
    ListAdapter<Message, MessageAdapter.MessageHolder>(WordsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        return MessageHolder.create(parent, viewModel)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class MessageHolder(itemView: View, private val viewModel: MessageViewModel) :
        RecyclerView.ViewHolder(itemView) {
        private val messageItem: ConstraintLayout = itemView.findViewById(R.id.row_item)
        private val messageItemTitleView: TextView = itemView.findViewById(R.id.row_item_title)
        private val messageItemContentView: TextView = itemView.findViewById(R.id.row_item_content)
        private val messageItemTimeView: TextView = itemView.findViewById(R.id.row_item_time)

        fun bind(msg: Message) {
            messageItemTitleView.text = msg.title
            messageItemContentView.text = msg.content

            val relativeTime =
                msg.createdAt.let { DateUtils.getRelativeTimeSpanString(it.fromRFC3339().time) }
            messageItemTimeView.text = relativeTime

            val dialogView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.message_dialog, messageItem, false)

            val dialogContent = dialogView.findViewById<TextView>(R.id.dialog_content)
            val dialogLong = dialogView.findViewById<TextView>(R.id.dialog_long)
            val dialogTime = dialogView.findViewById<TextView>(R.id.dialog_time)

            dialogContent.text = msg.content

            if (msg.long != "") {
                dialogLong.text = msg.long
                dialogLong.movementMethod = ScrollingMovementMethod.getInstance()
                dialogLong.visibility = View.VISIBLE
            }

            msg.createdAt.let {
                dialogTime.text =
                    SimpleDateFormat(
                        "y年M月d日 HH:mm:ss",
                        Locale.getDefault()
                    ).format(it.fromRFC3339().time)
            }

            val alertDialog = AlertDialog.Builder((itemView.context))
                .setTitle(msg.title)
                .setView(dialogView)
                .setPositiveButton("确定") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton("删除") { dialog, _ ->
                    val pref = PreferenceManager.getDefaultSharedPreferences(itemView.context)
                    val userid = pref.getString("user_id", "none")!!
                    thread {
                        msg.let {
                            viewModel.deleteMessage(it, userid)
                        }
                    }
                    dialog.cancel()
                }
                .create()

            messageItem.setOnClickListener {
                alertDialog.show()
                val neuButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
                neuButton.setTextColor(Color.RED)
            }
        }

        companion object {
            fun create(parent: ViewGroup, viewModel: MessageViewModel): MessageHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.text_row_item, parent, false)
                return MessageHolder(view, viewModel)
            }

            fun String.fromRFC3339(): Date {
                // 2022-02-08T18:00:54+08:00
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
                return sdf.parse(this)
            }
        }
    }

    class WordsComparator : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return this.areItemsTheSame(oldItem, newItem)
        }
    }
}