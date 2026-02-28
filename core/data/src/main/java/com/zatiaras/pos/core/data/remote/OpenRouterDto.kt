package com.zatiaras.pos.core.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
    val stream: Boolean = false
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: JsonElement // Using JsonElement to support both String and List<ContentPart>
)

@Serializable
data class OpenRouterContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: OpenRouterImageUrl? = null
)

@Serializable
data class OpenRouterImageUrl(
    val url: String // Can be data:image/jpeg;base64,...
)

@Serializable
data class OpenRouterChatResponse(
    val id: String,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage? = null
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterResultMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class OpenRouterResultMessage(
    val role: String,
    val content: String? = null
)

@Serializable
data class OpenRouterUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)
