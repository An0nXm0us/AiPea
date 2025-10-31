package vcmsa.projects.wilproject.api
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// API key for authenticating requests to the Gemini API.
private const val API_KEY = "AIzaSyCV-C5l4y8z6389PZDlVBjzMJPJJbWw9Wk"
// Base URL for the Gemini.
private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

/**
 * Object for handles and provides the Retrofit client for the Gemini API (Philipp Lackner,2021 & GeeksforGeeks (2020) .
 */
object RetrofitClientQuiz {


    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()

        val newUrl = original.url.newBuilder()
            .addQueryParameter("key", API_KEY)
            .build()

        val newRequest = original.newBuilder()
            .url(newUrl)
            .build()

        chain.proceed(newRequest)
    }

    // Configures OkHttpClient with the authentication making last as long as 60 seconds.
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Configures the Retrofit instance for the Gemini API.
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    //return instance of the service class
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}
