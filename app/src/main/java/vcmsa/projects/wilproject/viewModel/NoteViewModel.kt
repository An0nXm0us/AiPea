package vcmsa.projects.wilproject.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.state.NoteState
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.event.NotesEvent
import vcmsa.projects.wilproject.SessionManager
import vcmsa.projects.wilproject.firebase.NotesRepo
import com.google.firebase.auth.FirebaseAuth

class NoteViewModel (private val repository: NotesRepo, private val sessionManager: SessionManager) : ViewModel() {
    private val _noteState = MutableStateFlow(NoteState())
    val noteState = _noteState.asStateFlow()

    init {
        val userId = sessionManager.getUserId()
        Log.d("USER_ID_CHECK", "User ID loaded in ViewModel: '$userId'")
        if (userId != null) {
            _noteState.update { it.copy(userId = userId) }
        }

        if (userId != null) {
            viewModelScope.launch {
                repository.getNotesByUserId(userId).collect { notesList ->
                    Log.d("NoteViewModel", "Notes collected: ${notesList.size}")
                    _noteState.update { it.copy(notes = notesList) }
                }
            }
        }
    }

    fun onEvent(event: NotesEvent)
    {
        when(event){
            is NotesEvent.deleteNotes -> {
                viewModelScope.launch {
                    repository.deleteNote(event.note)
                }
            }
            NotesEvent.createNote -> {
                val liveUserID = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val title = noteState.value.title
                val description = noteState.value.descripton

                if(liveUserID.isBlank() || title.isBlank() || description.isBlank())
                {
                    if(liveUserID.isBlank()){
                        _noteState.update { it.copy(errorMessage = "Error: Current user is not authorised to enter notes") }
                        return
                    }
                    _noteState.update { it.copy(errorMessage = "Error regarding entering notes") }
                    return
                }
                val notes = Notes(
                    note_userId = liveUserID,
                    title = title,
                    description = description
                )

                viewModelScope.launch {
                    try {
                        repository.saveNote(notes)
                        _noteState.update { it.copy(
                            userId = liveUserID,
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
        fun provideFactory(repository: NotesRepo, sessionManager: SessionManager): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NoteViewModel(repository,sessionManager) as T
            }
        }
    }
}