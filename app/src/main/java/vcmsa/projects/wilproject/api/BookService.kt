package vcmsa.projects.wilproject.api

import vcmsa.projects.wilproject.models.BookResponse
import retrofit2.http.GET
import retrofit2.http.Query

//Retrofit  defining the endpoints for the Gutendex API (Philipp Lackner,2021)
interface BookService {
    @GET("books/")
    suspend fun searchBooks(
        @Query("search") query: String? = null
    ): BookResponse
}
