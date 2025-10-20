package vcmsa.projects.wilproject.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.dao.UserDao
import vcmsa.projects.wilproject.models.User
import vcmsa.projects.wilproject.state.UserEvent
import vcmsa.projects.wilproject.state.UserState
import java.security.MessageDigest

class UserViewModel(private val dao: UserDao) : ViewModel() {
    private val _userState = MutableStateFlow(UserState())
    val userState = _userState.asStateFlow()
    companion object {
        fun provideFactory(dao: UserDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Ensure the ViewModel class is correct here
                return UserViewModel(dao) as T
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
        // 1. Hash the new password using the existing utility function
        val hashedPassword = hasPass(newPasswordPlain)
        Log.d("ResetDebug", "Attempting reset for email: $email")
        return try {
            val user = dao.getUserByEmail(email) // Get the user object

            user?.let {
                // Create a copy of the user with the new hashed password
                Log.d("ResetDebug", "User found. Updating password.")
                val updatedUser = it.copy(password = hashedPassword)
                dao.updateUser(updatedUser) // Update the user in the database
                true // Success
            } ?: false // User not found
        } catch (e: Exception) {
            Log.e("ResetDebug", "Database UPDATE FAILED: ${e.message}", e)
            e.printStackTrace()
            false // Failure due to exception
        }
    }
    fun onEvent(event: UserEvent)
    {
        when(event){
            is UserEvent.deleteUser -> {
                viewModelScope.launch {
                    dao.deleteUser(event.user)
                }
            }
            UserEvent.createUser -> {
                val fullName = userState.value.firstName
                val password = userState.value.password
                val email = userState.value.email
                val checkedEmail = _userState.equals(UserState::isValid)
                val hashedPassword = hasPass(password)
                if(fullName.isBlank() || password.isBlank() || email.isBlank())
                {
                    _userState.update { it.copy(errorMessage = "All fields are required.") }
                    return
                }
                if (checkedEmail) {
                    _userState.update { it.copy(errorMessage = "Invalid email address.") }
                    return
                }


                val user = User(
                    firstName = fullName,
                    email = email,
                    password = hashedPassword,

                    )


                viewModelScope.launch {
                    try {
                        dao.upsertUser(user)
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
   /*
   *  companion object {
        fun provideFactory(dao: UserDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserViewModel(dao) as T
            }
        }
    }
   * */
}