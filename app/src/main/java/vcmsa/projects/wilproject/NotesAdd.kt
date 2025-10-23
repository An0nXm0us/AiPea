package vcmsa.projects.wilproject

import android.content.Intent
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
import vcmsa.projects.wilproject.event.NotesEvent
import vcmsa.projects.wilproject.viewModel.NoteViewModel
import vcmsa.projects.wilproject.firebase.NotesRepo
import vcmsa.projects.wilproject.firebase.FirebaseDB

class NotesAdd : AppCompatActivity() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes_add)
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

        viewModel = ViewModelProvider(
            this,
            NoteViewModel.provideFactory(notesRepository, sessionManager)
        )[NoteViewModel::class.java]

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        val addButton = findViewById<android.widget.Button>(R.id.addButton)

        addButton.setOnClickListener {
            addNote()
        }
    }

    private fun addNote() {
        val edTitle = findViewById<android.widget.EditText>(R.id.edTitle)
        val edDesc = findViewById<android.widget.EditText>(R.id.edDesc)

        val title = edTitle.text.toString()
        val description = edDesc.text.toString()

        if (title.isBlank() || description.isBlank()) {
            Toast.makeText(this,"Title or description must not be empty", Toast.LENGTH_LONG).show()
            return
        }


        viewModel.onEvent(NotesEvent.setTitle(title))
        viewModel.onEvent(NotesEvent.setDecription(description))
        viewModel.onEvent(NotesEvent.createNote)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.noteState.collect { state ->

                if (state.isSuccess) {
                    Toast.makeText(this@NotesAdd, "Note saved!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@NotesAdd, NotesFront::class.java)
                    startActivity(intent)
                    finish()
                }

                state.errorMessage?.let { error ->
                    Toast.makeText(this@NotesAdd, error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}