package vcmsa.projects.wilproject

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.event.NotesEvent
import vcmsa.projects.wilproject.viewModel.NoteViewModel
import vcmsa.projects.wilproject.firebase.NotesRepo
import vcmsa.projects.wilproject.firebase.FirebaseDB

class NotesUpdate : AppCompatActivity() {
    private lateinit var viewModel: NoteViewModel
    private lateinit var sessionManager: SessionManager
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

        // Initialize dependencies
        val database = EddieDatabase.getDatabase(applicationContext)
        val notesDao = database.notesDao()
        val firebaseConnect = FirebaseDB()
        sessionManager = SessionManager(this)


        val notesRepository = NotesRepo(notesDao, firebaseConnect)
        viewModel = ViewModelProvider(
            this,
            NoteViewModel.provideFactory(notesRepository, sessionManager)
        )[NoteViewModel::class.java]


        val noteId = intent.getStringExtra("NOTE_ID") ?: ""

        if (noteId.isNotBlank()) {
            loadNote(noteId)
        } else {
            finish()
        }

        setupClickListeners()
    }

    private fun loadNote(noteId: String) {
        lifecycleScope.launch {
            try {

                viewModel.noteState.collect { state ->
                    val note = state.notes.find { it.noteId == noteId }
                    if (note != null && currentNote == null) {
                        currentNote = note
                        populateNoteData(note)
                    } else if (currentNote == null) {
                    }
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
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun updateNote() {
        val title = findViewById<android.widget.EditText>(R.id.updateTitle).text.toString()
        val description = findViewById<android.widget.EditText>(R.id.updateDesc).text.toString()

        if (title.isBlank() || description.isBlank()) {
            Toast.makeText(this,"Title or description must not be empty", Toast.LENGTH_LONG).show()
            return
        }

        currentNote?.let { oldNote ->
            val updatedNote = oldNote.copy(
                title = title,
                description = description
            )


            lifecycleScope.launch {
                viewModel.onEvent(NotesEvent.deleteNotes(oldNote))
                viewModel.onEvent(NotesEvent.setTitle(updatedNote.title))
                viewModel.onEvent(NotesEvent.setDecription(updatedNote.description))
                viewModel.onEvent(NotesEvent.createNote)
                finish()
            }
        }
    }
}