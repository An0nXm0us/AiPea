
package vcmsa.projects.wilproject.viewModel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.SessionManager
import vcmsa.projects.wilproject.state.LoginState
import vcmsa.projects.wilproject.firebase.LoginRepo
import vcmsa.projects.wilproject.event.LoginEvent
import vcmsa.projects.wilproject.models.User

/**
 * This view model will be using both its repective dao and repo methods to save locally and on firebase
 * (Tadas Petra.2024 & Philipp Lackner,2023)
 * The state wil be used and reflected on ui (Philipp Lackner,2022)
 **/
class LoginViewModel(private val repository: LoginRepo, private val sessionManager: SessionManager ) : ViewModel() {
    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private var _loggedInUser: User? = null


    fun onEvent(event: LoginEvent) {
        when (event) {
            // Updates the username field in the state and clears any previous error message.
            is LoginEvent.checkUsername -> {
                _loginState.update { it.copy(username = event.username, errorMessage = null) }
            }
            // Updates the password field in the state and clears any previous error message.
            is LoginEvent.checkPassword -> {
                _loginState.update { it.copy(password = event.password, errorMessage = null) }
            }
            // Triggers the standard username/password login attempt.
            LoginEvent.Login -> {
                attemptLogin()
            }
            // Triggers the logic for handling a successful Google authentication result.
            is LoginEvent.GoogleSignInSuccess -> {
                handleGoogleSignIn(event.googleUserId, event.email, event.displayName)
            }
        }
    }

    //Executes the standard local/online login attempt.

    private fun attemptLogin() {
        val state = loginState.value
        Log.d("LoginViewModel", "Attempting login for user: ${state.username}")

        // Input validation
        if (state.username.isBlank()) {
            _loginState.update { it.copy(errorMessage = "Username is required") }
            Log.d("LoginViewModel", "Username is blank.")
            return
        }

        if (state.password.isBlank()) {
            _loginState.update { it.copy(errorMessage = "Password is required") }
            Log.d("LoginViewModel", "Password is blank.")
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Attempt  login
                val user = repository.attemptLocalLogin(state.username, state.password)

                if (user == null) {
                    Log.d("LoginViewModel", "User lookup failed or credentials invalid.")
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid username or password"
                        )
                    }
                    return@launch
                }

                // Save user session  if login is successful.
                sessionManager.saveUserSession(userId = user.userId, email = user.email, name = user.firstName)
                Log.d("LoginViewModel", "Login successful.")
                _loggedInUser = user

                // Update state to success, triggering navigation/UI change.
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                // Handle unexpected errors during the login process.
                Log.e("LoginViewModel", "Login attempt failed with exception: ${e.message}")
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Login failed: ${e.message}"
                    )
                }
            }

        }
    }


    /**
     * Handles the successful result of a Google/Firebase sign-in flow.
     */
    private fun handleGoogleSignIn(googleUserId: String, email: String, displayName: String) {
        Log.d("LoginViewModel", "Attempting Google Sign-In for user: $email")

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch or create user record based on Google ID.
                val user = repository.handleGoogleSignIn(googleUserId, email, displayName)

                // Save user session details.
                sessionManager.saveUserSession(userId = user.userId, email = user.email, name = user.firstName)

                Log.d("LoginViewModel", "Google Sign-In successful. Login complete.")
                _loggedInUser = user

                // Update state to success.
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                // Handle errors during user record synchronization or session creation.
                Log.e("LoginViewModel", "Google Sign-In failed with exception: ${e.message}")
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Google Sign-In failed: ${e.message}"
                    )
                }
            }
        }
    }

    class LoginViewModelFactory(private val repository: LoginRepo, private val sessionManager: SessionManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(repository,sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
