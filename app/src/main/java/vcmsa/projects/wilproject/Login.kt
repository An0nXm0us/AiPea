package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnToLogin2: Button
    private lateinit var btngoToSigin: Button
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

       val database = EddieDatabase.getDatabase(this)

        // Get the UserDao from the database instance.
        val userDao = database.userDao()

        // Pass the correct userDao to the ViewModelFactory.
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
            viewModel.onEvent(LoginEvent.Login)
        }

        btngoToSigin.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collectLatest { state ->
                state.errorMessage?.let {
                    Toast.makeText(this@Login, it, Toast.LENGTH_SHORT).show()
                }
                if (state.isSuccess) {
                    Toast.makeText(this@Login, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@Login, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
