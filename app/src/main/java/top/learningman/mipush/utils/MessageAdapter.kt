package top.learningman.mipush.utils

import android.content.DialogInterface
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.text.format.DateUtils
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
import top.learningman.mipush.view.MessageDialog
import java.util.*

class MessageAdapter(private val viewModel: MessageViewModel) :
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
        val userid = PreferenceManager.getDefaultSharedPreferences(itemView.context)
            .getString("user_id", "none")!!

        fun bind(msg: Message) {
            messageItemTitleView.text = msg.title
            messageItemContentView.text = msg.content

            val relativeTime =
                msg.createdAt.let { DateUtils.getRelativeTimeSpanString(it.fromRFC3339().time) }
            messageItemTimeView.text = relativeTime

            val message = MessageDialog.Message(
                msg.title,
                msg.content,
                msg.long,
                msg.createdAt.fromRFC3339(),
                msg.id,
                userid
            )

            val dialog = MessageDialog.show(message, itemView.context, false) {
                viewModel.deleteMessage(msg.id)
            }

            messageItem.setOnClickListener {
                dialog.show()
                val neuButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
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