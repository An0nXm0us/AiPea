package vcmsa.projects.wilproject

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class NotesUpdate : AppCompatActivity() {

    private lateinit var database: EddieDatabase
    private var currentNote: Notes? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes_update)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = EddieDatabase.getDatabase(applicationContext)

        // Get note ID from intent
        val noteId = intent.getStringExtra("NOTE_ID") ?: ""

        if (noteId.isNotBlank()) {
            loadNote(noteId)
        } else {
            finish() // No note ID is provided
        }

        setupClickListeners()
    }

    private fun loadNote(noteId: String) {
        lifecycleScope.launch {
            try {
                // Get the note from database
                val notesFlow = database.notesDao().getNoteById(noteId)
                val notesList = notesFlow.first()

                if (notesList.isNotEmpty()) {
                    currentNote = notesList[0]
                    populateNoteData(currentNote!!)
                } else {
                    finish() // Note not found
                }
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }
    }

    private fun populateNoteData(note: Notes) {
        findViewById<android.widget.EditText>(R.id.updateTitle).setText(note.title)
        findViewById<android.widget.EditText>(R.id.updateDesc).setText(note.description)
    }

    private fun setupClickListeners() {
        findViewById<android.widget.Button>(R.id.updateBtn).setOnClickListener {
            updateNote()
        }
    }

    private fun updateNote() {
        val title = findViewById<android.widget.EditText>(R.id.updateTitle).text.toString()
        val description = findViewById<android.widget.EditText>(R.id.updateDesc).text.toString()

        if (title.isBlank() || description.isBlank()) {
            // Show error message
            return
        }

        currentNote?.let { note ->
            // Create updated note
            val updatedNote = note.copy(
                title = title,
                description = description
            )

            lifecycleScope.launch {
                try {
                    // Delete old note and insert updated one
                    database.notesDao().deleteNote(note)
                    database.notesDao().insertNote(updatedNote)

                    // Return to previous activity
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Show error message
                }
            }
        }
    }
}