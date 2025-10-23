package vcmsa.projects.wilproject.state

import vcmsa.projects.wilproject.models.Notes

data class NoteState (
    val userId : String = "",
    val title: String = "",
    val descripton: String = "",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val notes: List<Notes> = emptyList(),
    )
{

}