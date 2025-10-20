package vcmsa.projects.wilproject.state

data class NoteState (
    val userId : String = "",
    val title: String = "",
    val descripton: String = "",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
    )
{

}