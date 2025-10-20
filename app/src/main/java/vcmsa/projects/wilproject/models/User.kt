package vcmsa.projects.wilproject.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class User(
    @PrimaryKey
    val userId : String = UUID.randomUUID().toString(),
    val firstName :String,
    val password : String,
    val email: String
)