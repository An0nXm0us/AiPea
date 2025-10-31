package vcmsa.projects.wilproject.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import vcmsa.projects.wilproject.models.ScoreEntity

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: ScoreEntity)

    @Query("SELECT * FROM quizScore WHERE topic = :topic ORDER BY timestamp DESC LIMIT 5")
    suspend fun getRecentScores(topic: String): List<ScoreEntity>

    @Query("SELECT AVG(percentage) FROM quizScore WHERE topic = :topic")
    suspend fun getAverageScore(topic: String): Double?
}