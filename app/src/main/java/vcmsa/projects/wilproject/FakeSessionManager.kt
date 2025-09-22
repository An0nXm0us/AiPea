package vcmsa.projects.wilproject

class FakeSessionManager(private val userId: String = "testUser") : SessionManager(

) {
    override fun getUserId(): String = userId
    // stub any other methods if you have them
}
