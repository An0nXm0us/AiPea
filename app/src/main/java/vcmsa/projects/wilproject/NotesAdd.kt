package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesAdd : AppCompatActivity() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var database: EddieDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes_add)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize database
        database = Room.databaseBuilder(
            applicationContext,
            EddieDatabase::class.java,
            "eddieDB.db"
        ).build()

        // Initialize session manager
        sessionManager = SessionManager(this)

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            NoteViewModel.provideFactory(database.notesDao(), sessionManager)
        )[NoteViewModel::class.java]

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val addButton = findViewById<android.widget.Button>(R.id.addButton)

        addButton.setOnClickListener {
            addNote()
            intent = Intent(this, NotesFront::class.java)
            startActivity(intent)
        }
    }

    private fun addNote() {
        val edTitle = findViewById<android.widget.EditText>(R.id.edTitle)
        val edDesc = findViewById<android.widget.EditText>(R.id.edDesc)

        val title = edTitle.text.toString()
        val description = edDesc.text.toString()

        if (title.isBlank() || description.isBlank()) {
            Toast.makeText(this,"Title or descrption must not be empty", Toast.LENGTH_LONG).show()
            return
        }

        // Use ViewModel to handle the note creation
        CoroutineScope(Dispatchers.Main).launch {
            try {

                viewModel.onEvent(NotesEvent.setTitle(title))
                viewModel.onEvent(NotesEvent.setDecription(description))

                viewModel.onEvent(NotesEvent.createNote)

                // Check if notes are successful and finish
                if (viewModel.noteState.value.isSuccess) {
                    finish()
                } else {
                    // need to show error message
                }
            } catch (e: Exception) {
                // error message atch
            }
        }
    }
}
