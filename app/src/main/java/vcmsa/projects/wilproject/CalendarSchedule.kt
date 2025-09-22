package vcmsa.projects.wilproject

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID
import java.util.Date


@Entity(
tableName = "Events",
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
data class CalendarSchedule(
   val eventName: String,
    val eventType: String,
    val eventDescription: String,
    val eventDate: Date,
    @PrimaryKey(autoGenerate = true)
    val eventId: Int = 0,
   val userId : String,
)
