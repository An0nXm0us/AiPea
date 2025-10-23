package com.example.eddiequiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class QuizViewModel(private val repo: QuizRepository) : ViewModel() {

    fun loadQuiz(topic: String, level: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val quizJson = repo.getQuiz(topic, level)
            onResult(quizJson)
        }
    }

    fun recordScore(topic: String, level: Int, correct: Int, total: Int) {
        val percent = (correct.toDouble() / total) * 100
        viewModelScope.launch {
            repo.saveScore(ScoreEntity(topic = topic, difficulty = level, correctAnswers = correct, totalQuestions = total, percentage = percent))
        }
    }

    suspend fun getAverage(topic: String) = repo.getAverage(topic)

    class QuizViewModelFactory(
        private val repository: QuizRepository
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return QuizViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

//    class QuizViewModelFactory(private val scoreDao: ScoreDao) : ViewModelProvider.Factory {
//        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
//                @Suppress("UNCHECKED_CAST")
//                return QuizViewModel(QuizRepository) as T
//            }
//            throw IllegalArgumentException("Unknown ViewModel class")
//        }
//    }
}