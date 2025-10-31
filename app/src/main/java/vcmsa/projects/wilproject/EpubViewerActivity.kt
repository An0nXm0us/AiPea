package vcmsa.projects.wilproject
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import vcmsa.projects.wilproject.utils.MinimalEpubReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Exception

class EpubViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
        private const val TAG = "EpubViewerActivity"
    }

    private lateinit var webViewReader: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigation: BottomNavigationView

    private var selectedText: String? = null

    private var extractedCacheDir: File? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epub_viewer)

        webViewReader = findViewById(R.id.webview_reader)
        progressBar = findViewById(R.id.progress_bar_loading)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "EPUB Reader"

        supportActionBar?.title = bookTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val settings = webViewReader.settings
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        webViewReader.isLongClickable = true
        webViewReader.setOnLongClickListener {
            selectedText = "trigger"
            invalidateOptionsMenu()
            false
        }

        webViewReader.addJavascriptInterface(WebViewInterface(), "Android")

        // Custom WebViewClient to control navigation and page loading events. (Codes Easy ,2021)
        webViewReader.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectHighlightingScript(view)
                progressBar.visibility = View.GONE
                webViewReader.visibility = View.VISIBLE
            }

            // Prevents the WebView from navigating to external websites (http/https) (Codes Easy ,2021)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d(TAG, "Attempting to load URL: $url")

                if (url.startsWith("http")) {
                    Toast.makeText(this@EpubViewerActivity, "External links are disabled in the reader.", Toast.LENGTH_SHORT).show()
                    return true
                }

                return false
            }
        }

        // Start EPUB loading process (Codes Easy ,2021)
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                loadEpubContent(file)
            } else {
                Toast.makeText(this, "Error: EPUB file not found.", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Error: No file path provided.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    //Cleans up the temporary extracted EPUB files when the activity is destroyed

    override fun onDestroy() {
        super.onDestroy()
        extractedCacheDir?.let { dir ->
            lifecycleScope.launch(Dispatchers.IO) {
                if (dir.exists()) {
                    val success = dir.deleteRecursively() // Delete the entire directory tree
                    if (success) {
                        Log.d(TAG, "Successfully deleted extracted EPUB files at: ${dir.absolutePath}")
                    } else {
                        Log.e(TAG, "Failed to delete extracted EPUB files at: ${dir.absolutePath}")
                    }
                }
            }
        }
    }

    //highlight functionality (Codes Easy ,2021 & The Android Factory, 2025)
    private fun injectHighlightingScript(webView: WebView?) {
        val script = """
            (function() {
                window.getSelectionText = function() {
                    if (window.getSelection) {
                        return window.getSelection().toString().trim();
                    }
                    return "";
                };

                window.getHighlightRects = function() {
                    var selection = window.getSelection();
                    if (!selection.rangeCount) return "[]";

                    var range = selection.getRangeAt(0);
                    var rects = range.getClientRects();
                    var rectArray = [];

                    for (var i = 0; i < rects.length; i++) {
                        var rect = rects[i];
                        rectArray.push({
                            left: rect.left + window.pageXOffset,
                            top: rect.top + window.pageYOffset,
                            width: rect.width,
                            height: rect.height
                        });
                    }

                    return JSON.stringify(rectArray);
                };

                window.drawHighlightSpans = function(rectsJson) {
                    var rects = JSON.parse(rectsJson);
                    var container = document.body;

                    rects.forEach(function(rect) {
                        var span = document.createElement('span');
                        span.className = 'manual-highlight';

                        span.style.position = 'absolute';
                        span.style.backgroundColor = 'rgba(255, 255, 0, 0.5)';
                        span.style.zIndex = '9999';
                        span.style.pointerEvents = 'none'; 
                        span.style.left = rect.left + 'px';
                        span.style.top = rect.top + 'px';
                        span.style.width = rect.width + 'px';
                        span.style.height = rect.height + 'px';

                        container.appendChild(span);
                    });
                };
            })();
        """.trimIndent()

        webView?.evaluateJavascript(script, null)
    }


    // Interface class to enable communication with JavaScript (Codes Easy ,2021)

    private inner class WebViewInterface {
        @JavascriptInterface
        fun onTextSelected(text: String) {

        }
    }


    //shows menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_epub_reader, menu)
        return true
    }

   //shows options for menu
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // Show the highlight button only if text selection is triggered (QCT,2023)
        val highlightItem = menu?.findItem(R.id.action_highlight)
        highlightItem?.isVisible = !selectedText.isNullOrBlank()
        return super.onPrepareOptionsMenu(menu)
    }

    //handles menu selection
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_highlight -> {
                applyHighlight()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //Highlight functionality (Codes Easy ,2021 & QCT,2023)
    private fun applyHighlight() {
        webViewReader.evaluateJavascript("window.getSelectionText();") { textString ->
            val currentText = textString.trim().replace("\"", "")

            if (currentText.isNotEmpty()) {
                webViewReader.evaluateJavascript("window.getHighlightRects();") { rectsJson ->
                    if (rectsJson.isNullOrBlank() || rectsJson == "[]" || rectsJson == "null") {
                        Toast.makeText(this, "Highlight failed. Selection lost or invalid.", Toast.LENGTH_SHORT).show()
                        return@evaluateJavascript
                    }

                    webViewReader.evaluateJavascript("window.getSelection().removeAllRanges();", null)

                    webViewReader.evaluateJavascript("window.drawHighlightSpans('$rectsJson');", null)
                    Toast.makeText(this, "Highlighted: ${currentText.take(30)}...", Toast.LENGTH_SHORT).show()
                   selectedText = null
                    invalidateOptionsMenu()
                }
            } else {
                Toast.makeText(this, "No text selected to highlight.", Toast.LENGTH_SHORT).show()
                selectedText = null
                invalidateOptionsMenu()
            }
        }
    }


    //loads epub (Codes Easy ,2021)
    private fun loadEpubContent(file: File) {
        progressBar.visibility = View.VISIBLE
        webViewReader.visibility = View.GONE

        lifecycleScope.launch {
            val combinedHtmlAndCacheDir = withContext(Dispatchers.IO) {
                try {
                    // MinimalEpubReader extracts assets to a directory
                    // and returns the main HTML content string Codes Easy ,2021 & Coding Shiksha, 2023)
                    MinimalEpubReader.getEpubHtmlContent(file, cacheDir)
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal failure to parse EPUB: ${e.message}", e)
                    null
                }
            }

           if (combinedHtmlAndCacheDir != null) {
                val (htmlContent, cacheDir) = combinedHtmlAndCacheDir
                extractedCacheDir = cacheDir

                // Load  content, using the cache directory  as the Base URL.
                webViewReader.loadDataWithBaseURL(
                    cacheDir.toURI().toString(),
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            } else {
                // Handle parsing failure (Coding Shiksha, 2023)
                progressBar.visibility = View.GONE
                webViewReader.visibility = View.VISIBLE
                webViewReader.loadDataWithBaseURL(
                    null,
                    "<h1>Error Loading Book</h1><p>Failed to parse or load the EPUB content. Check logs for details.</p>",
                    "text/html",
                    "UTF-8",
                    null
                )
                Toast.makeText(this@EpubViewerActivity, "Failed to load book content.", Toast.LENGTH_LONG).show()
            }
        }
    }
//bottom navigation
    private fun setupBottomNavigation() {
        // Set the home item as selected by default
        bottomNavigation.selectedItemId = R.id.btnTextbook

        // Set up navigation listener to switch activities
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnHome -> {
                    // Start HomePage and clear back stack to prevent navigation loop.
                    val intent = Intent(this, HomePage::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish() // Close current activity
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

    // functions that will redirect to other pages
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
        finish()
        return true
    }
}
