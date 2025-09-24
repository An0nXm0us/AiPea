package vcmsa.projects.wilproject

import android.util.Log // Import the Log class
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val dao: UserDao) : ViewModel() {
    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

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
               var user = dao.getUserByUsername(state.username)


                if (user == null && Patterns.EMAIL_ADDRESS.matcher(state.username).matches()) {
                    user = dao.getUserByEmail(state.username)
                    Log.d("LoginViewModel", "Initial username lookup failed. Attempting lookup by email.")
                }

                if (user == null) {
                    Log.d("LoginViewModel", "User lookup failed. User not found.")
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "User not found"
                        )
                    }
                    return@launch
                }

                Log.d("LoginViewModel", "User lookup successful. User found: ${user.email}")

                if (user.password != state.password) {
                    Log.d("LoginViewModel", "Password mismatch.")
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid password"
                        )
                    }
                    return@launch
                }

                Log.d("LoginViewModel", "Password match successful. Login complete.")

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

    class LoginViewModelFactory(private val userDao: UserDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(userDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
