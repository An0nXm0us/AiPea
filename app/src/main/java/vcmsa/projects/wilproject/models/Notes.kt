package vcmsa.projects.wilproject.models

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
            childColumns = arrayOf("note_userId"),
            onUpdate = ForeignKey.Companion.CASCADE,
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)
data class Notes @JvmOverloads constructor(
    val note_userId : String ="",
    @PrimaryKey
    val noteId :String = UUID.randomUUID().toString(),
    val title: String = "",
    val description:String =""

)
///