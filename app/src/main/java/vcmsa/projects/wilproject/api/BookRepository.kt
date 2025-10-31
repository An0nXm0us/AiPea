package vcmsa.projects.wilproject.api

import vcmsa.projects.wilproject.models.BookResponse
import android.util.Log
import vcmsa.projects.wilproject.models.Author
import vcmsa.projects.wilproject.models.Book

/**
 * Repository class responsible for handling data operations,
 * specifically fetching book search results from the API (Philipp Lackner,2021).
 */
class BookRepository {

    private val apiService = RetrofitClient.apiService

    //Searches for books based string provided by user
    suspend fun searchBooks(query: String): List<Book> {
        return try {
            val response: BookResponse = apiService.searchBooks(query)

            //  Process and sets the BookNetwork objects to local data models Book objects (Philipp Lackner,2021)
            response.results.mapNotNull { bookNetwork ->
                val downloadUrl = bookNetwork.getDownloadUrl()

                // Only include books that have a downloadable URL available.
                if (downloadUrl != null) {
                    Book(
                        id = bookNetwork.id,
                        title = bookNetwork.title,
                        authors = bookNetwork.authors.map { Author(it.name) },
                        formats = bookNetwork.formats
                    )
                } else {
                    null // Skip books without a download URL (Philipp Lackner,2021)
                }
            }
        } catch (e: Exception) {
           Log.e("BookRepository", "Error fetching books for query: $query", e)
            emptyList()
        }
    }
}
