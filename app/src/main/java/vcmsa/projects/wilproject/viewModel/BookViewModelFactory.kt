package vcmsa.projects.wilproject.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vcmsa.projects.wilproject.api.BookRepository // Changed to use the Repository


class BookViewModelFactory(
    private val application: Application,
    private val repository: BookRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel class is BookViewModel
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            // Return a new instance, injecting both dependencies
            return BookViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
