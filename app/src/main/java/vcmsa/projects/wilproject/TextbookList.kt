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
import com.google.android.material.bottomnavigation.BottomNavigationView
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


// Handles search, fetching and displaying of books from the API
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
    private lateinit var bottomNavigation: BottomNavigationView
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

      bottomNavigation = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()

        // Check if a local path was passed in the Intent
        val localPath = intent.getStringExtra(EXTRA_LOCAL_PATH)
        if (localPath != null) {
            // If local path exists, switch to local file display mode
            displayLocalBooks(localPath)
            isLocalMode = true
        } else {
            viewModel.searchBooks("")
        }
    }


    //Sets up RecyclerView to initialise BookAdapter
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            onBookClicked = {
            },
            onDownloadClicked = { book ->
                // Start the download process via the ViewModel
                viewModel.startDownload(book)
            },
            onPdfViewClicked = { book, filePath ->
                // Launch the appropriate viewer activity for the downloaded file
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
            // Check if the action performed is the 'Search' key
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

    /**
     * Helper function to hide the software keyboard.
     * @param view The view that currently has focus.
     */
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    private fun observeViewModel() {
        // Observer for the main UI state changes
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
                        // Display the list of books fetched from search
                        bookAdapter.submitList(state.books)
                    }
                    is UiState.Error -> {
                        // Display error message
                        bookAdapter.submitList(emptyList())
                        binding.textViewStatus.text = "Error: ${state.message}"
                        binding.textViewStatus.visibility = View.VISIBLE
                        Toast.makeText(this@TextbookList, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    UiState.Empty -> {
                        // Display no results message
                        bookAdapter.submitList(emptyList())
                        binding.textViewStatus.text = "No books found for this search."
                        binding.textViewStatus.visibility = View.VISIBLE
                    }
                    UiState.Welcome -> {
                        // Display welcome/initial prompt
                        bookAdapter.submitList(emptyList())
                        binding.textViewStatus.text = "Search for a book"
                        binding.textViewStatus.visibility = View.VISIBLE
                    }
                    else -> { /* Ignore other states */ }
                }
            }
        }

        // Observer for download status changes
        lifecycleScope.launch {
            viewModel.downloadStatuses.collect { statusMap ->
                bookAdapter.updateDownloadStatuses(statusMap)
                statusMap.values.lastOrNull()?.let { status ->
                    when (status) {
                        DownloadStatus.COMPLETE -> {
                            Toast.makeText(this@TextbookList, "Download complete. Click 'Read' to view.", Toast.LENGTH_SHORT).show()
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

      //Handles displaying local PDF/EPUB files from a specified directory.
    private fun displayLocalBooks(directoryPath: String) {
        val directory = File(directoryPath)

        // Validate the directory path
        if (!directory.exists() || !directory.isDirectory) {
            binding.progressBarLoading.visibility = View.GONE
            binding.textViewStatus.text = "Error: Local path is not a valid directory."
            binding.textViewStatus.visibility = View.VISIBLE
            return
        }

        // Filter files to include only supported formats (pdf, epub)
        val supportedFiles = directory.listFiles()?.filter { file ->
            file.isFile && (file.extension.equals("pdf", ignoreCase = true) || file.extension.equals("epub", ignoreCase = true))
        }

        // Handle case where no supported files are found
        if (supportedFiles.isNullOrEmpty()) {
            bookAdapter.submitList(emptyList())
            binding.progressBarLoading.visibility = View.GONE
            binding.textViewStatus.text = "No books found locally on device. Search for a book to download"
            binding.textViewStatus.visibility = View.VISIBLE
            return
        }

        // Map local files to Book data models
        val localBooks = supportedFiles.map { file ->
            // Generate a unique ID from the file path hashcode
            val bookId = file.absolutePath.hashCode().absoluteValue

            // Create a formats map to store the local path
            val formatKey = "application/${file.extension.lowercase()}"
            val formatsMap = mapOf(formatKey to file.absolutePath)

            Book(
                id = bookId,
                title = file.nameWithoutExtension, // Use the file name as the title
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

        // Update UI status to reflect local mode
        binding.progressBarLoading.visibility = View.GONE
        binding.textViewStatus.text = "Displaying local books from: $directoryPath"
        binding.textViewStatus.visibility = View.VISIBLE

        // Clear the search bar text
        binding.editTextSearch.setText("")
    }

    //launches to either PDF or EPUB viewer based on file extension
    private fun launchBookViewer(filePath: String, title: String? = null) {
        val lowerCasePath = filePath.lowercase()
        val file = File(filePath)

        // Verify the file exists
        if (!file.exists()) {
            Toast.makeText(this, "Error: Book file not found. Try viewing the book again.", Toast.LENGTH_LONG).show()
            return
        }

        // Determine which view to use based on the file extention
        val intent = when {
            lowerCasePath.endsWith(".pdf") -> {
                // Launch PDF Viewer Activity
                Intent(this, PdfViewerActivity::class.java).apply {
                    putExtra("extra_file_path", filePath)
                    if (title != null) putExtra("extra_book_title", title)
                }
            }
            lowerCasePath.endsWith(".epub") -> {
                // Launch EPUB Viewer Activity
                Intent(this, EpubViewerActivity::class.java).apply {
                    putExtra(EpubViewerActivity.EXTRA_FILE_PATH, filePath)
                    if (title != null) putExtra(EpubViewerActivity.EXTRA_BOOK_TITLE, title)
                }
            }
            else -> {
                // Handle unsupported file types
                Toast.makeText(this,
                    "Unsupported file format (${filePath.substringAfterLast('.')}). Cannot open.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        startActivity(intent)
    }


    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.btnTextbook

        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnHome -> {
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

    //methods to go to other pages
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

}