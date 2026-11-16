package com.finansio.app.data.remote

/** Konstanta Groq API: alamat server, API key (dari GroqSecrets), dan nama model LLM. */
object GroqApiConstants {
    const val BASE_URL = "https://api.groq.com/"
    // Isi GROQ_API_KEY di local.properties (tidak ikut ter-commit). Key dibuat di console.groq.com
    // GroqSecrets.kt dibuat otomatis oleh Gradle saat sync/build.
    const val API_KEY: String = GroqSecrets.GROQ_API_KEY
    // llama-3.3-70b-versatile sudah dihentikan Groq (HTTP 404 model_not_found)
    const val MODEL = "openai/gpt-oss-120b"
    // Model yang bisa membaca foto struk langsung (teks + gambar). Statusnya Preview di Groq,
    // jadi bila gagal/dihentikan, scan struk kembali memakai MODEL (hanya teks OCR). Lihat ADR-012.
    const val VISION_MODEL = "qwen/qwen3.8-27b"
}
