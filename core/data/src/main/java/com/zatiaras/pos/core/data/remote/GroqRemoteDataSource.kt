package com.zatiaras.pos.core.data.remote

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @deprecated Use [AiRemoteDataSource] instead, which routes requests through
 * the Supabase Edge Function (BFF pattern) with server-side API keys.
 * This class previously contained a hardcoded API key and was never used.
 */
@Deprecated(
    message = "Use AiRemoteDataSource instead. This class had hardcoded API keys and was unused.",
    replaceWith = ReplaceWith("AiRemoteDataSource")
)
@Singleton
class GroqRemoteDataSource @Inject constructor(
    private val aiRemoteDataSource: AiRemoteDataSource
) {
    suspend fun getChatCompletion(
        messages: List<GroqMessage>,
        model: String = "llama-3.3-70b-versatile"
    ): Result<String> {
        Timber.w("GroqRemoteDataSource is deprecated — delegating to AiRemoteDataSource")
        // Convert GroqMessage to OpenRouterMessage for the unified interface
        val openRouterMessages = messages.map { msg ->
            OpenRouterMessage(
                role = msg.role,
                content = kotlinx.serialization.json.JsonPrimitive(msg.content)
            )
        }
        return aiRemoteDataSource.getChatCompletion(
            provider = "groq",
            messages = openRouterMessages,
            model = model
        )
    }
}
