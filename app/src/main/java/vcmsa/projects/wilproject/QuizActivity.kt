package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import vcmsa.projects.wilproject.databinding.ActivityQuizBinding
import org.json.JSONArray
import org.json.JSONException
import vcmsa.projects.wilproject.api.GeminiApi
import vcmsa.projects.wilproject.api.RetrofitClientQuiz
import vcmsa.projects.wilproject.db.EddieDatabase
import vcmsa.projects.wilproject.firebase.FirebaseDB
import vcmsa.projects.wilproject.firebase.QuizRepository
import vcmsa.projects.wilproject.viewModel.QuizViewModel

//Handles giving out  quiz's (Android Developers, (n.d.) & (Islam,2025))

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private lateinit var geminiApi: GeminiApi
    private lateinit var viewModel: QuizViewModel
    private lateinit var firebaseDB: FirebaseDB
    private var topic: String = ""
    private var difficulty = 1
    private var currentQuestion = 0
    private var score = 0
    private var questions = JSONArray()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        topic = intent.getStringExtra("topic") ?: ""
        difficulty = intent.getIntExtra("difficulty", 1)

        sessionManager = SessionManager(this)
        val database = EddieDatabase.getDatabase(applicationContext)
        val scoreDao = database.scoreDao()
        geminiApi = RetrofitClientQuiz.createService(GeminiApi::class.java)
        firebaseDB = FirebaseDB()

        val repository = QuizRepository(firebaseDB , geminiApi)
        val factory = QuizViewModel.QuizViewModelFactory(repository, sessionManager)
        viewModel = ViewModelProvider(this, factory)[QuizViewModel::class.java]

        // Set the title display on the UI
        binding.tvTopicTitle.text = "Quiz for: $topic (Level $difficulty)"

        // Setup custom listeners for option CardViews
        setupOptionCardListeners()

        // Initiate the process of fetching quiz data
        loadQuizData()

        binding.btnNext.setOnClickListener { checkAnswerAndNext() }
        // Disables the next  until questions are loaded
        binding.btnNext.isEnabled = false
    }

    //checking for the radio buttons
    private fun setupOptionCardListeners() {
        binding.cvOption1.setOnClickListener {
            binding.rgOptions.clearCheck()
            binding.rbOption1.isChecked = true
        }
        binding.cvOption2.setOnClickListener {
            binding.rgOptions.clearCheck()
            binding.rbOption2.isChecked = true
        }
        binding.cvOption3.setOnClickListener {
            binding.rgOptions.clearCheck()
            binding.rbOption3.isChecked = true
        }
        binding.cvOption4.setOnClickListener {
            binding.rgOptions.clearCheck()
            binding.rbOption4.isChecked = true
        }

        val radioButtons = listOf(binding.rbOption1, binding.rbOption2, binding.rbOption3, binding.rbOption4)
        radioButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                binding.rgOptions.check(radioButton.id)
            }
        }
    }

    /**
     * Calls the ViewModel to fetch quiz questions and handles the JSON parsing of the response.
     */
    private fun loadQuizData() {
         viewModel.loadQuiz(topic, difficulty) { quizJson ->
            try {
                // Clean the JSON string
                var cleanJson = quizJson.trim()
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```").trim()
                }

                // Parse the cleaned string into a json array of questions
                questions = JSONArray(cleanJson)

                // Check if questions were successfully loaded
                if (questions.length() > 0) {
                    binding.btnNext.isEnabled = true // Enable the next button
                    showQuestion()
                } else {
                    // Handle case where parsing was successful but  array is empty
                    Toast.makeText(this, "Failed to load quiz questions. Try again.", Toast.LENGTH_LONG).show()
                    Log.e("QuizActivity", "Quiz JSON was empty or null.")
                    finish()
                }
            } catch (e: JSONException) {
                // Handle JSON parsing errors
                Toast.makeText(this, "Error parsing quiz data.", Toast.LENGTH_LONG).show()
                Log.e("QuizActivity", "Error parsing quiz JSON: ${e.message}")
                finish()
            }
        }
    }


    /**
     * Displays the current question and its options on the UI.
     * Proceeds to the next question or finishes the quiz if at the end (Islam, 2025).
     */
    private fun showQuestion() {
        binding.rgOptions.clearCheck() // Clear any previous selection

        // Check if there are more questions to display
        if (currentQuestion < questions.length()) {
            try {
                val q = questions.getJSONObject(currentQuestion)
                binding.tvQuestion.text = q.optString("question", "Question Missing")
                val options = q.optJSONArray("options")
                // Update the progress/score
                binding.tvProgress.text = "${currentQuestion + 1}/${questions.length()}"

                // Check for valid options
                if (options != null && options.length() >= 4) {
                    // Populate the option RadioButtons
                    binding.rbOption1.text = options.optString(0, "Option A")
                    binding.rbOption2.text = options.optString(1, "Option B")
                    binding.rbOption3.text = options.optString(2, "Option C")
                    binding.rbOption4.text = options.optString(3, "Option D")

                    // Update the Next button text if it's the last question
                    binding.btnNext.text = if (currentQuestion == questions.length() - 1) "Finish Quiz" else "Next Question"
                } else {
                    // Log warning and skip to the next question if options are invalid
                    Log.w("QuizActivity", "Question $currentQuestion options are missing or less than 4.")
                    currentQuestion++
                    showQuestion()
                }

            } catch (e: JSONException) {
                // Log error and skip to the next question if JSON parsing fails for this question
                Log.e("QuizActivity", "Error loading question $currentQuestion: ${e.message}")
                currentQuestion++
                showQuestion()
            }
        } else {
            finishQuiz()
        }
    }

    //Checks the selected answer against the correct answer and moves to the next question (Islam, 2025)
    private fun checkAnswerAndNext() {
        val selected = when (binding.rgOptions.checkedRadioButtonId) {
            binding.rbOption1.id -> 0
            binding.rbOption2.id -> 1
            binding.rbOption3.id -> 2
            binding.rbOption4.id -> 3
            else -> -1
        }

        //Force user to choose an option
        if (selected == -1) {
            Toast.makeText(this, "Please select an answer option.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check answer if within the bounds of the questions array
        if (currentQuestion < questions.length()) {
            try {
                val q = questions.getJSONObject(currentQuestion)
                val correctIndex = q.getInt("answerIndex")
                if (selected == correctIndex) score++
            } catch (e: JSONException) {
                Log.e("QuizActivity", "Error checking answer for question $currentQuestion: ${e.message}")
            }

            currentQuestion++
            showQuestion() // Display the next question or finish quiz
        } else {
            finishQuiz()
        }
    }
//goes to the page where the results are shown (Islam, 2025)
    private fun finishQuiz() {
        val total = questions.length() // Total number of questions
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("topic", topic)
        intent.putExtra("difficulty", difficulty)
            .putExtra("score", score)
            .putExtra("total", total)

        startActivity(intent)
        finish()
    }
}