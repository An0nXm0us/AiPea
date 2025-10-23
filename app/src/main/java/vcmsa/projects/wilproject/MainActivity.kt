package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // check if user is logged in
        if (sessionManager.isLoggedIn()) {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
            return
        }

        // If not logged in,  show start screen
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val btnStart: TextView = findViewById(R.id.startBtn)
        btnStart.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)

        }
    }
}