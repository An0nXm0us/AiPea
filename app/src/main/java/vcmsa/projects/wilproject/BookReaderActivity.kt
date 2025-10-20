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

class BookReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_FILE_PATH = "book_file_path"
        private const val TAG = "BookReaderActivity"
    }

    private lateinit var webViewReader: WebView
    private lateinit var progressBar: ProgressBar

    private var selectedText: String? = null
    private var extractedCacheDir: File? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)

        webViewReader = findViewById(R.id.webview_book_content)
        progressBar = findViewById(R.id.progress_bar_loading)

        val filePath = intent.getStringExtra(EXTRA_BOOK_FILE_PATH)
        supportActionBar?.title = "Book Reader"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webViewReader.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        webViewReader.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
        }

        webViewReader.addJavascriptInterface(WebViewInterface(), "Android")

        webViewReader.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectHighlightingScript(view)
                progressBar.visibility = View.GONE
                webViewReader.visibility = View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return if (url.startsWith("http") || url.startsWith("https")) {
                    true
                } else {
                    false
                }
            }
        }

        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                loadEpubContent(file)
            } else {
                Toast.makeText(this, "Error: EPUB file not found locally.", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                finish()
            }
        } else {
            Toast.makeText(this, "Error: No file path provided.", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
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
                        Log.d(TAG, "Successfully cleaned up extracted EPUB files.")
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
                window.lastSelectionRange = null;

                function getSelectionInfo() {
                    var selection = window.getSelection();
                    var selectedText = selection.toString().trim();
                    
                    if (selectedText.length > 0 && selection.rangeCount > 0) {
                        window.lastSelectionRange = selection.getRangeAt(0).cloneRange();
                        return { text: selectedText, hasSelection: true };
                    } else {
                        window.lastSelectionRange = null;
                        return { text: "", hasSelection: false };
                    }
                }

                document.addEventListener('mouseup', handleSelection, false);
                document.addEventListener('touchend', handleSelection, false);
                
                function handleSelection() {
                    setTimeout(function() {
                        var info = getSelectionInfo();
                        if (info.hasSelection) {
                            Android.onTextSelected(info.text);
                        } else {
                            Android.onSelectionCleared();
                        }
                    }, 100);
                }

                window.highlightSelection = function() {
                    var range = window.lastSelectionRange;

                    if (range) {
                        var span = document.createElement('span');
                        span.className = 'app-highlight';
                        span.style.backgroundColor = '#ffc000';
                        span.style.borderRadius = '3px';
                        
                        try {
                            range.surroundContents(span);
                            console.log("Highlighting succeeded with surroundContents.");
                        } catch (e) {
                            console.error("Highlighting failed: The selection likely crosses complex or non-text nodes, preventing reliable DOM insertion. Error: ", e);
                        }
                        
                        window.lastSelectionRange = null;
                        if (window.getSelection) {
                            window.getSelection().removeAllRanges();
                        }
                    } else {
                         console.log("No valid range stored for highlighting. Selection was lost or cleared.");
                    }
                };

                
                window.clearSearchMarks = function() {
                    var highlights = document.querySelectorAll('.app-search-mark');
                    highlights.forEach(function(span) {
                        var parent = span.parentNode;
                        while (span.firstChild) parent.insertBefore(span.firstChild, span);
                        parent.removeChild(span);
                    });
                    document.body.normalize(); 
                };
                
                window.findAndMarkText = function(term) {
                    if (!term) return 0;
                    
                    window.clearSearchMarks();
                    var count = 0;
                    var termLower = term.toLowerCase();

                    function wrapTextNodes(node) {
                        if (node.nodeType === Node.TEXT_NODE) {
                            var text = node.nodeValue;
                            var index = text.toLowerCase().indexOf(termLower);

                            if (index >= 0) {
                                var container = document.createDocumentFragment();
                                var lastIndex = 0;

                                while (index >= 0) {
                                    count++;
                                    
                                    if (index > lastIndex) {
                                        container.appendChild(document.createTextNode(text.substring(lastIndex, index)));
                                    }
                                    
                                    var mark = document.createElement('span');
                                    mark.className = 'app-search-mark';
                                    mark.style.backgroundColor = '#ffff00'; 
                                    mark.textContent = text.substring(index, index + term.length);
                                    container.appendChild(mark);
                                    
                                    lastIndex = index + term.length;
                                    index = text.toLowerCase().indexOf(termLower, lastIndex);
                                }

                                if (lastIndex < text.length) {
                                    container.appendChild(document.createTextNode(text.substring(lastIndex)));
                                }

                                node.parentNode.replaceChild(container, node);
                                return;
                            }
                        } else if (node.nodeType === Node.ELEMENT_NODE && 
                                   node.tagName !== 'SCRIPT' && node.tagName !== 'STYLE' &&
                                   !node.classList.contains('app-highlight') && !node.classList.contains('app-search-mark')) {
                            var childNodes = Array.from(node.childNodes);
                            childNodes.forEach(wrapTextNodes);
                        }
                    }
                    
                    wrapTextNodes(document.body);
                    Android.onSearchCompleted(count);
                    return count;
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
        }

        @JavascriptInterface
        fun onSelectionCleared() {
            selectedText = null
            runOnUiThread { invalidateOptionsMenu() }
        }

        @JavascriptInterface
        fun onSearchCompleted(count: Int) {
            runOnUiThread {
                Toast.makeText(
                    this@BookReaderActivity,
                    "Search found $count matches.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_book_reader, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val highlightItem = menu?.findItem(R.id.action_highlight)
        highlightItem?.isVisible = !selectedText.isNullOrBlank()

        val searchItem = menu?.findItem(R.id.action_search)
        searchItem?.isVisible = true

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_highlight -> {
                applyHighlight()
                true
            }
            R.id.action_search -> {
                handleSearchAction()
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
            webViewReader.evaluateJavascript("window.clearSearchMarks();", null)
        }
    }

    private fun handleSearchAction() {
        webViewReader.evaluateJavascript("window.clearSearchMarks();", null)

        val jsCall = """
            var searchTerm = prompt('Enter search term (Case Insensitive):');
            if (searchTerm) {
                window.findAndMarkText(searchTerm);
            }
        """.trimIndent()

        webViewReader.evaluateJavascript(jsCall, null)
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
                Toast.makeText(this@BookReaderActivity, "Failed to parse EPUB file.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
