package vcmsa.projects.wilproject


class FakeSessionManager(private val userId: String = "testUser") {
    fun getUserId(): String = userId
    fun isLoggedIn(): Boolean = true
}
