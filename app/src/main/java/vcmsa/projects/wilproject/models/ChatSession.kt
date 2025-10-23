package vcmsa.projects.wilproject.models

data class ChatSession(
    val id: Long = 0,
    val title: String,
    val messages: MutableList<Message> = mutableListOf(),
    val lastUpdated: Long = System.currentTimeMillis()
)