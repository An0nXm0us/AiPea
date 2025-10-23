package com.example.eddiequiz

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val difficulty: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val percentage: Double,
    val timestamp: Long = System.currentTimeMillis()
)
