package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.api.GeminiApi
import vcmsa.projects.wilproject.api.RetrofitClientQuiz
import vcmsa.projects.wilproject.databinding.ActivityQuizHomeBinding
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.firebase.QuizRepository
import vcmsa.projects.wilproject.adapter.ScoresAdapter
import vcmsa.projects.wilproject.firebase.FirebaseDB
import vcmsa.projects.wilproject.viewModel.QuizViewModel
//QuizHome Activity shows the user's quiz history and overall average (Android Developers, (n.d.))
class QuizHome : AppCompatActivity() {
    private lateinit var binding: ActivityQuizHomeBinding
    private lateinit var viewModel: QuizViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var scoreAdapter: ScoresAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        val database = EddieDatabase.getDatabase(applicationContext)
        val scoreDao = database.scoreDao()
        val firebaseDB = FirebaseDB()
        val geminiApi = RetrofitClientQuiz.createService(GeminiApi::class.java)

        val repository = QuizRepository(firebaseDB, geminiApi)

        val factory = QuizViewModel.QuizViewModelFactory(repository, sessionManager)
        viewModel = ViewModelProvider(this, factory)[QuizViewModel::class.java]

        scoreAdapter = ScoresAdapter()
        binding.rvScores.apply {
            layoutManager = LinearLayoutManager(this@QuizHome)
            adapter = scoreAdapter
        }

        binding.btnStartQuiz.setOnClickListener {
            startQuiz()
        }
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, HomePage::class.java))
        }
        loadOverallAverage()
        loadQuizHistory()
    }

    private fun startQuiz() {
        val topic = binding.etTopic.text.toString().trim()

        if (topic.isEmpty()) {
            Toast.makeText(this, "Please enter a quiz topic.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDifficultyId = binding.rgDifficulty.checkedRadioButtonId

        // Set the radio buttons to number difficulty level (1 to 4)
        val difficulty = when (selectedDifficultyId) {
            binding.rbDifficulty1.id -> 1
            binding.rbDifficulty2.id -> 2
            binding.rbDifficulty3.id -> 3
            binding.rbDifficulty4.id -> 4
            else -> 1
        }

        // Start the QuizActivity passing both the topic and the selected difficulty
        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("topic", topic)
            putExtra("difficulty", difficulty)
        }
        startActivity(intent)
    }

    //Loads and displays user' average
    private fun loadOverallAverage() {
        lifecycleScope.launch {
             val average = viewModel.getOverallAverage()
          binding.tvOverallAverage.text = "Overall Average: ${String.format("%.2f", average)}%"
        }
    }

    //Loads and displays the user's quiz history scores in the RecyclerView
    private fun loadQuizHistory() {
        lifecycleScope.launch {
            val scores = viewModel.getAllScoresForUser()
            Log.d("QuizHome", "Scores retrieved: ${scores.size}")
            scoreAdapter.submitList(scores.sortedByDescending { it.timestamp })
        }
    }

    //Refreshes the score history and overall average when the activity resume after completing a quiz

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            loadOverallAverage()
            loadQuizHistory()
        }
    }
}
