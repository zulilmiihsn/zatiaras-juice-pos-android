package com.zatiaras.pos.feature.reports.domain.repository

import com.zatiaras.pos.feature.reports.domain.model.AiChatMessage

/**
 * Repository interface for AI chat operations.
 *
 * Abstracts the AI provider (OpenRouter, Groq, etc.) behind a clean domain boundary.
 * The implementation routes requests through a BFF Edge Function so API keys
 * never leave the server side.
 */
interface AiChatRepository {

    /**
     * Send a chat completion request to the AI provider.
     *
     * @param messages Ordered list of chat messages (system, user, assistant turns)
     * @param model Target model identifier, defaults to the primary OpenRouter model
     * @param provider AI provider to route through ("openrouter" or "groq")
     * @return The assistant's reply text or a failure
     */
    suspend fun sendChatMessage(
        messages: List<AiChatMessage>,
        model: String = "qwen/qwen3-vl-30b-a3b-thinking",
        provider: String = "openrouter"
    ): Result<String>
}
