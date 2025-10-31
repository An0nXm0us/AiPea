package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.adapter.NotesAdapter
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.event.NotesEvent
import vcmsa.projects.wilproject.viewModel.NoteViewModel
import vcmsa.projects.wilproject.firebase.NotesRepo
import vcmsa.projects.wilproject.firebase.FirebaseDB
class NotesFront : AppCompatActivity() {

    private lateinit var adapter: NotesAdapter
    private lateinit var viewModel: NoteViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var bottomNavigation: BottomNavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes_front)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bottomNavigation = findViewById(R.id.bottom_navigation)


        val database = EddieDatabase.getDatabase(applicationContext)
        val notesDao = database.notesDao()
        val firebaseConnect = FirebaseDB()

        sessionManager = SessionManager(this)

        // Create repository
        val notesRepository = NotesRepo(notesDao, firebaseConnect)
        viewModel = ViewModelProvider(this, NoteViewModel.provideFactory(notesRepository, sessionManager) )[NoteViewModel::class.java] // Get the NoteViewModel instance

        setupRecyclerView()
        setupBottomNavigation()
        setupClickListeners()
        observeNotes()

    }

    //navigation to other pages
    private fun setupBottomNavigation() {

        bottomNavigation.selectedItemId = R.id.btnNotes

        // Set the listener for when a navigation item is selected
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnHome -> {
                    // Navigate to the HomePage
                    startActivity(Intent(this, HomePage::class.java))
                    finish() // Close the current
                    true
                }
                R.id.btnCalendar -> {
                    // Navigate to the CalenderFront
                    startActivity(Intent(this, CalenderFront::class.java))
                    finish()
                    true
                }
                R.id.btnAI -> {
                    // Navigate to the ChatHistory
                    startActivity(Intent(this, ChatHistory::class.java))
                    finish()
                    true
                }
                R.id.btnTextbook -> {
                    // Navigate to the TextbookList
                    startActivity(Intent(this, TextbookList::class.java))
                    finish()
                    true
                }
                R.id.btnNotes -> {
                    // Already on the Notes screen, do nothing
                    true
                }
                else -> false
            }
        }
    }


    private fun setupRecyclerView() {
        // Initialize the adapter with click and delete handlers
        adapter = NotesAdapter(
            // Lambda for handling a click on a note item
            onNoteClick = { note ->
                val intent = Intent(this, NotesUpdate::class.java).apply {
                    // Pass the ID of the clicked note to the update activity
                    putExtra("NOTE_ID", note.noteId)
                }
                startActivity(intent)
            },
            // Lambda for handling the delete action on a note item
            onNoteDelete = { note ->
                // Dispatch a deleteNotes event to the ViewModel
                viewModel.onEvent(NotesEvent.deleteNotes(note))
            }
        )

        // Find the RecyclerView in the layout and apply configurations
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@NotesFront)
            adapter = this@NotesFront.adapter
        }
    }

    /**
     * Sets up click listeners for interactive UI elements.
     */
    private fun setupClickListeners() {
        // Set a click listener for the Button
        findViewById<FloatingActionButton>(R.id.floatingId).setOnClickListener {
            // Navigate to the NotesAdd activity to create a new note
            startActivity(Intent(this, NotesAdd::class.java))
        }
    }

    /**
     * Observes the note list from the ViewModel's state flow and updates the adapter.
     */
    private fun observeNotes() {

        lifecycleScope.launch {
            viewModel.noteState.collect { state ->
                // Submit the new list of notes from the state to the adapter
                adapter.submitList(state.notes)
            }
        }
    }
}