package vcmsa.projects.wilproject.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
//(GeeksforGeeks, 2020)
data class RawText(val text: String)

data class MessageContent(val parts: List<RawText>)


data class GeminiRequest(val contents: List<MessageContent>)
data class Candidate(val content: MessageContent)

data class GenerateContentResults(
    val candidates: List<Candidate>
)

interface GeminiApi {
    @Headers("Content-Type: application/json")
    //Makes the ai model generate whats being prompted

    @POST("models/gemini-2.5-flash:generateContent")
    fun generateQuiz(@Body request: GeminiRequest): Call<GenerateContentResults>

}
