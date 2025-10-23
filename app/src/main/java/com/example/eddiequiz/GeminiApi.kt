package com.example.eddiequiz

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class GeminiRequest(val prompt: String)
data class GeminiResponse(val content: String)

interface GeminiApi {
    @Headers("Content-Type: application/json")
    @POST("v1/generate")
    fun generateQuiz(@Body request: GeminiRequest): Call<GeminiResponse>
}