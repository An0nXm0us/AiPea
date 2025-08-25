package vcmsa.projects.wilproject

data class UserState (
    val firstName: String ="",
    val password: String ="",
    val email : String ="",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val confirmPassword: String = ""

) {
    fun isValid(): Boolean {
        return firstName.isNotBlank() && password.isNotBlank() && email.isNotBlank() && email.contains("@")
    }
}
