package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.room.Room
import vcmsa.projects.wilproject.dao.UserDao
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.event.UserEvent
import vcmsa.projects.wilproject.viewModel.UserViewModel
import vcmsa.projects.wilproject.firebase.UserRepo // Import the repository
import vcmsa.projects.wilproject.firebase.FirebaseDB // Import Firebase dependency

class SignUp : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var signupButton: Button
    private lateinit var loginRedirectText: TextView
    private lateinit var sessionManager: SessionManager
    private val viewModel: UserViewModel by lazy {

        val database = EddieDatabase.getDatabase(applicationContext)
        val userDao = database.userDao()
        val firebaseConnect = FirebaseDB()
        val userRepo = UserRepo(userDao, firebaseConnect)
        val factory = UserViewModel.provideFactory(userRepo)

        ViewModelProvider(this, factory)[UserViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        sessionManager = SessionManager(this)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        signupButton = findViewById(R.id.signup_button)
        loginRedirectText = findViewById(R.id.loginRedirectText)

        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, HomePage::class.java))
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupEventListeners()
        observeViewModel()
    }

    private fun setupEventListeners() {
        etFullName.onTextChanged { text ->
            viewModel.onEvent(UserEvent.setFirstName(text))
        }

        etEmail.onTextChanged { text ->
            viewModel.onEvent(UserEvent.setEmail(text))
        }

        etPassword.onTextChanged { text ->
            viewModel.onEvent(UserEvent.setPassword(text))
        }

        etConfirmPassword.onTextChanged { text ->
        }

        signupButton.setOnClickListener {
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            etConfirmPassword.error = null
            viewModel.onEvent(UserEvent.createUser)
        }

        loginRedirectText.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.userState.collect { state ->
                // Handle errors
                state.errorMessage?.let { error ->
                    Toast.makeText(this@SignUp, error, Toast.LENGTH_SHORT).show()
                }

                // Handle success
                if (state.isSuccess) {
                    Toast.makeText(
                        this@SignUp,
                        "Account created successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Clear the form
                    etFullName.text.clear()
                    etEmail.text.clear()
                    etPassword.text.clear()
                    etConfirmPassword.text.clear()

                    // Redirect to Login
                    val intent = Intent(this@SignUp, Login::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

fun android.widget.EditText.onTextChanged(listener: (String) -> Unit) {
    this.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) {
            listener(s.toString())
        }
    })
}