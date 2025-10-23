package vcmsa.projects.wilproject.event

sealed interface LoginEvent {
    data class checkUsername(val username: String) : LoginEvent
    data class checkPassword(val password: String) : LoginEvent
    object Login : LoginEvent
    data class GoogleSignInSuccess(val googleUserId: String, val email: String, val displayName: String) : LoginEvent
}