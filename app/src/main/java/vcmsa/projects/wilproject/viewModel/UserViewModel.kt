package vcmsa.projects.wilproject.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.firebase.UserRepo
import vcmsa.projects.wilproject.models.User
import vcmsa.projects.wilproject.event.UserEvent
import vcmsa.projects.wilproject.state.UserState
import java.security.MessageDigest

class UserViewModel(private val repository: UserRepo) : ViewModel() {
    private val _userState = MutableStateFlow(UserState())
    val userState = _userState.asStateFlow()
    companion object {
        fun provideFactory(repository: UserRepo): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserViewModel(repository) as T
            }
        }
    }
    fun hasPass(hashPassword : String): String
    {
        val bytes = hashPassword.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }


    suspend fun resetPassword(email: String, newPasswordPlain: String): Boolean {
        Log.d("ResetDebug", "Attempting reset for email: $email")
        return repository.resetUserPassword(email, hasPass(newPasswordPlain))
    }



    fun onEvent(event: UserEvent)
    {
        when(event){
            UserEvent.createUser -> {
                val fullName = userState.value.firstName
                val rawPassword = userState.value.password
                val email = userState.value.email
                val checkedEmail = _userState.value.isValid()

                if(fullName.isBlank() || rawPassword.isBlank() || email.isBlank())
                {
                    _userState.update { it.copy(errorMessage = "All fields are required.") }
                    return
                }
                if (!checkedEmail) {
                    _userState.update { it.copy(errorMessage = "Invalid email address.") }
                    return
                }

                viewModelScope.launch {
                    try {
                        repository.registerNewUser(
                            email = email,
                            rawPassword = rawPassword,
                            firstName = fullName
                        )
                        _userState.update { it.copy(
                            firstName = "",
                            password = "",
                            email = "",
                            isSuccess = true,
                            errorMessage = null
                        ) }
                    } catch (e: Exception) {
                        e.printStackTrace()

                        _userState.update { it.copy(
                            errorMessage = "Account creation failed: ${e.message}",
                            isSuccess = false
                        ) }
                    }
                }
            }
            is UserEvent.setEmail -> {

                _userState.update { it.copy(
                    email = event.email,
                    errorMessage = null
                ) }
            }
            is UserEvent.setFirstName -> {
                _userState.update { it.copy(
                    firstName = event.firstName,
                    errorMessage = null
                ) }
            }
            is UserEvent.setPassword -> {
                _userState.update { it.copy(
                    password= event.password,
                    errorMessage = null
                ) }
            }

        }
    }
}