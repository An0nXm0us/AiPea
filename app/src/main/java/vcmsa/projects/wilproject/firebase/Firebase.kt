
package vcmsa.projects.wilproject.firebase
//Class handles all firebase functionality and database interactions (Tadas Petra, 2024)
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import vcmsa.projects.wilproject.models.CalendarSchedule
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.models.ScoreEntity
import vcmsa.projects.wilproject.models.User
import com.google.firebase.database.DatabaseReference

class FirebaseDB {

    // Initializes the main Firebase Database instance Tadas Petra.2024.
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val notesRef = database.getReference("notes")
    private val eventsRef = database.getReference("events")
    private val scoresRef = database.getReference("quizScores")

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    //sign in with firebase auth
    suspend fun signInWithFirebaseAuth(email: String, password: String): String? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e("FirebaseDB", "Firebase Auth Sign-In Failed: ${e.message}")
            throw e
        }
    }

    //saves or updates a User object in the Realtime Database using the userId as the key.

    suspend fun saveUserToFirebase(user: User) {
        usersRef.child(user.userId)
            .setValue(user)
            .await()
    }

    //Creates a new user account using Firebase Authentication.

    suspend fun registerUserWithFirebaseAuth(email: String, rawPassword: String): String? {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, rawPassword).await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e("FirebaseDB", "Firebase Auth Registration Failed: ${e.message}")
            throw e
        }
    }

    //updates passwords
    suspend fun updateUserPasswordRemote(userId: String, newHashedPassword: String) {
        usersRef.child(userId)
            .child("password")
            .setValue(newHashedPassword)
            .await()
    }


    //Fetches a User object from the database using their ID.
    suspend fun fetchUserById(userId: String): User? {
        val snapshot = usersRef.child(userId)
            .get()
            .await()

        return snapshot.getValue(User::class.java)
    }

    /**
     * Saves or updates a Notes object under the user's dedicated notes node.
     */
    suspend fun saveNoteToFirebase(note: Notes) {
        notesRef.child(note.note_userId).child(note.noteId).setValue(note).await()
    }

    /**
     * Deletes a specific Note object from the database.
     */
    suspend fun deleteNoteFromFirebase(note: Notes) {
        notesRef.child(note.note_userId).child(note.noteId).removeValue().await()
    }

    /**
     * Fetches all Notes associated with a given user ID.

     */
    suspend fun fetchNotesForUser(userId: String): List<Notes> {
        val snapshot = notesRef.child(userId).get().await()
        val notesList = mutableListOf<Notes>()
        snapshot.children.forEach { noteSnapshot ->
            noteSnapshot.getValue(Notes::class.java)?.let { notesList.add(it) }
        }
        return notesList
    }

    /**
     * Saves or updates a CalendarSchedule event. Uses push() for new events (eventId null).
     */
    suspend fun saveEventToFirebase(event: CalendarSchedule) {
        val eventNode: DatabaseReference = eventsRef.child(event.event_userId)

        if (event.eventId == null) {
            // New event: Use push() to generate a unique Firebase ID
            eventNode.push().setValue(event).await()
        } else {
            // Existing event: Use the local eventId for reference (Still risky, but consistent with current structure)
            eventNode.child(event.eventId.toString()).setValue(event).await()
        }
    }

    /**
     * Deletes a specific CalendarSchedule event from the database.
     */
    suspend fun deleteEventFromFirebase(event: CalendarSchedule) {
        eventsRef.child(event.event_userId).child(event.eventId.toString()).removeValue().await()
    }


    /**
     * Fetches all CalendarSchedule events for a specific user ID.

     */
    suspend fun fetchEventsForUser(userId: String): List<CalendarSchedule> {
        val snapshot = eventsRef.child(userId).get().await()
        val eventList = mutableListOf<CalendarSchedule>()
        snapshot.children.forEach { eventSnapshot ->
            eventSnapshot.getValue(CalendarSchedule::class.java)?.let { eventList.add(it) }
        }
        return eventList
    }

    /**
     * Saves a new quiz score .
     */
    suspend fun saveScoreToFirebase(score: ScoreEntity) {
        scoresRef.child(score.score_userId)
            .push()
            .setValue(score)
            .await()
    }

    /**
     * Fetches all quiz scores for a user, filtered by a specific topic.
     */
    suspend fun fetchScoresForUserAndTopic(userId: String, topic: String): List<ScoreEntity> {
        val snapshot = scoresRef.child(userId)
            .orderByChild("topic")
            .equalTo(topic)
            .get()
            .await()

        val scoreList = mutableListOf<ScoreEntity>()
        snapshot.children.forEach { scoreSnapshot ->
            scoreSnapshot.getValue(ScoreEntity::class.java)?.let { scoreList.add(it) }
        }
        return scoreList
    }

    /**
     * Fetches all recorded quiz scores for a specific user, regardless of topic.
     */
    suspend fun fetchAllScoresForUser(userId: String): List<ScoreEntity> {
        val snapshot = scoresRef.child(userId)
            .get()
            .await()

        val scoreList = mutableListOf<ScoreEntity>()
        snapshot.children.forEach { scoreSnapshot ->
            scoreSnapshot.getValue(ScoreEntity::class.java)?.let { scoreList.add(it) }
        }
        return scoreList
    }
}
