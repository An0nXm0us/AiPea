package vcmsa.projects.wilproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import vcmsa.projects.wilproject.models.ChatSession
import vcmsa.projects.wilproject.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val chatSessions: List<ChatSession>,
    private val onItemClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val previewTextView: TextView = itemView.findViewById(R.id.previewTextView)
        val messageCountTextView: TextView = itemView.findViewById(R.id.messageCountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatSession = chatSessions[position]

        holder.titleTextView.text = chatSession.title
        holder.dateTextView.text = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
            .format(Date(chatSession.lastUpdated))

        // Show message count
        holder.messageCountTextView.text = "${chatSession.messages.size} messages"

        // Show last message preview
        val preview = if (chatSession.messages.isNotEmpty()) {
            val lastMessage = chatSession.messages.last()
            "${if (lastMessage.isUser) "You: " else "AI: "}${lastMessage.text.take(50)}${if (lastMessage.text.length > 50) "..." else ""}"
        } else {
            "No messages yet"
        }
        holder.previewTextView.text = preview

        holder.itemView.setOnClickListener {
            onItemClick(chatSession)
        }
    }

    override fun getItemCount(): Int = chatSessions.size
}