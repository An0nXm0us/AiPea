package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class HomePage : AppCompatActivity() {
    private lateinit var logOut: Button
    private lateinit var sessionManager: SessionManager
    private lateinit var cardAI: MaterialCardView
    private lateinit var cardNotes: MaterialCardView
    private lateinit var cardCalendar: MaterialCardView
    private lateinit var cardTextbook: MaterialCardView
    private lateinit var cardQuiz: MaterialCardView
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)
        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        // Initialize CardViews
        cardAI = findViewById(R.id.aiCard)
        cardNotes = findViewById(R.id.notesCard)
        cardCalendar = findViewById(R.id.calendarCard)
        cardTextbook = findViewById(R.id.textbookCard)
        cardQuiz = findViewById(R.id.quizCard)
        logOut = findViewById(R.id.btn_log_out)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupClickListeners() {
        // AI Card click listener
        cardAI.setOnClickListener {
            openChatHistory()
        }

        // Notes Card click listener
        cardNotes.setOnClickListener {
            openNotesFront()
        }

        // Calendar Card click listener
        cardCalendar.setOnClickListener {
            openCalendarFront()
        }

        // Textbook Card click listener
        cardTextbook.setOnClickListener {
            openTextbookFront()
        }

        // Quiz Card click listener
        cardQuiz.setOnClickListener {
            openQuizFront()
        }

        // Log Out Button
        logOut.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the current activity
            Toast.makeText(this, "Successfully logged off!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        // Set the home item as selected by default
        bottomNavigation.selectedItemId = R.id.btnHome

        // Set up navigation listener
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnHome -> {
                    // We're already on the home page, so just highlight the button
                    true
                }
                R.id.btnCalendar -> {
                    openCalendarFront()
                    true
                }
                R.id.btnNotes -> {
                    openNotesFront()
                    true
                }
                R.id.btnTextbook -> {
                    openTextbookFront()
                    true
                }
                R.id.btnAI -> {
                    openChatHistory()
                    true
                }
                else -> false
            }
        }
    }

    private fun openChatHistory() {
        val intent = Intent(this, ChatHistory::class.java)
        startActivity(intent)
    }

    private fun openNotesFront() {
        val intent = Intent(this, NotesFront::class.java)
        startActivity(intent)
    }

    private fun openCalendarFront() {
        val intent = Intent(this, CalenderFront::class.java)
        startActivity(intent)
    }

    private fun openTextbookFront() {
        val intent = Intent(this, TextbookList::class.java)
        startActivity(intent)
    }

    private fun openQuizFront() {
        Toast.makeText(this, "Quiz feature coming soon!", Toast.LENGTH_SHORT).show()
    }
}