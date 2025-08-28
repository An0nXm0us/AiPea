package vcmsa.projects.example66

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
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

class MainActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messageAdapter
        }
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
    }

    private fun sendMessage(message: String) {
        // Add user message
        val userMessage = Message(message, true)
        messages.add(userMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)

        // Show loading message
        val loadingMessage = Message("Thinking...", false)
        messages.add(loadingMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)

        // Get AI response in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(message)
                val aiResponse = response.text ?: "Sorry, I couldn't generate a response."

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    // Remove loading message
                    messages.removeAt(messages.size - 1)
                    messageAdapter.notifyItemRemoved(messages.size)

                    // Add AI response
                    val aiMessage = Message(aiResponse, false)
                    messages.add(aiMessage)
                    messageAdapter.notifyItemInserted(messages.size - 1)
                    chatRecyclerView.scrollToPosition(messages.size - 1)
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
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }
}