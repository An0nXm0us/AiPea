package vcmsa.projects.wilproject.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.state.NoteState
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.dao.NotesDao
import vcmsa.projects.wilproject.event.NotesEvent
import vcmsa.projects.wilproject.SessionManager

class NoteViewModel (private val dao: NotesDao, private val sessionManager: SessionManager) : ViewModel() {
    private val _noteState = MutableStateFlow(NoteState())
    val noteState = _noteState.asStateFlow()

    init {
        // put user id automatically
        val userId = sessionManager.getUserId()
        if (userId != null) {
            _noteState.update { it.copy(userId = userId) }
        }
    }

    fun onEvent(event: NotesEvent)
    {
      when(event){
          is NotesEvent.deleteNotes -> {
              viewModelScope.launch {
                    dao.deleteNote(event.note)
              }
          }
          NotesEvent.createNote -> {
                  val userID = sessionManager.getUserId().toString()
                  val title = noteState.value.title
                  val description = noteState.value.descripton

                  if(userID.isBlank() || title.isBlank() || description.isBlank())
                  {
                      if(userID.isBlank()){
                          _noteState.update { it.copy(errorMessage = "Error: Current user is not authorised to enter notes") }
                          return
                      }
                        _noteState.update { it.copy(errorMessage = "Error regarding entering notes") }
                        return
                  }
                val notes = Notes(
                    userId = userID,
                    title = title,
                    description = description
                )

              viewModelScope.launch {
                  try {
                      dao.insertNote(notes)
                      _noteState.update { it.copy(
                          userId = "",
                          title = "",
                          descripton = ""
                      ) }
                  }
                  catch (e: Exception){
                      e.printStackTrace()

                      _noteState.update { it.copy(
                          errorMessage = "Note feature failed due to: ${e.message}",
                          isSuccess = false
                      ) }
                  }
              }
          }
          is NotesEvent.setDecription -> {
              _noteState.update { it.copy(
                  descripton = event.decription,
                  errorMessage = null
              ) }
          }
          is NotesEvent.setTitle -> {
            _noteState.update { it.copy(
                title = event.title,
                errorMessage = null
            )}
          }
          is NotesEvent.setUserID -> {
              _noteState.update { it.copy(
                  userId = event.userId,
                  errorMessage = null
              ) }
          }
      }
    }




    companion object {
        fun provideFactory(dao: NotesDao, sessionManager: SessionManager): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NoteViewModel(dao,sessionManager) as T
            }
        }
    }
}