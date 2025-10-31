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

/**
 * This view model will be using both its repective dao and repo methods to save locally and on firebase
 * (Tadas Petra.2024 & Philipp Lackner,2023)
 * The state wil be used and reflected on ui (Philipp Lackner,2022)
 **/
class NoteViewModel (private val repository: NotesRepo, private val sessionManager: SessionManager) : ViewModel() {
    private val _noteState = MutableStateFlow(NoteState())
    val noteState = _noteState.asStateFlow()

    init {
        // Load the stored user ID from the session manager.
        val userId = sessionManager.getUserId()
        Log.d("USER_ID_CHECK", "User ID loaded in ViewModel: '$userId'")

        // Update the state with the user id.
        if (userId != null) {
            _noteState.update { it.copy(userId = userId) }
        }
        if (userId != null) {
            viewModelScope.launch {
                // Collect notes by user ID and continuously update the _noteState
                repository.getNotesByUserId(userId).collect { notesList ->
                    Log.d("NoteViewModel", "Notes collected: ${notesList.size}")
                    _noteState.update { it.copy(notes = notesList) }
                }
            }
        }
    }

    //Events that will handle user interaction
    fun onEvent(event: NotesEvent)
    {
        when(event){
            // Handles the deletion of a specific note
            is NotesEvent.deleteNotes -> {
                viewModelScope.launch {
                    repository.deleteNote(event.note)
                }
            }
            // Handles the creation and saving of a new note
            NotesEvent.createNote -> {
                // Get the current authenticated user directly from Firebase
                val liveUserID = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val title = noteState.value.title
                val description = noteState.value.descripton

                // Input validation checks.
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

                // Save the note and reset the input fields upon success.
                viewModelScope.launch {
                    try {
                        repository.saveNote(notes)
                        _noteState.update { it.copy(
                            userId = liveUserID,
                            title = "",
                            descripton = "",
                            errorMessage = null,
                            isSuccess = true
                        ) }
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        // Update state with error message on failure.
                        _noteState.update { it.copy(
                            errorMessage = "Note feature failed due to: ${e.message}",
                            isSuccess = false
                        ) }
                    }
                }
            }
            // Updates the description field in the state.
            is NotesEvent.setDecription -> {
                _noteState.update { it.copy(
                    descripton = event.decription,
                    errorMessage = null
                ) }
            }
            // Updates the title field in the state.
            is NotesEvent.setTitle -> {
                _noteState.update { it.copy(
                    title = event.title,
                    errorMessage = null
                )}
            }
            // Updates the user ID field in the state.
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
