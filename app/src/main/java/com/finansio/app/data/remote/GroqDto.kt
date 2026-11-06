package com.finansio.app.data.remote

import com.google.gson.annotations.SerializedName

data class GroqChatRequest(
    @SerializedName("model") val model: String = GroqApiConstants.MODEL,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("response_format") val responseFormat: GroqResponseFormat? = null,
    @SerializedName("temperature") val temperature: Double? = 0.2,
    @SerializedName("max_tokens") val maxTokens: Int? = 1024
)

data class GroqMessage(
    @SerializedName("role") val role: String, // "system", "user", "assistant"
    @SerializedName("content") val content: String
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
