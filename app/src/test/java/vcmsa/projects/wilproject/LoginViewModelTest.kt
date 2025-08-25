package vcmsa.projects.wilproject

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var viewModel: LoginViewModel
    private lateinit var dao: UserDao

    @Before
    fun setUp() {
        dao = mock(UserDao::class.java)
        viewModel = LoginViewModel(dao)
    }

    @Test
    fun `empty username shows error`() = runTest {
        viewModel.onEvent(LoginEvent.checkUsername(""))
        viewModel.onEvent(LoginEvent.checkPassword("password"))
        viewModel.onEvent(LoginEvent.Login)

        val state = viewModel.loginState.value
        assertEquals("Username is required", state.errorMessage)
    }

    @Test
    fun `empty password shows error`() = runTest {
        viewModel.onEvent(LoginEvent.checkUsername("testUser"))
        viewModel.onEvent(LoginEvent.checkPassword(""))
        viewModel.onEvent(LoginEvent.Login)

        val state = viewModel.loginState.value
        assertEquals("Password is required", state.errorMessage)
    }

    @Test
    fun `user not found updates errorMessage`() = runTest {
        `when`(dao.getUserByUsername("testUser")).thenReturn(null)
        `when`(dao.getUserByEmail("testUser")).thenReturn(null)

        viewModel.onEvent(LoginEvent.checkUsername("testUser"))
        viewModel.onEvent(LoginEvent.checkPassword("password"))
        viewModel.onEvent(LoginEvent.Login)

        val state = viewModel.loginState.value
        assertEquals("User not found", state.errorMessage)
    }

    @Test
    fun `invalid password shows error`() = runTest {
        val user = User(
            email = "test@example.com",
            password = "correctPass",
            userId = TODO(),
            firstName = TODO()
        )
        `when`(dao.getUserByUsername("testUser")).thenReturn(user)

        viewModel.onEvent(LoginEvent.checkUsername("testUser"))
        viewModel.onEvent(LoginEvent.checkPassword("wrongPass"))
        viewModel.onEvent(LoginEvent.Login)

        val state = viewModel.loginState.value
        assertEquals("Invalid password", state.errorMessage)
    }

    @Test
    fun `valid login updates isSuccess`() = runTest {
        val user = User(
            email = "test@example.com",
            password = "correctPass",
            userId = TODO(),
            firstName = TODO()
        )
        `when`(dao.getUserByUsername("testUser")).thenReturn(user)

        viewModel.onEvent(LoginEvent.checkUsername("testUser"))
        viewModel.onEvent(LoginEvent.checkPassword("correctPass"))
        viewModel.onEvent(LoginEvent.Login)

        val state = viewModel.loginState.value
        assertTrue(state.isSuccess)
    }
}
