package com.finansio.app.data.remote

import com.finansio.app.BuildConfig

object GroqApiConstants {
    const val BASE_URL = "https://api.groq.com/"
    // Isi GROQ_API_KEY di local.properties (tidak ikut ter-commit). Key dibuat di console.groq.com
    val API_KEY: String = BuildConfig.GROQ_API_KEY
    const val MODEL = "llama-3.3-70b-versatile"
}
