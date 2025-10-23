package vcmsa.projects.wilproject.firebase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.dao.UserDao
import vcmsa.projects.wilproject.models.User
import java.security.MessageDigest
import java.util.UUID

class LoginRepo(private val userDao: UserDao, private val firebaseConnect: FirebaseDB) {
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }

    private fun verifyPassword(providedPassword: String, storedHash: String): Boolean {
        val providedPasswordHash = hashPassword(providedPassword)
        return providedPasswordHash == storedHash
    }

    suspend fun attemptLocalLogin(usernameOrEmail: String, password: String): User? {
        var user = userDao.getUserByUsername(usernameOrEmail)
        if (user == null) {
            user = userDao.getUserByEmail(usernameOrEmail)
        }

        if (user == null) {
            try {
                val uid = firebaseConnect.signInWithFirebaseAuth(usernameOrEmail, password)

                if (uid != null) {
                    user = firebaseConnect.fetchUserById(uid)

                    if (user != null) {
                        userDao.upsertUser(user)
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginRepo", "Firebase Auth Sign-In failed: ${e.message}")
                return null
            }
        }

        if (user != null && verifyPassword(password, user.password)) {
            return user
        }
        return null
    }

    suspend fun handleGoogleSignIn(
        googleUserId: String,
        email: String,
        displayName: String
    ): User {
        var user = userDao.getUserById(googleUserId)

        if (user == null) {
            user = firebaseConnect.fetchUserById(googleUserId)

            if (user == null) {
                user = User(
                    userId = googleUserId,
                    firstName = displayName,
                    email = email,
                    password = hashPassword(UUID.randomUUID().toString())
                )

                userDao.upsertUser(user)

                CoroutineScope(Dispatchers.IO).launch{
                    try {
                        firebaseConnect.saveUserToFirebase(user)
                    } catch (e: Exception) {
                        Log.e("LoginRepo", "Failed to sync new Google user to Firebase: ${e.message}")
                    }
                }

            } else {
                userDao.upsertUser(user)
            }
        } else {
            userDao.upsertUser(user)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firebaseConnect.saveUserToFirebase(user)
                } catch (e: Exception) {
                    Log.e("LoginRepo", "Failed to sync existing user update to Firebase: ${e.message}")
                }
            }
        }

        return user
    }
}