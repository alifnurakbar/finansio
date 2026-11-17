package com.finansio.app.data.remote

import com.google.gson.annotations.SerializedName

/** Model data request/response Groq Chat Completions yang dikirim dan diterima dalam format JSON. */
data class GroqChatRequest(
    @SerializedName("model") val model: String = GroqApiConstants.MODEL,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("response_format") val responseFormat: GroqResponseFormat? = null,
    @SerializedName("temperature") val temperature: Double? = 0.2,
    // gpt-oss adalah reasoning model: token "berpikir" ikut dihitung, jadi beri ruang lebih
    @SerializedName("max_tokens") val maxTokens: Int? = 4096
)

data class GroqMessage(
    @SerializedName("role") val role: String, // "system", "user", "assistant"
    @SerializedName("content") val content: String
)

/**
 * Request dengan gambar (model vision). Isi pesan user berupa daftar bagian: teks dan gambar.
 * Pesan system tetap berupa teks biasa ([GroqMessage]), jadi [messages] berisi dua jenis pesan.
 */
data class GroqVisionChatRequest(
    @SerializedName("model") val model: String = GroqApiConstants.VISION_MODEL,
    @SerializedName("messages") val messages: List<Any>,
    @SerializedName("response_format") val responseFormat: GroqResponseFormat? = GroqResponseFormat(),
    @SerializedName("temperature") val temperature: Double? = 0.1,
    // Batas kuota gratis Groq untuk model vision: 1000 token output per menit. Dengan max_tokens besar,
    // Groq menolak permintaan (HTTP 429 "Request too large ... OTPM") sebelum diproses.
    // 900 token cukup untuk sekitar 15 item JSON ringkas; struk lebih panjang jatuh ke model teks.
    @SerializedName("max_tokens") val maxTokens: Int? = 900
)

data class GroqVisionMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: List<GroqContentPart>
)

/** Satu bagian isi pesan: `type` "text" dengan [text], atau "image_url" dengan [imageUrl]. */
data class GroqContentPart(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: GroqImageUrl? = null
) {
    companion object {
        fun text(text: String) = GroqContentPart(type = "text", text = text)
        fun jpeg(base64: String) = GroqContentPart(
            type = "image_url",
            imageUrl = GroqImageUrl("data:image/jpeg;base64,$base64")
        )
    }
}

data class GroqImageUrl(
    @SerializedName("url") val url: String
)

data class GroqResponseFormat(
    @SerializedName("type") val type: String = "json_object"
)

data class GroqChatResponse(
    @SerializedName("id") val id: String,
    @SerializedName("object") val objectType: String,
    @SerializedName("created") val created: Long,
    @SerializedName("model") val model: String,
    @SerializedName("choices") val choices: List<GroqChoice>,
    @SerializedName("usage") val usage: GroqUsage?
)

data class GroqChoice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: GroqMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class GroqUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)
