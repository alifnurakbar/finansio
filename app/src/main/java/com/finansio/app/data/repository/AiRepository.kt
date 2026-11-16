package com.finansio.app.data.repository

import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.data.model.ReceiptParseResult
import com.finansio.app.data.remote.GroqApiService
import com.finansio.app.data.remote.GroqChatRequest
import com.finansio.app.data.remote.GroqMessage
import com.finansio.app.data.remote.GroqContentPart
import com.finansio.app.data.remote.GroqResponseFormat
import com.finansio.app.data.remote.GroqVisionMessage
import com.finansio.app.data.remote.GroqVisionRequest
import com.finansio.app.data.scanner.ReceiptJsonReader
import com.finansio.app.data.scanner.ReceiptPrompt
import com.finansio.app.data.scanner.ReceiptReconciler
import com.finansio.app.data.scanner.ReconciledReceipt
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository(private val apiService: GroqApiService) {
    private val gson = Gson()

    private companion object {
        const val TAG = "AiRepository"
        // Jumlah pesan terakhir yang ikut dikirim sebagai konteks percakapan
        const val MAX_CHAT_TURNS = 10
    }

    /**
     * Membaca struk belanja. Utamakan model vision (melihat foto struk + teks OCR per baris).
     * Bila model vision gagal (misalnya dihentikan Groq atau batas pemakaian habis), otomatis
     * kembali ke model teks dengan teks OCR per baris. Hasil AI selalu diperiksa ulang oleh
     * [ReceiptReconciler] supaya jumlah item cocok dengan TOTAL struk.
     */
    suspend fun parseReceipt(
        imageBase64: String?,
        ocrLayoutText: String,
        categories: List<CategoryEntity>
    ): Result<ReconciledReceipt> = withContext(Dispatchers.IO) {
        val expenseCategories = categories
            .filter { it.type == TransactionType.EXPENSE }
            .joinToString(", ") { it.name }

        var lastError: Exception? = null

        if (imageBase64 != null) {
            try {
                val request = GroqVisionRequest(
                    messages = listOf(
                        GroqVisionMessage(
                            role = "user",
                            content = listOf(
                                GroqContentPart.text(ReceiptPrompt.visionUserText(expenseCategories, ocrLayoutText)),
                                GroqContentPart.jpegBase64(imageBase64)
                            )
                        )
                    )
                )
                val content = apiService.createVisionCompletion(request).choices.firstOrNull()?.message?.content
                if (content != null) {
                    val receipt = ReceiptReconciler.reconcile(ReceiptJsonReader.read(content))
                    if (receipt.lines.isNotEmpty()) return@withContext Result.success(receipt)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Model vision gagal, beralih ke teks OCR", e)
                lastError = e
            }
        }

        if (ocrLayoutText.isBlank()) {
            return@withContext Result.failure(
                Exception(lastError?.let { friendlyAiError(it) } ?: "Tidak ada teks yang terbaca dari gambar.", lastError)
            )
        }

        try {
            val request = GroqChatRequest(
                messages = listOf(
                    GroqMessage(role = "system", content = ReceiptPrompt.instructions(expenseCategories)),
                    GroqMessage(role = "user", content = ReceiptPrompt.textOnlyUserText(ocrLayoutText))
                ),
                responseFormat = GroqResponseFormat(),
                temperature = 0.1
            )
            val content = apiService.createChatCompletion(request).choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("AI tidak memberikan jawaban. Coba scan ulang."))
            Result.success(ReceiptReconciler.reconcile(ReceiptJsonReader.read(content)))
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAiError(e), e))
        }
    }

    suspend fun parseVoiceText(
        voiceText: String,
        categories: List<CategoryEntity>
    ): Result<ReceiptParseResult> = withContext(Dispatchers.IO) {
        try {
            val incomeCategories = categories
                .filter { it.type == TransactionType.INCOME }
                .joinToString(", ") { it.name }
                
            val expenseCategories = categories
                .filter { it.type == TransactionType.EXPENSE }
                .joinToString(", ") { it.name }

            val systemPrompt = """
                Kamu adalah asisten keuangan pintar. Analisis ucapan pengguna berikut dan ekstrak informasi transaksi keuangan ke dalam format JSON.
                
                Kategori PEMASUKAN (INCOME) yang tersedia: [$incomeCategories]
                Kategori PENGELUARAN (EXPENSE) yang tersedia: [$expenseCategories]

                Aturan:
                1. Jika ucapan menyiratkan pengeluaran (contoh: "beli", "bayar", "jajan", "ongkos"), set suggested_type ke "EXPENSE".
                2. Jika ucapan menyiratkan pemasukan (contoh: "gaji", "dikasih", "terima"), set suggested_type ke "INCOME".
                3. Jika ada beberapa transaksi sekaligus (misal "beli kopi 15 ribu dan makan 25 ribu"), pisahkan menjadi beberapa objek di dalam array `items`.
                4. Field date isi dengan hari ini (format YYYY-MM-DD), kosongkan merchant_name.

                Berikan output JSON DENGAN EXACT KEY BERIKUT (jangan tambah key lain):
                {
                  "merchant_name": null,
                  "date": "YYYY-MM-DD",
                  "items": [
                    {
                      "name": "nama transaksi/item",
                      "price": angka_tanpa_titik_koma,
                      "quantity": jumlah (default 1),
                      "suggested_category": "nama kategori dari list di atas yang paling cocok (null jika tidak yakin)",
                      "suggested_type": "INCOME" atau "EXPENSE"
                    }
                  ],
                  "total_amount": angka_total_keseluruhan
                }
            """.trimIndent()

            val request = GroqChatRequest(
                messages = listOf(
                    GroqMessage(role = "system", content = systemPrompt),
                    GroqMessage(role = "user", content = "Ucapan:\n---\n$voiceText\n---")
                ),
                responseFormat = GroqResponseFormat()
            )

            val response = apiService.createChatCompletion(request)
            val jsonContent = response.choices.firstOrNull()?.message?.content
            
            if (jsonContent != null) {
                val parsedResult = gson.fromJson(jsonContent, ReceiptParseResult::class.java)
                Result.success(parsedResult)
            } else {
                Result.failure(Exception("Failed to get content from Groq API"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAiError(e), e))
        }
    }

    /**
     * Chat Asisten Keuangan. [history] berisi percakapan sebelumnya (role "user"/"assistant"),
     * [financeContext] berisi ringkasan data keuangan pengguna dari FinanceContextBuilder.
     */
    suspend fun chat(history: List<GroqMessage>, financeContext: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val systemPrompt = """
                    Kamu adalah "Finansio AI", asisten keuangan pribadi di aplikasi Finansio.
                    Jawab pertanyaan pengguna HANYA berdasarkan data keuangan di bawah ini.

                    Aturan:
                    - Bahasa Indonesia yang santai, hangat, dan mudah dipahami.
                    - Ringkas: maksimal sekitar 120 kata, kecuali pengguna minta penjelasan panjang.
                    - Sebut angka konkret dari data (format Rp dengan titik ribuan, misalnya Rp1.250.000).
                    - Boleh memakai daftar dengan tanda "- " dan **teks tebal** untuk poin penting.
                      Jangan memakai tabel, judul markdown (#), atau blok kode.
                    - Jika data tidak cukup untuk menjawab, katakan terus terang dan sarankan pengguna mencatat transaksinya.
                    - Jika pertanyaan di luar topik keuangan pribadi, tolak dengan sopan dan arahkan kembali ke keuangan.
                    - Jangan mengarang transaksi atau angka yang tidak ada di data.

                    DATA KEUANGAN PENGGUNA:
                    $financeContext
                """.trimIndent()

                val request = GroqChatRequest(
                    messages = listOf(GroqMessage(role = "system", content = systemPrompt)) +
                        history.takeLast(MAX_CHAT_TURNS),
                    responseFormat = null,
                    temperature = 0.5,
                    maxTokens = 2048
                )
                val content = apiService.createChatCompletion(request)
                    .choices.firstOrNull()?.message?.content?.trim()
                if (content.isNullOrBlank()) Result.failure(Exception("AI tidak memberikan jawaban. Coba tanyakan lagi."))
                else Result.success(content)
            } catch (e: Exception) {
                Result.failure(Exception(friendlyAiError(e), e))
            }
        }

    /** Saran singkat saat pengeluaran sebuah kategori mendekati/melewati anggarannya. */
    suspend fun generateBudgetAdvice(
        categoryName: String,
        spent: Long,
        limit: Long,
        percent: Int,
        daysLeft: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                Kamu adalah asisten keuangan pribadi yang ramah. Beri SATU saran singkat dan konkret
                dalam Bahasa Indonesia santai (maksimal 2 kalimat, maksimal 220 karakter) untuk
                pengguna yang pengeluaran kategorinya mendekati atau melewati anggaran.
                Sebutkan sisa uang per hari bila masih ada sisa. Tanpa markdown, tanpa emoji berlebihan.
            """.trimIndent()
            val userPrompt = """
                Kategori: $categoryName
                Terpakai: Rp$spent dari anggaran Rp$limit ($percent%)
                Sisa hari di bulan ini: $daysLeft
            """.trimIndent()

            val request = GroqChatRequest(
                messages = listOf(
                    GroqMessage(role = "system", content = systemPrompt),
                    GroqMessage(role = "user", content = userPrompt)
                ),
                responseFormat = null,
                temperature = 0.6,
                maxTokens = 1024
            )
            val content = apiService.createChatCompletion(request)
                .choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) Result.failure(Exception("Tidak ada respons dari AI"))
            else Result.success(content)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyAiError(e), e))
        }
    }

    suspend fun generateCashflowInsight(insightPrompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val systemPrompt = """
                    Kamu adalah analis keuangan personal yang cerdas dan ramah. Tugasmu adalah menganalisis data keuangan pengguna secara menyeluruh dan memberikan insight yang benar-benar berguna.
                    
                    Struktur analisismu HARUS mencakup semua aspek berikut dalam satu aliran teks yang natural (bukan bullet point):
                    1. Ringkasan kondisi keuangan bulan ini (income, expense, saldo)
                    2. Evaluasi kategori pengeluaran terbesar — apakah wajar atau perlu diperhatikan
                    3. Perbandingan dengan bulan lalu — apakah membaik atau memburuk
                    4. Evaluasi saving rate — apakah sehat (idealnya minimal 20% dari pemasukan)
                    5. Minimal 2 saran konkret dan spesifik yang bisa langsung diterapkan
                    
                    Aturan penulisan:
                    - Gunakan Bahasa Indonesia yang santai, hangat, dan mudah dipahami — seperti teman yang peduli keuanganmu
                    - Tulis dalam format paragraf teks biasa, BUKAN bullet list atau JSON
                    - Panjang: 5-8 kalimat yang padat dan informatif
                    - Jika ada data bulan lalu, bandingkan secara eksplisit dengan angka konkret
                    - Jika data kosong, sampaikan dengan ramah bahwa belum ada data dan ajak user untuk mulai mencatat
                    - Saran harus spesifik ke kategori pengeluaran yang ada di data, bukan saran generik
                """.trimIndent()

                val request = GroqChatRequest(
                    messages = listOf(
                        GroqMessage(role = "system", content = systemPrompt),
                        GroqMessage(role = "user", content = insightPrompt)
                    ),
                    responseFormat = null,
                    temperature = 0.7,
                    maxTokens = 4096
                )

                val response = apiService.createChatCompletion(request)
                val content = response.choices.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(Exception("Tidak ada respons dari AI"))

                Result.success(content.trim())
            } catch (e: Exception) {
                Result.failure(Exception(friendlyAiError(e), e))
            }
        }
}


/** Mengubah error teknis (HTTP, jaringan, JSON) menjadi pesan Bahasa Indonesia untuk pengguna. */
internal fun friendlyAiError(e: Throwable): String = when (e) {
    is HttpException -> when (e.code()) {
        400 -> "Permintaan ke AI tidak valid (HTTP 400). Coba lagi."
        401, 403 -> "API key Groq salah atau belum diisi (HTTP ${e.code()}). Periksa GROQ_API_KEY di local.properties."
        404 -> "Model AI tidak tersedia (HTTP 404). Perbarui aplikasi ke versi terbaru."
        413 -> "Teks terlalu panjang untuk diproses AI (HTTP 413)."
        429 -> "Batas pemakaian AI tercapai (HTTP 429). Tunggu sebentar lalu coba lagi."
        in 500..599 -> "Server AI sedang bermasalah (HTTP ${e.code()}). Coba lagi beberapa saat lagi."
        else -> "AI gagal merespons (HTTP ${e.code()})."
    }
    is SocketTimeoutException -> "AI terlalu lama merespons. Periksa koneksi internet lalu coba lagi."
    is IOException -> "Tidak bisa terhubung ke AI. Periksa koneksi internet Anda."
    is JsonSyntaxException -> "AI gagal membaca struk dengan benar. Coba foto ulang dengan lebih jelas."
    else -> e.message ?: "Terjadi kesalahan saat memproses dengan AI."
}
