package vcmsa.projects.wilproject.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "quizScore",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = arrayOf("userId"),
            childColumns = arrayOf("score_userId"),
            onUpdate = ForeignKey.Companion.CASCADE,
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)
data class ScoreEntity(
    val score_userId : String ="",
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String = "",
    val difficulty: Int = 0,
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val percentage: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)
