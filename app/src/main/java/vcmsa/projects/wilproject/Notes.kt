package vcmsa.projects.wilproject

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "Notes",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = arrayOf("userId"),
            childColumns = arrayOf("userId"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Notes(
    val userId : String,
    @PrimaryKey
    val noteId :String = UUID.randomUUID().toString(),
    val title: String,
    val description:String
)




