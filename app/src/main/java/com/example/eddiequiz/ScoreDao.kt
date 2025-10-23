package com.example.eddiequiz

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: ScoreEntity)

    @Query("SELECT * FROM scores WHERE topic = :topic ORDER BY timestamp DESC LIMIT 5")
    suspend fun getRecentScores(topic: String): List<ScoreEntity>

    @Query("SELECT AVG(percentage) FROM scores WHERE topic = :topic")
    suspend fun getAverageScore(topic: String): Double?
}