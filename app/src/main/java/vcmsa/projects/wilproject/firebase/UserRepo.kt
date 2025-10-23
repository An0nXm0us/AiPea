package vcmsa.projects.wilproject.firebase

import android.util.Log
import com.google.firebase.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.firebase.FirebaseDB
import vcmsa.projects.wilproject.dao.UserDao
import vcmsa.projects.wilproject.models.User
import java.security.MessageDigest // Added import for hashing

class UserRepo(private val userDao: UserDao, private val firebaseConnect: FirebaseDB) {


    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }


    suspend fun registerNewUser(email: String, rawPassword: String, firstName: String): User {


        val uid = firebaseConnect.registerUserWithFirebaseAuth(email, rawPassword)
            ?: throw Exception("Firebase Auth registration failed: No UID returned.")

        val hashedPass = hashPassword(rawPassword)
        val newUser = User(
            userId = uid,
            firstName = firstName,
            email = email,
            password = hashedPass
        )


        userDao.upsertUser(newUser)
        firebaseConnect.saveUserToFirebase(newUser)

        return newUser
    }




    suspend fun resetUserPassword(email: String, newHashedPassword: String): Boolean {
        val user = userDao.getUserByEmail(email)

        return user?.let {
            val updatedUser = it.copy(password = newHashedPassword)
            userDao.updateUser(updatedUser)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firebaseConnect.updateUserPasswordRemote(it.userId, newHashedPassword)
                } catch (e: Exception) {
                    Log.e("UserRepository", "Failed to sync password reset to Firebase.")
                }
            }
            true
        } ?: false
    }



}