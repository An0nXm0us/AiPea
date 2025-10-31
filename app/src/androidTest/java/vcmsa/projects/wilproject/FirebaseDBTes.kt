package vcmsa.projects.wilproject

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import vcmsa.projects.wilproject.firebase.FirebaseDB
import vcmsa.projects.wilproject.models.Notes
import vcmsa.projects.wilproject.models.User
import java.lang.reflect.Field

@ExperimentalCoroutinesApi
class FirebaseDBTest {
    //Unit tests (SkyFish,2023)

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUsersRef: DatabaseReference
    private lateinit var mockNotesRef: DatabaseReference
    private lateinit var mockChildRef: DatabaseReference

    // --- Class Under Test ---
    private lateinit var firebaseDB: FirebaseDB

    @Before
    fun setup() {
        // Initialize all the mock objects
        mockAuth = mock()
        mockUsersRef = mock()
        mockNotesRef = mock()
        mockChildRef = mock()

        // Create instance of the class to test
        firebaseDB = FirebaseDB()

        // --- Inject all private Firebase val members ---
        injectMock(firebaseDB, "auth", mockAuth)
        injectMock(firebaseDB, "usersRef", mockUsersRef)
        injectMock(firebaseDB, "notesRef", mockNotesRef)
    }

    private fun injectMock(target: Any, fieldName: String, mock: Any) {
        try {
            val field: Field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(target, mock)
        } catch (e: NoSuchFieldException) {
            throw RuntimeException("Field '$fieldName' not found in ${target.javaClass.simpleName}. Update the test.", e)
        }
    }

    private fun <T> mockTask(result: T?, exception: Exception? = null): Task<T> {
        val task: Task<T> = mock()
        whenever(task.isComplete).thenReturn(true)
        whenever(task.isSuccessful).thenReturn(exception == null)
        whenever(task.result).thenReturn(result)
        whenever(task.exception).thenReturn(exception)

        return task
    }

    @Test
    fun signInWithFirebaseAuth_success_returnsUid() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val expectedUid = "test-uid-123"

        val mockAuthResult: AuthResult = mock()
        val mockFirebaseUser: FirebaseUser = mock()
        whenever(mockFirebaseUser.uid).thenReturn(expectedUid)
        whenever(mockAuthResult.user).thenReturn(mockFirebaseUser)
        val successTask: Task<AuthResult> = mockTask(result = mockAuthResult)
        whenever(mockAuth.signInWithEmailAndPassword(email, password)).thenReturn(successTask)

        // Act
        val actualUid = firebaseDB.signInWithFirebaseAuth(email, password)

        // Assert
        assert(expectedUid == actualUid)
    }

    @Test
    fun saveUserToFirebase_completesSuccessfully() = runTest {
        // Arrange
        val user = User("user-1", "test@example.com", "password-hash")
        val voidTask: Task<Void> = mockTask(result = null)
        whenever(mockUsersRef.child(user.userId)).thenReturn(mockChildRef)
        whenever(mockChildRef.setValue(user)).thenReturn(voidTask)

        // Act & Assert (verifies the call completes without exception)
        firebaseDB.saveUserToFirebase(user)
    }

    @Test
    fun fetchNotesForUser_success_returnsNotesList() = runTest {
        // Arrange
        val userId = "user-1"
        val note1 = Notes(noteId = "note-1", note_userId = userId, title = "First note", description = "this is first notes")
        val note2 = Notes(noteId = "note-2", note_userId = userId, title = "Second note", description = "this is second notes")
        val expectedNotes = listOf(note1, note2)

        val mockChildSnapshot1: DataSnapshot = mock()
        whenever(mockChildSnapshot1.getValue(Notes::class.java)).thenReturn(note1)

        val mockChildSnapshot2: DataSnapshot = mock()
        whenever(mockChildSnapshot2.getValue(Notes::class.java)).thenReturn(note2)

        val mockParentSnapshot: DataSnapshot = mock()
        whenever(mockParentSnapshot.children).thenReturn(listOf(mockChildSnapshot1, mockChildSnapshot2))

        val successTask: Task<DataSnapshot> = mockTask(result = mockParentSnapshot)
        whenever(mockNotesRef.child(userId)).thenReturn(mockChildRef)
        whenever(mockChildRef.get()).thenReturn(successTask)

        // Act
        val actualNotes = firebaseDB.fetchNotesForUser(userId)

        // Assert
        assert(expectedNotes.size == actualNotes.size)
        assert(actualNotes.containsAll(expectedNotes))
    }
}
