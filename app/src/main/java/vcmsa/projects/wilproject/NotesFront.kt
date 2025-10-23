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


        val notesRepository = NotesRepo(notesDao, firebaseConnect)


        viewModel = ViewModelProvider(
            this,
            NoteViewModel.provideFactory(notesRepository, sessionManager)
        )[NoteViewModel::class.java]

        setupRecyclerView()
        setupBottomNavigation()
        setupClickListeners()

        observeNotes()

    }
    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.btnNotes

        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnHome -> {
                    val intent = Intent(this, HomePage::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnCalendar -> {
                    val intent = Intent(this, CalenderFront::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnAI -> {
                    val intent = Intent(this, ChatHistory::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnTextbook -> {
                    val intent = Intent(this, TextbookList::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnNotes -> {
                    true
                }
                else -> false
            }
        }
    }
    private fun setupRecyclerView() {
        adapter = NotesAdapter(
            onNoteClick = { note ->
                val intent = Intent(this, NotesUpdate::class.java).apply {
                    putExtra("NOTE_ID", note.noteId)
                }
                startActivity(intent)
            },
            onNoteDelete = { note ->
                viewModel.onEvent(NotesEvent.deleteNotes(note))
            }
        )

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@NotesFront)
            adapter = this@NotesFront.adapter
        }
    }

    private fun setupClickListeners() {
        findViewById<FloatingActionButton>(R.id.floatingId).setOnClickListener {
            startActivity(Intent(this, NotesAdd::class.java))
        }
    }

    private fun observeNotes() {

        lifecycleScope.launch {
            viewModel.noteState.collect { state ->
                adapter.submitList(state.notes)
            }
        }
    }
}