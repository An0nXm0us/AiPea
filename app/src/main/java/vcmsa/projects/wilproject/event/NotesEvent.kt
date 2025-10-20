package vcmsa.projects.wilproject.event

import vcmsa.projects.wilproject.models.Notes

sealed interface NotesEvent {
    //events that are triggered that will be used to save to the db
    object createNote : NotesEvent
    data class setUserID(val userId: String):NotesEvent
    data class setTitle(val title: String):NotesEvent
    data class setDecription(val decription: String):NotesEvent
    data class deleteNotes(val note : Notes):NotesEvent
}