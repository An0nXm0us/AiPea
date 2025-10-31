package vcmsa.projects.wilproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

//Activity responsible for displaying a PDF
class PdfViewerActivity : AppCompatActivity() {

    // Define constant keys for Intent extras
    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
    }

    private lateinit var pdfView: PDFView // Reference to the PDF viewer component
    private lateinit var bottomNavigation: BottomNavigationView // Reference to the bottom navigation bar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        pdfView = findViewById(R.id.pdf_view)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "PDF Reader"
        bottomNavigation = findViewById(R.id.bottom_navigation)

        setupBottomNavigation()

        supportActionBar?.title = bookTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Check if a file path was successfully received
        if (filePath != null) {
            val file = File(filePath) // Create a File object from the path

            // Check if the file actually exists on the device
            if (file.exists()) {
                Log.d("PdfViewerActivity", "Loading PDF from path: $filePath")

                // Start loading the PDF
                pdfView.fromFile(file)
                    .defaultPage(0)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .load() // Load and display the PDF

            } else {
                // Handle case where file path is received but the file is missing
                Toast.makeText(this, "Error: PDF file not found locally.", Toast.LENGTH_LONG).show()
                Log.e("PdfViewerActivity", "File path received, but file does not exist: $filePath")
                finish() // Close the activity
            }
        } else {
            // Handle case where no file path was passed in the Intent
            Toast.makeText(this, "Error: No file path provided.", Toast.LENGTH_LONG).show()
            finish() // Close the activity
        }
    }

    //Handles navigation to different sections of the app.
      private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.btnHome

        // Set up navigation listener
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnHome -> {
                    val intent = Intent(this, HomePage::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.btnCalendar -> {
                    openCalendarFront()
                    true
                }
                R.id.btnNotes -> {
                    openNotesFront()
                    true
                }
                R.id.btnTextbook -> {
                    openTextbookFront()
                    true
                }
                R.id.btnAI -> {
                    openChatHistory()
                    true
                }
                else -> false
            }
        }
    }

    // navigation to other pages

    private fun openChatHistory() {
        val intent = Intent(this, ChatHistory::class.java)
        startActivity(intent)
    }

    private fun openNotesFront() {
        val intent = Intent(this, NotesFront::class.java)
        startActivity(intent)
    }

    private fun openCalendarFront() {
        val intent = Intent(this, CalenderFront::class.java)
        startActivity(intent)
    }

    private fun openTextbookFront() {
        val intent = Intent(this, TextbookList::class.java)
        startActivity(intent)
    }


    override fun onSupportNavigateUp(): Boolean {
        finish() // Simply close the current activity
        return true
    }
}