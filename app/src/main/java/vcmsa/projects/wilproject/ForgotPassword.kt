package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.viewModel.UserViewModel // Import the ViewModel
class ForgotPassword : AppCompatActivity() {
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSuccessMessage: TextView
    private lateinit var tvErrorMessage: TextView // New TextView for specific errors

    // Use the ViewModel instead of direct DAO access
    private lateinit var viewModel: UserViewModel

    private lateinit var userEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)


        // Initialize database and ViewModel
        val database = EddieDatabase.getDatabase(applicationContext)
        viewModel = ViewModelProvider(
            this,
            UserViewModel.provideFactory(database.userDao()) // Use the factory from UserViewModel
        )[UserViewModel::class.java]

        initializeViews()
        setupClickListeners()
        setupPasswordValidation()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        userEmail = findViewById(R.id.etEmail)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage)
        // Assuming you add an ID for an error message TextView in your layout
        // tvErrorMessage = findViewById(R.id.tvErrorMessage)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnResetPassword.setOnClickListener {
            handlePasswordReset()
        }
    }

    private fun setupPasswordValidation() {
        // ... (validation logic remains the same)
        val passwordTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePasswords()
            }
        }

        etNewPassword.addTextChangedListener(passwordTextWatcher)
        etConfirmPassword.addTextChangedListener(passwordTextWatcher)
    }

    private fun validatePasswords() {
        // ... (validation logic remains the same)
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        val doPasswordsMatch =
            newPassword == confirmPassword && newPassword.length >= 6 // Recommend min length check

        btnResetPassword.isEnabled = doPasswordsMatch
        btnResetPassword.alpha = if (btnResetPassword.isEnabled) 1.0f else 0.5f

        if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
            etConfirmPassword.error = "Passwords do not match"
        } else {
            etConfirmPassword.error = null
        }
    }

    private fun handlePasswordReset() {
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        val email = userEmail.text.toString()
        // Final validation
        if (email.isBlank()) {
            showError("Cannot reset password: User email is missing.")
            return
        }
        if (newPassword.length < 6) { // Basic length check
            showError("Password must be at least 6 characters.")
            return
        }
        if (newPassword != confirmPassword) {
            showError("Passwords do not match.")
            return
        }

        // Show loading state
        setLoadingState(true)

        lifecycleScope.launch {
            // 1. Call the new function in the ViewModel
            val success = viewModel.resetPassword(userEmail.text.toString(), newPassword)

            // Hide loading state
            setLoadingState(false)

            if (success) {
                showSuccessMessage()
                // Navigate to login after delay
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToLogin()
                }, 3000)
            } else {
                // 2. Updated error handling for specific failure
                showError("Failed to reset password. User may not exist or database error.")
            }
        }
    }

    // REMOVE the manual updatePasswordInDatabase function

    private fun setLoadingState(isLoading: Boolean) {
        btnResetPassword.isEnabled = !isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnResetPassword.text = if (isLoading) "Resetting..." else "Reset Password"

        etNewPassword.isEnabled = !isLoading
        etConfirmPassword.isEnabled = !isLoading
    }

    private fun showSuccessMessage() {
        tvSuccessMessage.visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.formSection)?.visibility = View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToLogin()
    }
}