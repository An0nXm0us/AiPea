package vcmsa.projects.wilproject.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import vcmsa.projects.wilproject.models.CalendarSchedule
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.models.User

class FirebaseDB {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val notesRef = database.getReference("notes")
    private val eventsRef = database.getReference("events")

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun signInWithFirebaseAuth(email: String, password: String): String? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e("FirebaseDB", "Firebase Auth Sign-In Failed: ${e.message}")
            throw e
        }
    }

    suspend fun saveUserToFirebase(user: User) {
        usersRef.child(user.userId)
            .setValue(user)
            .await()
    }
    suspend fun registerUserWithFirebaseAuth(email: String, rawPassword: String): String? {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, rawPassword).await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e("FirebaseDB", "Firebase Auth Registration Failed: ${e.message}")
            throw e
        }
    }
    suspend fun updateUserPasswordRemote(userId: String, newHashedPassword: String) {
        usersRef.child(userId)
            .child("password")
            .setValue(newHashedPassword)
            .await()
    }


    suspend fun fetchUserById(userId: String): User? {
        val snapshot = usersRef.child(userId)
            .get()
            .await()

        return snapshot.getValue(User::class.java)
    }

    suspend fun saveNoteToFirebase(note: Notes) {
        notesRef.child(note.note_userId).child(note.noteId).setValue(note).await()
    }

    suspend fun deleteNoteFromFirebase(note: Notes) {
        notesRef.child(note.note_userId).child(note.noteId).removeValue().await()
    }

    suspend fun fetchNotesForUser(userId: String): List<Notes> {
        val snapshot = notesRef.child(userId).get().await()
        val notesList = mutableListOf<Notes>()
        snapshot.children.forEach { noteSnapshot ->
            noteSnapshot.getValue(Notes::class.java)?.let { notesList.add(it) }
        }
        return notesList
    }

    suspend fun saveEventToFirebase(event: CalendarSchedule) {
        eventsRef.child(event.event_userId).child(event.eventId.toString()).setValue(event).await()
    }

    suspend fun deleteEventFromFirebase(event: CalendarSchedule) {
        eventsRef.child(event.event_userId).child(event.eventId.toString()).removeValue().await()
    }


    suspend fun fetchEventsForUser(userId: String): List<CalendarSchedule> {
        val snapshot = eventsRef.child(userId).get().await()
        val eventList = mutableListOf<CalendarSchedule>()
        snapshot.children.forEach { eventSnapshot ->
            eventSnapshot.getValue(CalendarSchedule::class.java)?.let { eventList.add(it) }
        }
        return eventList
    }

}