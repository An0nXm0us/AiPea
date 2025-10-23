package vcmsa.projects.wilproject

import android.os.Bundle
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import vcmsa.projects.wilproject.adapter.ChatAdapter
import vcmsa.projects.wilproject.db.ChatDatabaseHelper
import vcmsa.projects.wilproject.models.ChatSession

class ChatHistory : AppCompatActivity() {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var newChatButton: FloatingActionButton
    private lateinit var newChatText: TextView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var dbHelper: ChatDatabaseHelper
    private lateinit var bottomNavigation: BottomNavigationView
    private val chatSessions = mutableListOf<ChatSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_history)

        dbHelper = ChatDatabaseHelper(this)
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation()
        loadAllChatSessions()
    }

    private fun initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        newChatButton = findViewById(R.id.newChatButton)
        newChatText = findViewById(R.id.newChatText)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatSessions) { chatSession ->
            openChat(chatSession)
        }
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatHistory)
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        newChatButton.setOnClickListener {
            createNewChat()
        }

        newChatText.setOnClickListener {
            createNewChat()
        }
    }

    private fun setupBottomNavigation() {
        // Set the AI item as selected since we're on the chat history page
        bottomNavigation.selectedItemId = R.id.btnAI

        // Set up navigation listener
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnHome -> {
                    // Navigate to HomePage
                    val intent = Intent(this, HomePage::class.java)
                    startActivity(intent)
                    finish() // Close current activity to avoid back stack issues
                    true
                }
                R.id.btnCalendar -> {
                    // Navigate to Calendar
                    val intent = Intent(this, CalenderFront::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnNotes -> {
                    // Navigate to Notes
                    val intent = Intent(this, NotesFront::class.java)
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
                R.id.btnAI -> {
                                       true
                }
                else -> false
            }
        }
    }

    private fun loadAllChatSessions() {
        // Load all chat sessions from database
        val sessionsFromDb = dbHelper.getAllChatSessionsWithMessages()
        chatSessions.clear()
        chatSessions.addAll(sessionsFromDb)

        if (chatSessions.isEmpty()) {
            // Show empty state or create a sample chat
            showEmptyState()
        }

        chatAdapter.notifyDataSetChanged()
    }

    private fun showEmptyState() {
        // You can add an empty state view here if needed
        // For now, we'll just leave it empty
    }

    private fun createNewChat() {
        val title = "New Chat"
        val newChatId = dbHelper.insertChatSession(title)

        val newChat = ChatSession(
            id = newChatId,
            title = title,
            messages = mutableListOf(),
            lastUpdated = System.currentTimeMillis()
        )

        chatSessions.add(0, newChat)
        chatAdapter.notifyItemInserted(0)
        openChat(newChat)
    }

    private fun openChat(chatSession: ChatSession) {
        val intent = Intent(this, MessageActivity::class.java).apply {
            putExtra("CHAT_ID", chatSession.id)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when returning from chat to show updated messages
        loadAllChatSessions()

        // Make sure the AI button stays selected when returning from MessageActivity
        bottomNavigation.selectedItemId = R.id.btnAI
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}