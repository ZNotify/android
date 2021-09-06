package top.learningman.mipush.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        holder.bind(current.title)
    }

    class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageItemView: TextView = itemView.findViewById(R.id.textView)

        fun bind(text: String?) {
            messageItemView.text = text
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