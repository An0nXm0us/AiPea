package vcmsa.projects.wilproject.api

import vcmsa.projects.wilproject.models.BookResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BookService {
    @GET("books/")
    suspend fun searchBooks(
        @Query("search") query: String? = null
    ): BookResponse
}