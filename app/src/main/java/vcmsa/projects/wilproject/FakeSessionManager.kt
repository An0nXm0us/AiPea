package vcmsa.projects.wilproject

// Notice: we don't extend SessionManager, just create a stand-in
class FakeSessionManager(private val userId: String = "testUser") {
    fun getUserId(): String = userId
    fun isLoggedIn(): Boolean = true
}
