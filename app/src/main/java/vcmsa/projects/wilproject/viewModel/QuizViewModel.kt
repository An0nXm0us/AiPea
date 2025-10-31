package vcmsa.projects.wilproject.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.SessionManager
import vcmsa.projects.wilproject.firebase.QuizRepository
import vcmsa.projects.wilproject.models.ScoreEntity

/**
 * This view model will be using both its repective dao and repo methods to save locally and on firebase
 * (Tadas Petra.2024 & Philipp Lackner,2023)
 * The state wil be used and reflected on ui (Philipp Lackner,2022)
 **/
class QuizViewModel(private val repo: QuizRepository,private val sessionManager: SessionManager) : ViewModel() {
//loads quiz's
    fun loadQuiz(topic: String, level: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val quizJson = repo.getQuiz(topic, level)
            onResult(quizJson)
        }
    }

    fun recordScore(topic: String, level: Int, correct: Int, total: Int) {
        val percent = (correct.toDouble() / total) * 100
        val userId = sessionManager.getUserId()
        viewModelScope.launch {
            repo.saveScore(
                ScoreEntity(
                    score_userId = userId.toString(),
                    topic = topic,
                    difficulty = level,
                    correctAnswers = correct,
                    totalQuestions = total,
                    percentage = percent
                )
            )
        }
    }


    suspend fun getOverallAverage(): Double {
        val userId = sessionManager.getUserId().toString()
        val allScores = repo.getAllScores(userId)
        return if (allScores.isNotEmpty()) {
            allScores.mapNotNull { it.percentage }.average()
        } else {
            0.0
        }
    }

    suspend fun getAllScoresForUser(): List<ScoreEntity> {
        val userId = sessionManager.getUserId().toString()
        return repo.getAllScores(userId)
    }

    class QuizViewModelFactory(private val repository: QuizRepository, private val sessionManager: SessionManager) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return QuizViewModel(repository,sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}