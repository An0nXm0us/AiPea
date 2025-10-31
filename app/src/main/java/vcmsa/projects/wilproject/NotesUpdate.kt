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
import kotlinx.coroutines.CancellationException
//page for updating notes
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

        val database = EddieDatabase.getDatabase(applicationContext)
        val notesDao = database.notesDao()
        val firebaseConnect = FirebaseDB()
        sessionManager = SessionManager(this)

        val notesRepository = NotesRepo(notesDao, firebaseConnect)
        viewModel = ViewModelProvider(this, NoteViewModel.provideFactory(notesRepository, sessionManager))[NoteViewModel::class.java]


        // Retrieve the note ID passed from the NotesFront
        val noteId = intent.getStringExtra("NOTE_ID") ?: ""

        // Check if a valid note ID was passed
        if (noteId.isNotBlank()) {
            loadNote(noteId) // Proceed to load the note data
        } else {
            // No ID found, close the activity
            finish()
        }

        // Setup UI click listeners
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
            }
            catch (e: CancellationException){

            }
            catch (e: Exception) {
                // Handle potential errors
                e.printStackTrace()
                Toast.makeText(this@NotesUpdate, "Error loading note.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    //Populates the EditText fields with the details of the retrieved note that is going to be updated.

    private fun populateNoteData(note: Notes) {
        findViewById<android.widget.EditText>(R.id.updateTitle).setText(note.title)
        findViewById<android.widget.EditText>(R.id.updateDesc).setText(note.description)
    }

    //cClick listeners for the update button and the back button.
    private fun setupClickListeners() {

        findViewById<android.widget.Button>(R.id.updateBtn).setOnClickListener {
            updateNote() // Call the function to handle the update logic
        }

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // Navigate back to the previous screen
        }
    }

    //Handles the process of updating the note.

    private fun updateNote() {
        // Get the updated title from the EditText
        val title = findViewById<android.widget.EditText>(R.id.updateTitle).text.toString()
        // Get the updated description from the EditText
        val description = findViewById<android.widget.EditText>(R.id.updateDesc).text.toString()

        // Input validation check
        if (title.isBlank() || description.isBlank()) {
            Toast.makeText(this,"Title or description must not be empty", Toast.LENGTH_LONG).show()
            return
        }

        // Use the currentNote/old note data for reference
        currentNote?.let { oldNote ->
            // Create a copy of the old note with the new title and description
            val updatedNote = oldNote.copy(
                title = title,
                description = description

            )

            // Perform the database operation asynchronously
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