package vcmsa.projects.wilproject
import java.util.UUID
import android.provider.ContactsContract.CommonDataKinds.Email
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey
    val userId : String =UUID.randomUUID().toString(),
    val firstName :String,
    val password : String,
    val email: String
)
