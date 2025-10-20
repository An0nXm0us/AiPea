package vcmsa.projects.wilproject.state
import vcmsa.projects.wilproject.models.User
sealed interface UserEvent {
    object createUser : UserEvent
    data class setFirstName(val firstName: String):UserEvent
    data class setPassword(val password: String):UserEvent
    data class setEmail(val email: String):UserEvent
    data class deleteUser(val user : User):UserEvent

}