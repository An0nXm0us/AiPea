package vcmsa.projects.wilproject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

class UserViewModel(private val dao: UserDao) : ViewModel() {
    private val _userState = MutableStateFlow(UserState())
    val userState = _userState.asStateFlow()
    fun onEvent(event: UserEvent)
    {
        when(event){
            is UserEvent.setConfirmPassword -> _userState.update { it.copy(confirmPassword = event.password) }

            is UserEvent.deleteUser -> {
                viewModelScope.launch{
                    dao.deleteUser(event.user)
                }
            }
            UserEvent.createUser -> {
                val fullName = userState.value.firstName
                val password = userState.value.password
                val email = userState.value.email

                if(fullName.isBlank() || password.isBlank() || email.isBlank())
                {
                    _userState.update { it.copy(errorMessage = "All fields are required.") }
                    return
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    _userState.update { it.copy(errorMessage = "Invalid email address.") }
                    return
                }

                val user = User(
                    firstName = fullName,
                    password = password,
                    email = email
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
    companion object {
        fun provideFactory(dao: UserDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserViewModel(dao) as T
            }
        }
    }
}
