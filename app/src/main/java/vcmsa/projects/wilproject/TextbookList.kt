package vcmsa.projects.wilproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import vcmsa.projects.wilproject.adapter.BookAdapter
import vcmsa.projects.wilproject.api.BookRepository
import vcmsa.projects.wilproject.databinding.ActivityTextbookListBinding
import vcmsa.projects.wilproject.models.DownloadStatus
import vcmsa.projects.wilproject.models.UiState
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.viewModel.BookViewModel
import vcmsa.projects.wilproject.viewModel.BookViewModelFactory
import java.io.File
import vcmsa.projects.wilproject.models.Book
import vcmsa.projects.wilproject.models.Author
import kotlin.math.absoluteValue


class TextbookList : AppCompatActivity() {

    private val binding: ActivityTextbookListBinding by lazy {
        ActivityTextbookListBinding.inflate(layoutInflater)
    }

    private val bookRepository by lazy { BookRepository() }

    private val viewModelFactory by lazy {
        BookViewModelFactory(application, bookRepository)
    }
    private val viewModel: BookViewModel by viewModels {
        viewModelFactory
    }
    private lateinit var bookAdapter: BookAdapter
    private var isLocalMode = false

    companion object {
        const val EXTRA_LOCAL_PATH = "extra_local_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchInput()
        observeViewModel()

        val localPath = intent.getStringExtra(EXTRA_LOCAL_PATH)
        if (localPath != null) {
            displayLocalBooks(localPath)
            isLocalMode = true
        } else {
            viewModel.searchBooks("")
        }
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            onBookClicked = { },
            onDownloadClicked = { book ->
                viewModel.startDownload(book)
            },
            onPdfViewClicked = { book, filePath ->
                launchBookViewer(filePath, book.title)
            }
        )

        binding.recyclerViewBooks.apply {
            layoutManager = LinearLayoutManager(this@TextbookList)
            adapter = bookAdapter
        }
    }

    private fun setupSearchInput() {

        binding.editTextSearch.setOnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = textView.text.toString().trim()
                if (query.isNotBlank()) {
                    isLocalMode = false
                    viewModel.searchBooks(query)
                    hideKeyboard(textView)
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->

                if (isLocalMode) return@collect

                binding.progressBarLoading.visibility = View.GONE
                binding.textViewStatus.visibility = View.GONE

                when (state) {
                    is UiState.Loading -> {
                        binding.progressBarLoading.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        bookAdapter.submitList(state.books)
                    }
                    is UiState.Error -> {
                        bookAdapter.submitList(emptyList())
                        binding.textViewStatus.text = "Error: ${state.message}"
                        binding.textViewStatus.visibility = View.VISIBLE
                        Toast.makeText(this@TextbookList, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    UiState.Empty -> {
                        bookAdapter.submitList(emptyList())
                        binding.textViewStatus.text = "No books found for this search."
                        binding.textViewStatus.visibility = View.VISIBLE
                    }
                    UiState.Welcome -> {
                        bookAdapter.submitList(emptyList())
                        binding.textViewStatus.text = "Search for a book"
                        binding.textViewStatus.visibility = View.VISIBLE
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.downloadStatuses.collect { statusMap ->
                bookAdapter.updateDownloadStatuses(statusMap)

                statusMap.values.lastOrNull()?.let { status ->
                    when (status) {
                        DownloadStatus.COMPLETE -> {
                            Toast.makeText(this@TextbookList, "Click 'Read' to view.", Toast.LENGTH_SHORT).show()
                        }
                        DownloadStatus.FAILED -> {
                            Toast.makeText(this@TextbookList, "Failed load book. Please try again.", Toast.LENGTH_LONG).show()
                        }
                        else -> { }
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.localBookPaths.collect { paths ->
                bookAdapter.updateLocalPaths(paths)
            }
        }
    }

    private fun displayLocalBooks(directoryPath: String) {
        val directory = File(directoryPath)

        if (!directory.exists() || !directory.isDirectory) {
            binding.progressBarLoading.visibility = View.GONE
            binding.textViewStatus.text = "Error: Local path is not a valid directory."
            binding.textViewStatus.visibility = View.VISIBLE
            return
        }

        val supportedFiles = directory.listFiles()?.filter { file ->
            file.isFile && (file.extension.equals("pdf", ignoreCase = true) || file.extension.equals("epub", ignoreCase = true))
        }

        if (supportedFiles.isNullOrEmpty()) {
            bookAdapter.submitList(emptyList())
            binding.progressBarLoading.visibility = View.GONE
            binding.textViewStatus.text = "No books found locally on device. Search for a book to download"
            binding.textViewStatus.visibility = View.VISIBLE
            return
        }

        val localBooks = supportedFiles.map { file ->
            val bookId = file.absolutePath.hashCode().absoluteValue

            val formatKey = "application/${file.extension.lowercase()}"
            val formatsMap = mapOf(formatKey to file.absolutePath)

            Book(
                id = bookId,
                title = file.nameWithoutExtension,
                authors = listOf(Author(name = "Local Document")),
                formats = formatsMap
            )
        }

        bookAdapter.submitList(localBooks)

        val localPathsMap = localBooks.associate { book ->
            val matchingFile = supportedFiles.firstOrNull { it.nameWithoutExtension == book.title }
            book.id to matchingFile?.absolutePath
        }.filterValues { it != null } as Map<Int, String>

        val downloadStatusMap = localBooks.associate { it.id to DownloadStatus.COMPLETE }

        bookAdapter.updateLocalPaths(localPathsMap)
        bookAdapter.updateDownloadStatuses(downloadStatusMap)

        binding.progressBarLoading.visibility = View.GONE
        binding.textViewStatus.text = "Displaying local books from: $directoryPath"
        binding.textViewStatus.visibility = View.VISIBLE

        binding.editTextSearch.setText("")
    }

    private fun launchBookViewer(filePath: String, title: String? = null) {
        val lowerCasePath = filePath.lowercase()
        val file = File(filePath)

        if (!file.exists()) {
            Toast.makeText(this, "Error: Book file not found. Try downloading again.", Toast.LENGTH_LONG).show()
            return
        }

        val intent = when {
            lowerCasePath.endsWith(".pdf") -> {
                Intent(this, PdfViewerActivity::class.java).apply {
                    putExtra("extra_file_path", filePath)
                    if (title != null) putExtra("extra_book_title", title)
                }
            }
            lowerCasePath.endsWith(".epub") -> {
                Intent(this, EpubViewerActivity::class.java).apply {
                    putExtra(EpubViewerActivity.EXTRA_FILE_PATH, filePath)
                    if (title != null) putExtra(EpubViewerActivity.EXTRA_BOOK_TITLE, title)
                }
            }
            else -> {
                Toast.makeText(this,
                    "Unsupported file format (${filePath.substringAfterLast('.')}). Cannot open.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        startActivity(intent)
    }
}