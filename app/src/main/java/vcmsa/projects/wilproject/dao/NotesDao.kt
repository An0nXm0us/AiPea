package vcmsa.projects.wilproject.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.models.User

@Dao
interface NotesDao {
//vararg= 0 or more arguments
    @Upsert
    suspend fun insertNote(vararg notes: Notes)

    @Delete
    suspend fun deleteNote(note: Notes)

    // Fetch user ID based on note ID
    @Query("SELECT note_userId FROM notes WHERE noteId = :noteId")
    suspend fun getUserIdByNoteId(noteId: String): String?

    // Get notes of a user
    @Query("SELECT * FROM notes WHERE note_userId = :userId")
    fun getNotesByUserId(userId: String): Flow<List<Notes>>

    // To get user details along with their notes
    @Query("SELECT * FROM User WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM notes WHERE noteId = :noteId")
    fun getNoteById(noteId: String): Flow<List<Notes>>
}