package vcmsa.projects.wilproject.event

sealed interface LoginEvent {
    data class checkUsername(val username: String) : LoginEvent
    data class checkPassword(val password: String) : LoginEvent
    object Login : LoginEvent
}