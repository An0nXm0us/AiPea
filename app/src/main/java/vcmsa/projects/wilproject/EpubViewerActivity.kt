package vcmsa.projects.wilproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    private var selectedText: String? = null
    private var extractedCacheDir: File? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epub_viewer)

        webViewReader = findViewById(R.id.webview_reader)
        progressBar = findViewById(R.id.progress_bar_loading)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "EPUB Reader"

        supportActionBar?.title = bookTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webViewReader.settings.javaScriptEnabled = true
        webViewReader.addJavascriptInterface(WebViewInterface(), "Android")

        webViewReader.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectHighlightingScript(view)
                progressBar.visibility = View.GONE
                webViewReader.visibility = View.VISIBLE
            }
        }

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

    override fun onDestroy() {
        super.onDestroy()
        extractedCacheDir?.let { dir ->
            lifecycleScope.launch(Dispatchers.IO) {
                if (dir.exists()) {
                    val success = dir.deleteRecursively()
                    if (success) {
                        Log.d(TAG, "Successfully extracted EPUB files.")
                    } else {
                        Log.e(TAG, "Failed to delete extracted EPUB files.")
                    }
                }
            }
        }
    }

    private fun injectHighlightingScript(webView: WebView?) {
        val script = """
            (function() {
                function getSelectionText() {
                    var text = "";
                    if (window.getSelection) {
                        text = window.getSelection().toString();
                    } else if (document.selection && document.selection.type != "Control") {
                        text = document.selection.createRange().text;
                    }
                    return text.trim();
                }

                document.addEventListener('mouseup', function() {
                    var selectedText = getSelectionText();
                    if (selectedText.length > 0) {
                        Android.onTextSelected(selectedText);
                    } else {
                        Android.onSelectionCleared();
                    }
                }, false);
                
                window.highlightSelection = function() {
                    var selection = window.getSelection();
                    if (selection && selection.rangeCount > 0) {
                        var range = selection.getRangeAt(0);
                        var span = document.createElement('span');
                        span.style.backgroundColor = 'yellow';
                        span.style.borderRadius = '3px';
                        span.className = 'app-highlight';
                        
                        try {
                            range.surroundContents(span);
                        } catch (e) {
                            console.error("Highlight failed: ", e);
                        }
                        selection.removeAllRanges();
                    }
                };
            })();
        """.trimIndent()

        webView?.evaluateJavascript(script, null)
    }

    private inner class WebViewInterface {
        @JavascriptInterface
        fun onTextSelected(text: String) {
            selectedText = text
            runOnUiThread { invalidateOptionsMenu() }
            Log.d(TAG, "Text selected: $text")
        }

        @JavascriptInterface
        fun onSelectionCleared() {
            selectedText = null
            runOnUiThread { invalidateOptionsMenu() }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_epub_reader, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val highlightItem = menu?.findItem(R.id.action_highlight)
        highlightItem?.isVisible = !selectedText.isNullOrBlank()
        return super.onPrepareOptionsMenu(menu)
    }

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

    private fun applyHighlight() {
        if (!selectedText.isNullOrBlank()) {
            webViewReader.evaluateJavascript("window.highlightSelection();", null)
            Toast.makeText(this, "Highlighted: ${selectedText!!.take(30)}...", Toast.LENGTH_SHORT).show()
            selectedText = null
            invalidateOptionsMenu()

        }
    }


    private fun loadEpubContent(file: File) {
        progressBar.visibility = View.VISIBLE
        webViewReader.visibility = View.GONE

        lifecycleScope.launch {
            val combinedHtmlAndCacheDir = withContext(Dispatchers.IO) {
                try {
                    MinimalEpubReader.getEpubHtmlContent(file, cacheDir)
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal failure to parse EPUB: ${e.message}", e)
                    null
                }
            }

            if (combinedHtmlAndCacheDir != null) {
                val (htmlContent, cacheDir) = combinedHtmlAndCacheDir
                extractedCacheDir = cacheDir

                webViewReader.loadDataWithBaseURL(
                    cacheDir.toURI().toString(),
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            } else {
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
