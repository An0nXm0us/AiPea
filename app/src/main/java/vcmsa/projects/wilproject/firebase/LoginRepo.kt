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
    //hashes password
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }

   //checks provided password with hashed passwords
    private fun verifyPassword(providedPassword: String, storedHash: String): Boolean {
        val providedPasswordHash = hashPassword(providedPassword)
        return providedPasswordHash == storedHash
    }

    //finds user either by checking if there is a user stored locally or check for
    // the user on firebase
    suspend fun attemptLocalLogin(usernameOrEmail: String, password: String): User? {
        //  Check local DB email.
        var user = userDao.getUserByUsername(usernameOrEmail)
        if (user == null) {
            user = userDao.getUserByEmail(usernameOrEmail)
        }

        if (user == null) {
            // If not found locally, attempt to sign in via Firebase Auth.
            try {
                val uid = firebaseConnect.signInWithFirebaseAuth(usernameOrEmail, password)

                if (uid != null) {
                    // If Firebase Auth succeeds, fetch the full User object from the Firebase RDB.
                    user = firebaseConnect.fetchUserById(uid)

                    if (user != null) {
                        // 4. Cache the remote user data locally (Room).
                        userDao.upsertUser(user)
                    }
                }
            } catch (e: Exception) {
                // Log and return null on Firebase Auth failure.
                Log.e("LoginRepo", "Firebase Auth Sign-In failed: ${e.message}")
                return null
            }
        }

        // verify the password against the locally stored hash.
        if (user != null && verifyPassword(password, user.password)) {
            return user
        }
        return null
    }

    /**
     ** checks local DB, then Firebase , creates the user if necessary,
     * and ensures synchronization
     */
    suspend fun handleGoogleSignIn(googleUserId: String, email: String, displayName: String
    ): User {
        //  Try to find user locally by id.
        var user = userDao.getUserById(googleUserId)

        if (user == null) {
            // If not local, try to find user remotely in Firebase.
            user = firebaseConnect.fetchUserById(googleUserId)

            if (user == null) {
                //  Create a new User object
                user = User(
                    userId = googleUserId,
                    firstName = displayName,
                    email = email,
                    password = hashPassword(UUID.randomUUID().toString())
                )

                // Save to local Room.
                userDao.upsertUser(user)

                // Save to remote Firebase.
                CoroutineScope(Dispatchers.IO).launch{
                    try {
                        firebaseConnect.saveUserToFirebase(user)
                    } catch (e: Exception) {
                        Log.e("LoginRepo", "Failed to sync new Google user to Firebase: ${e.message}")
                    }
                }

            } else {
                // Firebase user found: Cache remote user data locally.
                userDao.upsertUser(user)
            }
        } else {
            // .Local user found
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
