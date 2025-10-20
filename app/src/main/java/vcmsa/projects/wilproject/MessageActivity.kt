package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.wilproject.adapter.MessageAdapter
import vcmsa.projects.wilproject.db.ChatDatabaseHelper
import vcmsa.projects.wilproject.models.Message

class MessageActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var generativeModel: GenerativeModel
    private lateinit var dbHelper: ChatDatabaseHelper
    private var currentChatId: Long = -1
    private var isScrolledToBottom = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val apiKey = "AIzaSyDwsZhVi4kW0lGNPOdIyBlaXidirsbpFxw"
        generativeModel = GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey
        )

        dbHelper = ChatDatabaseHelper(this)
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadChatSession()
    }

    private fun initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        backButton = findViewById(R.id.backButton) // Initialize back button
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        val layoutManager = LinearLayoutManager(this@MessageActivity)
        chatRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = messageAdapter
        }

        chatRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                isScrolledToBottom =
                    firstVisibleItemPosition + visibleItemCount >= totalItemCount - 2
            }
        })
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageEditText.text.clear()
            }
        }

        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val message = messageEditText.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessage(message)
                    messageEditText.text.clear()
                }
                return@setOnEditorActionListener true
            }
            false
        }

        // Back button click listener
        backButton.setOnClickListener {
            navigateToChatHistory()
        }
    }

    private fun navigateToChatHistory() {
        val intent = Intent(this, ChatHistory::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish() // Close current activity
    }

    // Alternative: Call super first
    override fun onBackPressed() {
        super.onBackPressed() // Call super first to handle default behavior
        navigateToChatHistory()

    }

    private fun loadChatSession() {
        currentChatId = intent.getLongExtra("CHAT_ID", -1)

        if (currentChatId == -1L) {
            // Create new chat session if none provided
            currentChatId = dbHelper.insertChatSession("New Chat")
        } else {
            // Load ALL existing messages for this chat session
            val existingMessages = dbHelper.getMessagesForChatSession(currentChatId)
            messages.clear()
            messages.addAll(existingMessages)
            messageAdapter.notifyDataSetChanged()

            // Scroll to bottom to show latest messages
            if (messages.isNotEmpty()) {
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage(message: String) {
        // Add user message to UI
        val userMessage = Message(message, true)
        messages.add(userMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)

        // Save user message to database
        dbHelper.insertMessage(currentChatId, userMessage)

        // Scroll to bottom
        if (isScrolledToBottom) {
            chatRecyclerView.scrollToPosition(messages.size - 1)
        }

        // Update chat session title with first message if this is the first message
        if (messages.size == 1) {
            updateChatTitle(message)
        }

        // Show loading message
        val loadingMessage = Message("Thinking...", false)
        messages.add(loadingMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)

        if (isScrolledToBottom) {
            chatRecyclerView.scrollToPosition(messages.size - 1)
        }

        // Get AI response
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(message)
                val aiResponse = response.text ?: "Sorry, I couldn't generate a response."

                withContext(Dispatchers.Main) {
                    // Remove loading message
                    messages.removeAt(messages.size - 1)
                    messageAdapter.notifyItemRemoved(messages.size)

                    // Add AI response
                    val aiMessage = Message(aiResponse, false)
                    messages.add(aiMessage)
                    messageAdapter.notifyItemInserted(messages.size - 1)

                    // Save AI message to database
                    dbHelper.insertMessage(currentChatId, aiMessage)

                    if (isScrolledToBottom) {
                        chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Remove loading message
                    messages.removeAt(messages.size - 1)
                    messageAdapter.notifyItemRemoved(messages.size)

                    // Add error message
                    val errorMessage = Message("Error: ${e.message}", false)
                    messages.add(errorMessage)
                    messageAdapter.notifyItemInserted(messages.size - 1)

                    // Save error message to database
                    dbHelper.insertMessage(currentChatId, errorMessage)

                    if (isScrolledToBottom) {
                        chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }
    }

    private fun updateChatTitle(firstMessage: String) {
        val title = if (firstMessage.length > 20) {
            firstMessage.substring(0, 20) + "..."
        } else {
            firstMessage
        }
        dbHelper.updateChatSessionTitle(currentChatId, title)
    }

    override fun onPause() {
        super.onPause()
        if (messages.isNotEmpty() && currentChatId != -1L) {
            dbHelper.updateChatSessionLastUpdated(currentChatId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}