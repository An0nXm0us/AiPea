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
class LoginViewModel(private val repository: LoginRepo, private val sessionManager: SessionManager ) : ViewModel() {
    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private var _loggedInUser: User? = null


    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.checkUsername -> {
                _loginState.update { it.copy(username = event.username, errorMessage = null) }
            }
            is LoginEvent.checkPassword -> {
                _loginState.update { it.copy(password = event.password, errorMessage = null) }
            }
            LoginEvent.Login -> {
                attemptLogin()
            }
            is LoginEvent.GoogleSignInSuccess -> {
                handleGoogleSignIn(event.googleUserId, event.email, event.displayName)
            }
        }
    }

    private fun attemptLogin() {
        val state = loginState.value
        Log.d("LoginViewModel", "Attempting login for user: ${state.username}")

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
                sessionManager.saveUserSession(userId = user.userId, email = user.email, name = user.firstName)
                Log.d("LoginViewModel", "Login successful.")
                _loggedInUser = user
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
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


    private fun handleGoogleSignIn(googleUserId: String, email: String, displayName: String) {
        Log.d("LoginViewModel", "Attempting Google Sign-In for user: $email")

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = repository.handleGoogleSignIn(googleUserId, email, displayName)
                sessionManager.saveUserSession(userId = user.userId, email = user.email, name = user.firstName)

                Log.d("LoginViewModel", "Google Sign-In successful. Login complete.")
                _loggedInUser = user
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
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