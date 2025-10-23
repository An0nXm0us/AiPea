package vcmsa.projects.wilproject.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import vcmsa.projects.wilproject.models.User

@Dao
interface UserDao {

    @Upsert
    suspend fun upsertUser(user:User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM User WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM User WHERE firstName = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM User WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?



    @Update
    suspend fun updateUser(user: User)



}