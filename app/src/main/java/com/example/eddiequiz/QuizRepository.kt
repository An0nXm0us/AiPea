package com.example.eddiequiz

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizRepository(
    private val scoreDao: ScoreDao,
    private val geminiApi: GeminiApi
) {
    suspend fun getQuiz(topic: String, level: Int): String = withContext(Dispatchers.IO) {
        val prompt = """
            Generate 5 multiple choice questions about $topic at difficulty level $level.
            Return as JSON array: [{"question":"", "options":["A","B","C","D"], "answerIndex":0}]
        """.trimIndent()

        val response = geminiApi.generateQuiz(GeminiRequest(prompt)).execute()
        return@withContext response.body()?.content ?: "[]"
    }

    suspend fun saveScore(score: ScoreEntity) = withContext(Dispatchers.IO) {
        scoreDao.insertScore(score)
    }

    suspend fun getAverage(topic: String) = withContext(Dispatchers.IO) {
        scoreDao.getAverageScore(topic) ?: 0.0
    }
}