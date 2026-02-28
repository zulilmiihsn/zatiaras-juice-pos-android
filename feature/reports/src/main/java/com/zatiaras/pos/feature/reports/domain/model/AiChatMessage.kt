package com.zatiaras.pos.feature.reports.domain.model

/**
 * Domain model for AI chat messages used in the chat repository.
 * Decouples domain logic from OpenRouter-specific DTOs.
 */
data class AiChatMessage(
    val role: String,           // "system", "user", "assistant"
    val content: String,
    val imageBase64: String? = null  // Optional base64-encoded image
)
