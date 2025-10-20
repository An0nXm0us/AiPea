package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.dao.UserDao
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.state.UserEvent
import vcmsa.projects.wilproject.viewModel.UserViewModel

class SignUp : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var signupButton: Button
    private lateinit var loginRedirectText: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var database: EddieDatabase
    private lateinit var userDao: UserDao

    // initialize for ViewModel
    private val viewModel: UserViewModel by lazy {
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserViewModel(userDao) as T
            }
        }
        ViewModelProvider(this, factory)[UserViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        // Initialize database and DAO
        database = EddieDatabase.getDatabase(applicationContext)
        userDao = database.userDao()
        sessionManager = SessionManager(this)

        // Initialize views
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        signupButton = findViewById(R.id.signup_button)
        loginRedirectText = findViewById(R.id.loginRedirectText)

        // Redirect to home page if the user is already logged in
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, HomePage::class.java))
            finish()
            return
        }

        // FIX: Use the root view of the layout instead of R.id.main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupEventListeners()
        observeViewModel()  // observe state of the view model
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
            // nothing happens here cos we are not saving this to db
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

        // Add click listener for login redirect
        loginRedirectText.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun observeViewModel() {
        // Observe the ViewModel state for success/error
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

                    // Redirect to Login page instead of SignUp
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
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) {
            listener(s.toString())
        }
    })
}