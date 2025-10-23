package vcmsa.projects.wilproject.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import vcmsa.projects.wilproject.models.DownloadStatus
import vcmsa.projects.wilproject.models.UiState
import vcmsa.projects.wilproject.models.Book as UIBook // Alias to prevent naming conflict with network Book model
import vcmsa.projects.wilproject.api.BookRepository // Use the correct Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class BookViewModel(application: Application, private val repository: BookRepository) : AndroidViewModel(application) {
    private val applicationContext = application.applicationContext

    private val downloadDir: File = applicationContext.filesDir

    private val _uiState = MutableStateFlow<UiState>(UiState.Welcome)
    val uiState: StateFlow<UiState> = _uiState

    private val _downloadStatuses = MutableStateFlow<Map<Int, DownloadStatus>>(emptyMap())
    val downloadStatuses: StateFlow<Map<Int, DownloadStatus>> = _downloadStatuses

    private val _localBookPaths = MutableStateFlow<Map<Int, String>>(emptyMap())
    val localBookPaths: StateFlow<Map<Int, String>> = _localBookPaths

    init {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
    }

    fun searchBooks(query: String) {
        if (query.isBlank()) return
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val books = repository.searchBooks(query)

                Log.d("BookViewModel", "Repository returned ${books.size} downloadable books for '$query'.")

                _uiState.value = if (!books.isEmpty()) {
                    UiState.Success(books)
                } else {
                    UiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to fetch books: ${e.message}")
            }
        }
    }

    fun startDownload(book: UIBook) {
        val downloadUrl = book.getDownloadUrl()
        val extension = book.getDownloadExtension()

        if (downloadUrl == null || extension == null) {
            _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.FAILED)
            Log.e("BookViewModel", "Cannot download book ${book.id}: Download URL or file format extension is missing.")
            return
        }

        val safeTitlePart = book.title.take(10).replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "${book.id}_${safeTitlePart}${extension}"
        val targetFile = File(downloadDir, fileName)

        if (targetFile.exists()) {
            Log.d("BookViewModel", "File ${book.id} already exists. Skipping download.")
            _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.COMPLETE)
            _localBookPaths.value = _localBookPaths.value + (book.id to targetFile.absolutePath)
            return
        }

        _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.PENDING)

        viewModelScope.launch {
            _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.DOWNLOADING)

            val localFilePath = withContext(Dispatchers.IO) {
                try {
                    val url = URL(downloadUrl)
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.connectTimeout = 30000 // 30 seconds
                    connection.readTimeout = 30000    // 30 seconds

                    connection.connect()

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        Log.e("BookViewModel", "Server returned error code: ${connection.responseCode}")
                        return@withContext null
                    }

                    connection.getInputStream().use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    targetFile.absolutePath // Download successful
                } catch (e: Exception) {
                    Log.e("BookViewModel", "Download failed for Book ID ${book.id}: ${e.message}", e)
                    targetFile.delete() // Clean up partial download
                    null // Download failed
                }
            }

            if (localFilePath != null) {
                _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.COMPLETE)
                _localBookPaths.value = _localBookPaths.value + (book.id to localFilePath)
            } else {
                _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.FAILED)
            }
        }
    }


}
