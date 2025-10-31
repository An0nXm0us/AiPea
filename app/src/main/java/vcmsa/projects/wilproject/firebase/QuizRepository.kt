package vcmsa.projects.wilproject.firebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import vcmsa.projects.wilproject.api.GeminiApi
import vcmsa.projects.wilproject.api.GeminiRequest
import vcmsa.projects.wilproject.api.MessageContent
import vcmsa.projects.wilproject.api.RawText
import vcmsa.projects.wilproject.models.ScoreEntity

class QuizRepository(private val firebaseDB: FirebaseDB, private val geminiApi: GeminiApi) {
//generates quiz
    suspend fun getQuiz(topic: String, level: Int): String = withContext(Dispatchers.IO) {
        val prompt = """
            Generate 5 multiple choice questions about $topic at difficulty level $level.
            Return as JSON array: [{"question":"", "options":["A","B","C","D"], "answerIndex":0}]
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(
                MessageContent(
                    parts = listOf(
                        RawText(text = prompt)
                    )
                )
            )
        )

        try {
            val call = geminiApi.generateQuiz(requestBody)
            val response = call.execute()

            if (response.isSuccessful) {
                val geminiResponse = response.body()

                val rawQuizJson = geminiResponse?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                return@withContext rawQuizJson ?: "[]"
            } else {
                Log.e("QuizRepository", "API call failed with code: ${response.code()}. Error Body: ${response.errorBody()?.string()}")
                return@withContext "[]"
            }
        } catch (e: Exception) {
            Log.e("QuizRepository", "API call failed: ${e.message}")
            return@withContext "[]"
        }
    }
//saves quiz score and content
    suspend fun saveScore(score: ScoreEntity) = withContext(Dispatchers.IO) {
        try {
            firebaseDB.saveScoreToFirebase(score)
        } catch (e: Exception) {
            Log.e("QuizRepository", "Failed to save score to Firebase: ${e.message}")
        }
    }

    suspend fun getAverage(topic: String, userId: String): Double = withContext(Dispatchers.IO) {
        return@withContext try {
            val scores = firebaseDB.fetchScoresForUserAndTopic(userId, topic)
            if (scores.isNotEmpty()) {
                scores.mapNotNull { it.percentage }
                    .average()
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.e("QuizRepository", "Failed to fetch average from Firebase: ${e.message}")
            0.0
        }
    }

    /**
     * Fetches all recorded quiz scores for a specific user from Firebase.
     */
    suspend fun getAllScores(userId: String): List<ScoreEntity> = withContext(Dispatchers.IO) {
        return@withContext try {

            firebaseDB.fetchAllScoresForUser(userId)
        } catch (e: Exception) {
            Log.e("QuizRepository", "Failed to fetch all scores from Firebase: ${e.message}")
            emptyList()
        }
    }
}
