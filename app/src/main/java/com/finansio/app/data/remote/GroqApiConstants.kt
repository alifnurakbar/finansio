package com.finansio.app.data.remote

/** Konstanta Groq API: alamat server, API key (dari GroqSecrets), dan nama model LLM. */
object GroqApiConstants {
    const val BASE_URL = "https://api.groq.com/"
    // Isi GROQ_API_KEY di local.properties (tidak ikut ter-commit). Key dibuat di console.groq.com
    // GroqSecrets.kt dibuat otomatis oleh Gradle saat sync/build.
    const val API_KEY: String = GroqSecrets.GROQ_API_KEY
    // llama-3.3-70b-versatile sudah dihentikan Groq (HTTP 404 model_not_found)
    const val MODEL = "openai/gpt-oss-120b"
    // Model vision untuk scan struk: membaca langsung dari foto, bukan hanya dari teks OCR.
    // Masih berstatus Preview di Groq. Jika dihentikan, scan otomatis kembali ke MODEL + teks OCR.
    const val VISION_MODEL = "qwen/qwen3.8-27b"
}
