package com.example.eddiequiz

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.eddiequiz.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var viewModel: QuizViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val topic = intent.getStringExtra("topic") ?: ""
        val difficulty = intent.getIntExtra("difficulty", 1)
        val score = intent.getIntExtra("score", 0)
        val total = intent.getIntExtra("total", 0)

        binding.tvResult.text = "You scored $score / $total"

        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]
        viewModel.recordScore(topic, difficulty, score, total)
    }
}