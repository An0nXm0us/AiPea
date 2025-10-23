package vcmsa.projects.wilproject

import android.annotation.SuppressLint
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
        webViewReader.settings.allowFileAccess = true
        webViewReader.settings.allowFileAccessFromFileURLs = true
        webViewReader.settings.allowUniversalAccessFromFileURLs = true


        webViewReader.isLongClickable = true
        webViewReader.setOnLongClickListener {
            selectedText = "trigger"
            invalidateOptionsMenu()
            false
        }

        webViewReader.addJavascriptInterface(WebViewInterface(), "Android")

        webViewReader.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectHighlightingScript(view)
                progressBar.visibility = View.GONE
                webViewReader.visibility = View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d(TAG, "Attempting to load URL: $url")

                if (url.startsWith("http")) {
                    return true
                }

                return false
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
                        Log.d(TAG, "Successfully deleted extracted EPUB files at: ${dir.absolutePath}")
                    } else {
                        Log.e(TAG, "Failed to delete extracted EPUB files at: ${dir.absolutePath}")
                    }
                }
            }
        }
    }

    private fun injectHighlightingScript(webView: WebView?) {
        val script = """
            (function() {
                // Function to get the currently selected text, called only when the button is pressed
                window.getSelectionText = function() {
                    if (window.getSelection) {
                        return window.getSelection().toString().trim();
                    }
                    return ""; 
                };

                // Function to get coordinates for robust, coordinate-based highlighting
                window.getHighlightRects = function() {
                    var selection = window.getSelection();
                    if (!selection.rangeCount) return "[]";

                    var range = selection.getRangeAt(0);
                    var rects = range.getClientRects();
                    var rectArray = [];

                    for (var i = 0; i < rects.length; i++) {
                        var rect = rects[i];
                        // Add scroll offset to get absolute page coordinates
                        rectArray.push({
                            left: rect.left + window.pageXOffset,
                            top: rect.top + window.pageYOffset,
                            width: rect.width,
                            height: rect.height
                        });
                    }
                    
                    return JSON.stringify(rectArray);
                };

                // Function to manually draw the highlight spans using coordinates
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


    private inner class WebViewInterface {
        @JavascriptInterface
        fun onTextSelected(text: String) {
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