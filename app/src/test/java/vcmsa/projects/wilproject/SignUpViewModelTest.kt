package vcmsa.projects.wilproject

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    private lateinit var viewModel: UserViewModel
    private lateinit var dao: UserDao

    @Before
    fun setUp() {
        dao = mock(UserDao::class.java)
        viewModel = UserViewModel(dao)
    }

    @Test
    fun `empty full name shows error`() = runTest {
        viewModel.onEvent(UserEvent.setFirstName(""))
        viewModel.onEvent(UserEvent.setEmail("test@example.com"))
        viewModel.onEvent(UserEvent.setPassword("password"))
        viewModel.onEvent(UserEvent.setConfirmPassword("password"))
        viewModel.onEvent(UserEvent.createUser)

        val state = viewModel.userState.value
        assertEquals("Full name is required", state.errorMessage)
    }

    @Test
    fun `invalid email shows error`() = runTest {
        viewModel.onEvent(UserEvent.setFirstName("Test User"))
        viewModel.onEvent(UserEvent.setEmail("invalid-email"))
        viewModel.onEvent(UserEvent.setPassword("password"))
        viewModel.onEvent(UserEvent.setConfirmPassword("password"))
        viewModel.onEvent(UserEvent.createUser)

        val state = viewModel.userState.value
        assertEquals("Invalid email format", state.errorMessage)
    }

    @Test
    fun `password and confirm password mismatch shows error`() = runTest {
        viewModel.onEvent(UserEvent.setFirstName("Test User"))
        viewModel.onEvent(UserEvent.setEmail("test@example.com"))
        viewModel.onEvent(UserEvent.setPassword("password"))
        viewModel.onEvent(UserEvent.setConfirmPassword("wrongPassword"))
        viewModel.onEvent(UserEvent.createUser)

        val state = viewModel.userState.value
        assertEquals("Passwords do not match", state.errorMessage)
    }

    @Test
    fun `existing email shows error`() = runTest {
        // Mock DAO to return an existing user
        val existingUser = User(firstName= "testName", email = "test@example.com", password = "1234")
        `when`(dao.getUserByEmail("test@example.com")).thenReturn(existingUser)

        viewModel.onEvent(UserEvent.setFirstName("Test User"))
        viewModel.onEvent(UserEvent.setEmail("test@example.com"))
        viewModel.onEvent(UserEvent.setPassword("password"))
        viewModel.onEvent(UserEvent.setConfirmPassword("password"))
        viewModel.onEvent(UserEvent.createUser)

        val state = viewModel.userState.value
        assertEquals("Email already exists", state.errorMessage)
    }

    @Test
    fun `successful signup updates isSuccess`() = runTest {
        // Mock DAO to return null (email not used)
        `when`(dao.getUserByEmail("newuser@example.com")).thenReturn(null)

        viewModel.onEvent(UserEvent.setFirstName("New User"))
        viewModel.onEvent(UserEvent.setEmail("newuser@example.com"))
        viewModel.onEvent(UserEvent.setPassword("password"))
        viewModel.onEvent(UserEvent.setConfirmPassword("password"))
        viewModel.onEvent(UserEvent.createUser)

        val state = viewModel.userState.value
        assertTrue(state.isSuccess)

        // Verify that DAO's insertUser was called
        verify(dao, times(1)).upsertUser(any(User::class.java))
    }
}
