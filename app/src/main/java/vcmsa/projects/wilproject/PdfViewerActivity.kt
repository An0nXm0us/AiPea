package vcmsa.projects.wilproject

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
    }

    private lateinit var pdfView: PDFView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        pdfView = findViewById(R.id.pdf_view)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "PDF Reader"

        supportActionBar?.title = bookTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (filePath != null) {
            val file = File(filePath)

            if (file.exists()) {
                Log.d("PdfViewerActivity", "Loading PDF from path: $filePath")

                pdfView.fromFile(file)
                    .defaultPage(0)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .load()

            } else {
                Toast.makeText(this, "Error: PDF file not found locally.", Toast.LENGTH_LONG).show()
                Log.e("PdfViewerActivity", "File path received, but file does not exist: $filePath")
                finish()
            }
        } else {
            Toast.makeText(this, "Error: No file path provided.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
