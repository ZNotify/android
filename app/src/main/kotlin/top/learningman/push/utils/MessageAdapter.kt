package top.learningman.push.utils

import android.content.DialogInterface
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.learningman.push.MessageViewModel
import top.learningman.push.R
import top.learningman.push.databinding.TextRowItemBinding
import top.learningman.push.entity.Message
import top.learningman.push.view.MessageDialog
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
        private val binding = TextRowItemBinding.bind(itemView)
        private val messageItem = binding.rowItem
        private val messageItemTitleView = binding.rowItemTitle
        private val messageItemContentView = binding.rowItemContent
        private val messageItemTimeView = binding.rowItemTime

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

            val dialog = MessageDialog.show(message, itemView.context, false) { positive ->
                if (!positive) {
                    viewModel.deleteMessage(msg.id)
                }
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