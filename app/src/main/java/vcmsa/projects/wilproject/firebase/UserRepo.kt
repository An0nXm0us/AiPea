/**
 * Repository class responsible for managing user-specific profile data and registration.
 *
 * This class coordinates actions related to creating new users and updating user properties
 * (like passwords), ensuring data consistency between the local Room database and the
 * remote Firebase Realtime Database and Authentication services.
 */
package vcmsa.projects.wilproject.firebase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.dao.UserDao
import vcmsa.projects.wilproject.models.User
import java.security.MessageDigest // Added import for hashing

class UserRepo(private val userDao: UserDao, private val firebaseConnect: FirebaseDB) {

    /**
     * Hashes a raw password using SHA-512 for secure storage.
     */
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }

    //Registers a completely new user
    suspend fun registerNewUser(email: String, rawPassword: String, firstName: String): User {

        //  Create user in Firebase Authentication and get the unique ID.
        val uid = firebaseConnect.registerUserWithFirebaseAuth(email, rawPassword)
            ?: throw Exception("Firebase Auth registration failed: No UID returned.")

        // Hash the password
        val hashedPass = hashPassword(rawPassword)

        //Create the local User object
        val newUser = User(
            userId = uid,
            firstName = firstName,
            email = email,
            password = hashedPass
        )

        // Save to local Room DB
        userDao.upsertUser(newUser)

        // Save to Firebase
        firebaseConnect.saveUserToFirebase(newUser)

        return newUser
    }


    /**
     * Resets a user's password. Updates both local and cloud  storage.
     */
    suspend fun resetUserPassword(email: String, newHashedPassword: String): Boolean {
        // Find the user locally by email.
        val user = userDao.getUserByEmail(email)

        return user?.let {
            // Create a copy of the user with the new hashed password.
            val updatedUser = it.copy(password = newHashedPassword)

            // Update Room DB.
            userDao.updateUser(updatedUser)

            // Update Firebase
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firebaseConnect.updateUserPasswordRemote(it.userId, newHashedPassword)
                } catch (e: Exception) {
                    Log.e("UserRepository", "Failed to sync password reset to Firebase.")
                }
            }
            true // User found and update initiated.
        } ?: false // User not found.
    }
}
