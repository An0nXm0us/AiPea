package vcmsa.projects.wilproject.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.R

class NotesAdapter(
    private val onNoteClick: (Notes) -> Unit,
    private val onNoteDelete: (Notes) -> Unit
) : ListAdapter<Notes, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_notes_adapter, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.noteTitleTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.noteDescriptionTextView)
        //private val dateTextView: TextView = itemView.findViewById(R.id.noteDateTextView)
        private val update = itemView.findViewById<LinearLayout>(R.id.note_content)
        fun bind(note: Notes) {

            titleTextView.text = note.title
            descriptionTextView.text = note.description


            //dateTextView.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(this)

            itemView.setOnClickListener {
                onNoteClick(note)
            }
            update.setOnClickListener {
                onNoteClick(note)
            }

            itemView.findViewById<View>(R.id.deleteNoteButton).setOnClickListener {
                onNoteDelete(note)
            }
        }
    }
}

class NoteDiffCallback : DiffUtil.ItemCallback<Notes>() {
    override fun areItemsTheSame(oldItem: Notes, newItem: Notes): Boolean {
        return oldItem.noteId == newItem.noteId
    }

    override fun areContentsTheSame(oldItem: Notes, newItem: Notes): Boolean {
        return oldItem == newItem
    }
}
