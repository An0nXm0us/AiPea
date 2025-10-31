package vcmsa.projects.wilproject.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Base URL for the Gutendex book API.
private const val BASE_URL = "https://gutendex.com/"


object RetrofitClient {

    // Configures OkHttpClient with long timeouts for potentially slow connections by 60 seconds
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    val apiService: BookService by lazy {
        retrofit.create(BookService::class.java)
    }
}
