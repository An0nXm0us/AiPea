package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomePage : AppCompatActivity() {
    private lateinit var notesBtn : Button
    private lateinit var logOut : Button
    private lateinit var sessionManager: SessionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sessionManager = SessionManager(this)
        notesBtn = findViewById(R.id.button12)
        notesBtn.setOnClickListener {
            val intent = Intent(this, CalenderFront::class.java )
            startActivity(intent)
        }
        logOut = findViewById(R.id.btn_log_out)
        logOut.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            Toast.makeText(this,"Successfully logged off!", Toast.LENGTH_SHORT).show()
        }

    }
}