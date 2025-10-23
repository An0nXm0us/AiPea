package com.example.eddiequiz

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eddiequiz.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartQuiz.setOnClickListener {
            val topic = binding.etTopic.text.toString().trim()
            if (topic.isNotEmpty()) {
                val intent = Intent(this, QuizActivity::class.java)
                intent.putExtra("topic", topic)
                startActivity(intent)
            } else {
                binding.etTopic.error = "Please enter a topic"
            }
        }
    }
}