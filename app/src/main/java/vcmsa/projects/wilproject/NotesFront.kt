package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesFront : AppCompatActivity() {

    private lateinit var adapter: NotesAdapter
    private lateinit var viewModel: NoteViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes_front)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // getDatabase
        val database = EddieDatabase.getDatabase(applicationContext)

        sessionManager = SessionManager(this)

        viewModel = ViewModelProvider(
            this,
            NoteViewModel.provideFactory(database.notesDao(), sessionManager)
        )[NoteViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeNotes()
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
        val userId = sessionManager.getUserId() ?: ""
        if (userId.isBlank()) return

        lifecycleScope.launch {

            val database = EddieDatabase.getDatabase(applicationContext)
            database.notesDao().getNotesByUserId(userId).collect { notes ->
                adapter.submitList(notes)
                // Log to see if notes are being loaded
                println("Loaded ${notes.size} notes for user $userId")
            }
        }
    }
}