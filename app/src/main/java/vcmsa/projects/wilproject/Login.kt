package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.room.Room
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.event.LoginEvent
import vcmsa.projects.wilproject.viewModel.LoginViewModel
import vcmsa.projects.wilproject.firebase.FirebaseDB
import vcmsa.projects.wilproject.firebase.LoginRepo

class Login : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnToLogin2: Button
    private lateinit var btngoToSigin: Button
    private lateinit var viewModel: LoginViewModel
    private lateinit var sessionManager: SessionManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val database = Room.databaseBuilder(
            applicationContext,
            EddieDatabase::class.java,
            "eddieDB.db"
        ).build()

        val userDao = database.userDao()
        val firebase = FirebaseDB()
        sessionManager = SessionManager (this)
        val authRepository = LoginRepo(userDao, firebase)


        val viewModelFactory = LoginViewModel.LoginViewModelFactory(authRepository,sessionManager)
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
            sessionManager.clearSession()

            viewModel.onEvent(LoginEvent.Login)
        }

        btngoToSigin.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }



        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            val intent = Intent(
                this,
                ForgotPassword::class.java
            )
            startActivity(intent)
        }
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collectLatest { state ->
                 state.errorMessage?.let { error ->
                    Toast.makeText(this@Login, error, Toast.LENGTH_SHORT).show()
                }


                if (state.isSuccess) {

                    Toast.makeText(this@Login, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@Login, HomePage::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}