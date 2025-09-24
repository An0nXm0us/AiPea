package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
class SignUp : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var signupButton: Button
    val database = EddieDatabase.getDatabase(this)
    val userDao = database.userDao()
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
           EddieDatabase::class.java,
            "eddieDB.db"
        ).build()
    }

    private val viewModel by viewModels<UserViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Pass the correct userDao to the ViewModel
                return UserViewModel(userDao) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        // Initialize views
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        signupButton = findViewById(R.id.signup_button)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        EventListeners()
    }

    private fun EventListeners() {
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

        // Sign up button click
        signupButton.setOnClickListener {
            // Validate confirm password
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Clear error and proceed with signup
            etConfirmPassword.error = null
            viewModel.onEvent(UserEvent.createUser)

            // Show success message
            Toast.makeText(
                this,
                "Account created successfully!",
                Toast.LENGTH_LONG
            ).show()

            // Clears the form
            etFullName.text.clear()
            etEmail.text.clear()
            etPassword.text.clear()
            etConfirmPassword.text.clear()
            val intent : Intent
            intent =  Intent(this, SignUp::class.java)
            startActivity(intent)
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