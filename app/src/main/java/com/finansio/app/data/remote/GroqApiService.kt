package com.finansio.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/** Definisi endpoint Retrofit untuk Groq Chat Completions (format kompatibel OpenAI). */
interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: GroqChatRequest
    ): GroqChatResponse

    /** Endpoint yang sama, tetapi pesan user berisi teks dan gambar (untuk scan struk). */
    @POST("openai/v1/chat/completions")
    suspend fun createVisionChatCompletion(
        @Body request: GroqVisionChatRequest
    ): GroqChatResponse
}
