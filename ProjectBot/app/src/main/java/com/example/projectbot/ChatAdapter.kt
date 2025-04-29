package com.example.projectbot



import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.projectbot.R


class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val imageView: ImageView = itemView.findViewById(R.id.messageImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageTextView.text = message.message
        if(message.imagePath != null){
            holder.imageView.visibility = View.VISIBLE;
            holder.imageView.setImageURI(Uri.parse(message.imagePath))
        } else {
            holder.imageView.visibility = View.GONE;
        }

        if (message.isUser) {
            holder.itemView.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.user_chat_bubble)
            holder.messageTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
        } else {
            holder.itemView.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bot_chat_bubble)
            holder.messageTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}