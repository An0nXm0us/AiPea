package com.example.eddiequiz

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.eddiequiz.databinding.ActivityQuizBinding
import com.example.eddiequiz.QuizViewModel
import org.json.JSONArray

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private lateinit var viewModel: QuizViewModel
    private var topic: String = ""
    private var difficulty = 1
    private var currentQuestion = 0
    private var score = 0
    private var questions = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        topic = intent.getStringExtra("topic") ?: ""

        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]
        viewModel.loadQuiz(topic, difficulty) { quizJson ->
            questions = JSONArray(quizJson)
            showQuestion()
        }

        binding.btnNext.setOnClickListener { checkAnswerAndNext() }
    }

    private fun showQuestion() {
        if (currentQuestion < questions.length()) {
            val q = questions.getJSONObject(currentQuestion)
            binding.tvQuestion.text = q.getString("question")
            val options = q.getJSONArray("options")
            binding.rbOption1.text = options.getString(0)
            binding.rbOption2.text = options.getString(1)
            binding.rbOption3.text = options.getString(2)
            binding.rbOption4.text = options.getString(3)
        } else {
            finishQuiz()
        }
    }

    private fun checkAnswerAndNext() {
        val q = questions.getJSONObject(currentQuestion)
        val correctIndex = q.getInt("answerIndex")
        val selected = when (binding.rgOptions.checkedRadioButtonId) {
            binding.rbOption1.id -> 0
            binding.rbOption2.id -> 1
            binding.rbOption3.id -> 2
            binding.rbOption4.id -> 3
            else -> -1
        }

        if (selected == correctIndex) score++
        currentQuestion++
        binding.rgOptions.clearCheck()
        showQuestion()
    }

    private fun finishQuiz() {
        val total = questions.length()
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("topic", topic)
        intent.putExtra("difficulty", difficulty)
        intent.putExtra("score", score)
        intent.putExtra("total", total)
        startActivity(intent)
        finish()
    }
}