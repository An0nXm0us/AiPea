package vcmsa.projects.wilproject.api

import vcmsa.projects.wilproject.models.BookResponse
import android.util.Log
import vcmsa.projects.wilproject.models.Author
import vcmsa.projects.wilproject.models.Book


class BookRepository {

    private val apiService = RetrofitClient.apiService
    suspend fun searchBooks(query: String): List<Book> {
        return try {
            val response: BookResponse = apiService.searchBooks(query)

            response.results.mapNotNull { bookNetwork ->
                val downloadUrl = bookNetwork.getDownloadUrl()

               if (downloadUrl != null) {
                    Book(
                        id = bookNetwork.id,
                        title = bookNetwork.title,
                        authors = bookNetwork.authors.map { Author(it.name) },
                        formats = bookNetwork.formats
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error fetching books for query: $query", e)
            emptyList()
        }
    }
}
