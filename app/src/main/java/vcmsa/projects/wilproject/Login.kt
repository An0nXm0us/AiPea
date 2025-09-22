package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Login : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnToLogin2: Button
    private lateinit var btngoToSigin: Button
    private lateinit var viewModel: LoginViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var database: EddieDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        database = Room.databaseBuilder(
            applicationContext,
            EddieDatabase::class.java,
            "eddieDB.db"
        ).build()
        sessionManager = SessionManager()

        // Get the UserDao from the database instance.
        val userDao = database.userDao()
        val sessionManager = SessionManager() //this here to make option later for staying logged in
        val viewModelFactory = LoginViewModel.LoginViewModelFactory(userDao)
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]

        etUsername = findViewById(R.id.etUserName)
        etPassword = findViewById(R.id.etPassword)
        btnToLogin2 = findViewById(R.id.btnToLogin2)
        btngoToSigin = findViewById(R.id.btnSignUp)

        setupEventListeners()
        observeViewModel()
    }

    private fun setupEventListeners() {
        //check what is being typed for error handling
        etUsername.doOnTextChanged { text, _, _, _ ->
            Log.d("TEXT_CHANGE", "Username text changed: $text")
            viewModel.onEvent(LoginEvent.checkUsername(text.toString()))
        }

        etPassword.doOnTextChanged { text, _, _, _ ->
            Log.d("TEXT_CHANGE", "Password text changed: $text")
            viewModel.onEvent(LoginEvent.checkPassword(text.toString()))
        }

        btnToLogin2.setOnClickListener {
            Log.d("LOGIN_CLICK", "Login button was clicked.")
            sessionManager.clearSession()
            // Trigger login event through ViewModel
            viewModel.onEvent(LoginEvent.Login)
        }

        btngoToSigin.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collectLatest { state ->
                // For proccess bar in case
           //     if (state.isLoading) {
                    // Show loading indicator if you have one
             //   } else {
                    // progressBar.visibility = View.GONE
            //}

                // Show error messages
                state.errorMessage?.let { error ->
                    Toast.makeText(this@Login, error, Toast.LENGTH_SHORT).show()
                }

                // Handle successful login
                if (state.isSuccess) {
                    //writes to the database
                    lifecycleScope.launch {
                        try {
                            val username = viewModel.loginState.value.username
                            val user = withContext(Dispatchers.IO) {
                                // Try to get user by username first
                                var user = database.userDao().getUserByUsername(username)

                                // If not found by username, try by email
                                if (user == null && Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
                                    user = database.userDao().getUserByEmail(username)
                                }
                                user
                            }

                            if (user != null) {
                                // Save user session
                                sessionManager.saveUserSession(user.userId, user.email, user.firstName)

                                Toast.makeText(this@Login, "Login successful!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@Login, HomePage::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this@Login, "User data not found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@Login, "Error retrieving user data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
