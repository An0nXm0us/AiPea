
package vcmsa.projects.wilproject.firebase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.firebase.FirebaseDB
import vcmsa.projects.wilproject.dao.NotesDao
import vcmsa.projects.wilproject.models.Notes

class NotesRepo(
    private val notesDao: NotesDao,
    private val firebaseConnect: FirebaseDB
) {
    suspend fun saveNote(note: Notes) {
        notesDao.insertNote(note)
        try {
            firebaseConnect.saveNoteToFirebase(note)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Failed to sync note to Firebase: ${e.message}")
        }
    }


    suspend fun deleteNote(note: Notes) {
        notesDao.deleteNote(note)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firebaseConnect.deleteNoteFromFirebase(note)
            } catch (e: Exception) {
                Log.e("NotesRepository", "Failed to delete note from Firebase: ${e.message}")
            }
        }
    }

    fun getNotesByUserId(userId: String): Flow<List<Notes>> {
        val localFlow = notesDao.getNotesByUserId(userId)

        localFlow.onEach { localNotes ->
            if (localNotes.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val remoteNotes = firebaseConnect.fetchNotesForUser(userId)
                        notesDao.insertNote(*remoteNotes.toTypedArray())
                    } catch (e: Exception) {
                        Log.e("NotesRepository", "Failed to sync initial notes from Firebase: ${e.message}")
                    }
                }
            }
        }.launchIn(CoroutineScope(Dispatchers.IO))

        return localFlow
    }
}