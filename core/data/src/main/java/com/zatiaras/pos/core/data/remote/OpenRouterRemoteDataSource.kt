package com.zatiaras.pos.core.data.remote

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @deprecated Use [AiRemoteDataSource] instead, which routes requests through
 * the Supabase Edge Function (BFF pattern) with server-side API keys.
 * This class previously contained a hardcoded API key — a critical security issue.
 */
@Deprecated(
    message = "Use AiRemoteDataSource instead. This class had hardcoded API keys.",
    replaceWith = ReplaceWith("AiRemoteDataSource")
)
@Singleton
class OpenRouterRemoteDataSource @Inject constructor(
    private val aiRemoteDataSource: AiRemoteDataSource
) {
    suspend fun getChatCompletion(
        messages: List<OpenRouterMessage>,
        model: String = "qwen/qwen3-vl-30b-a3b-thinking"
    ): Result<String> {
        Timber.w("OpenRouterRemoteDataSource is deprecated — delegating to AiRemoteDataSource")
        return aiRemoteDataSource.getChatCompletion(
            provider = "openrouter",
            messages = messages,
            model = model
        )
    }
}
