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
 * Request untuk model vision: isi pesan berupa daftar bagian (teks dan gambar base64),
 * sesuai format Chat Completions yang kompatibel OpenAI.
 */
data class GroqVisionRequest(
    @SerializedName("model") val model: String = GroqApiConstants.VISION_MODEL,
    @SerializedName("messages") val messages: List<GroqVisionMessage>,
    @SerializedName("response_format") val responseFormat: GroqResponseFormat? = GroqResponseFormat(),
    @SerializedName("temperature") val temperature: Double? = 0.1,
    @SerializedName("max_tokens") val maxTokens: Int? = 4096
)

data class GroqVisionMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: List<GroqContentPart>
)

data class GroqContentPart(
    @SerializedName("type") val type: String, // "text" atau "image_url"
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: GroqImageUrl? = null
) {
    companion object {
        fun text(value: String) = GroqContentPart(type = "text", text = value)
        fun jpegBase64(base64: String) =
            GroqContentPart(type = "image_url", imageUrl = GroqImageUrl("data:image/jpeg;base64,$base64"))
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
