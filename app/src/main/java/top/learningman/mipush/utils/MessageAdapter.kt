package top.learningman.mipush.utils

import android.app.AlertDialog
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.learningman.mipush.entity.Message
import top.learningman.mipush.R

class MessageAdapter : ListAdapter<Message, MessageAdapter.MessageHolder>(WordsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {

        return MessageHolder.create(parent)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)

    }

    class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageItemTitleView: TextView = itemView.findViewById(R.id.row_item_title)
        private val messageItemContentView: TextView = itemView.findViewById(R.id.row_item_content)
        private val messageItemTimeView: TextView = itemView.findViewById(R.id.row_item_time)
        private val messageItem: ConstraintLayout = itemView.findViewById(R.id.row_item)


        fun bind(msg: Message?) {
            messageItemTitleView.text = msg?.title
            messageItemContentView.text = msg?.content

            val relativeTime = msg?.time?.let { DateUtils.getRelativeTimeSpanString(it) }
            messageItemTimeView.text = relativeTime

//            val dialogView = LayoutInflater.from(itemView.context).inflate(R.layout.message_dialog,null,false)

            messageItem.setOnClickListener {
                Toast.makeText(
                    it.context,
                    "Clicked Item at ${this.adapterPosition}",
                    Toast.LENGTH_SHORT
                ).show()
                AlertDialog.Builder((itemView.context))
                    .setTitle(msg?.title)
                    .create()
            }
        }

        companion object {
            fun create(parent: ViewGroup): MessageHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.text_row_item, parent, false)
                return MessageHolder(view)
            }
        }
    }

    class WordsComparator : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.title == newItem.title
        }
    }
}