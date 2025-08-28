package vcmsa.projects.example66

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userMessageLayout: LinearLayout = itemView.findViewById(R.id.userMessageLayout)
        val userMessageText: TextView = itemView.findViewById(R.id.userMessageText)
        val aiMessageLayout: LinearLayout = itemView.findViewById(R.id.aiMessageLayout)
        val aiMessageText: TextView = itemView.findViewById(R.id.aiMessageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            holder.userMessageLayout.visibility = View.VISIBLE
            holder.aiMessageLayout.visibility = View.GONE
            holder.userMessageText.text = message.text
        } else {
            holder.userMessageLayout.visibility = View.GONE
            holder.aiMessageLayout.visibility = View.VISIBLE
            holder.aiMessageText.text = message.text
        }
    }

    override fun getItemCount(): Int = messages.size
}