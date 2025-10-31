package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import vcmsa.projects.wilproject.databinding.ActivityResultBinding
import vcmsa.projects.wilproject.api.GeminiApi
import vcmsa.projects.wilproject.api.RetrofitClientQuiz
import vcmsa.projects.wilproject.firebase.FirebaseDB
import vcmsa.projects.wilproject.firebase.QuizRepository
import vcmsa.projects.wilproject.viewModel.QuizViewModel

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var viewModel: QuizViewModel
    private lateinit var geminiApi: GeminiApi
    private lateinit var firebaseDB: FirebaseDB
    private lateinit var sessionManager: SessionManager
//shows results (Android Developers, (n.d.) & (Islam,2025))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val topic = intent.getStringExtra("topic") ?: ""
        val difficulty = intent.getIntExtra("difficulty", 1)
        val score = intent.getIntExtra("score", 0)
        val total = intent.getIntExtra("total", 0)
        val incorrect = total - score // Calculate incorrect answers


        // Display the score
        binding.tvResult.text = "$score / $total"
        // Display the number of correct answers
        binding.tvCorrectCount.text = score.toString()
        // Display the number of incorrect answers
        binding.tvIncorrectCount.text = incorrect.toString()

        // Calculate the performance percentage
        val percentage = if (total > 0) (score.toFloat() / total) * 100 else 0f
        // Display performance message
        binding.tvPerformance.text = getPerformanceMessage(percentage)

        // Initialize Firebase connection
        firebaseDB = FirebaseDB()

       //button to take user to the quiz home where they can do another quiz
        binding.btnTakeAnotherQuiz.setOnClickListener {
            val intent = Intent(this, QuizHome::class.java)
            startActivity(intent)
            finish()
        }

        sessionManager = SessionManager(this)
        geminiApi = RetrofitClientQuiz.createService(GeminiApi::class.java)
        val repository = QuizRepository(firebaseDB, geminiApi)
        val factory = QuizViewModel.QuizViewModelFactory(repository, sessionManager)
        viewModel = ViewModelProvider(this,factory)[QuizViewModel::class.java]

        // Call the ViewModel to save the quiz result to the database
        viewModel.recordScore(topic, difficulty, score, total)
    }

    //Methods that displays a message backed on performance
    private fun getPerformanceMessage(percentage: Float): String {
        return when {
            percentage >= 90 -> "Excellent work! You mastered this topic."
            percentage >= 70 -> "Great job! You have a solid understanding."
            percentage >= 50 -> "Good effort! A little more study and you'll ace it."
            else -> "Keep practicing! You can do better next time."
        }
    }
}