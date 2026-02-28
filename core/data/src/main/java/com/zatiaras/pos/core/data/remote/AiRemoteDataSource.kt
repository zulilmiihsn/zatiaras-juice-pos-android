package com.zatiaras.pos.core.data.remote

import io.github.jan.supabase.functions.Functions
import io.ktor.client.call.body
import io.ktor.http.Headers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified AI data source that routes requests through Supabase Edge Function (BFF pattern).
 * API keys are stored server-side as Supabase Secrets — never embedded in the client.
 *
 * Replaces the old OpenRouterRemoteDataSource and GroqRemoteDataSource
 * which had hardcoded API keys.
 */
@Singleton
class AiRemoteDataSource @Inject constructor(
    private val functions: Functions,
    private val json: Json
) {

    /**
     * Send a chat completion request through the BFF Edge Function.
     *
     * @param provider "openrouter" or "groq"
     * @param messages List of chat messages (system, user, assistant)
     * @param model Model identifier (e.g. "qwen/qwen3-vl-30b-a3b-thinking")
     * @param temperature Sampling temperature (default 0.7)
     * @param maxTokens Maximum tokens in response (default 2048)
     */
    suspend fun getChatCompletion(
        provider: String = "openrouter",
        messages: List<OpenRouterMessage>,
        model: String = "qwen/qwen3-vl-30b-a3b-thinking",
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): Result<String> {
        return try {
            val request = AiEdgeFunctionRequest(
                provider = provider,
                model = model,
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens
            )

            val requestBody = json.encodeToString(
                AiEdgeFunctionRequest.serializer(),
                request
            )

            Timber.d("Invoking ai-chat Edge Function (provider=$provider, model=$model)")

            val response = functions.invoke(
                function = "ai-chat",
                body = requestBody,
                headers = Headers.build {
                    append("Content-Type", "application/json")
                }
            )

            val responseBody = response.body<String>()
            val chatResponse = json.decodeFromString(
                OpenRouterChatResponse.serializer(),
                responseBody
            )

            val content = chatResponse.choices.firstOrNull()?.message?.content
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("No response from AI provider ($provider)"))
            }
        } catch (e: Exception) {
            Timber.e(e, "AI chat request failed (provider=$provider)")
            Result.failure(e)
        }
    }
}

/**
 * Request body sent to the ai-chat Edge Function.
 */
@Serializable
data class AiEdgeFunctionRequest(
    val provider: String,
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 2048
)
