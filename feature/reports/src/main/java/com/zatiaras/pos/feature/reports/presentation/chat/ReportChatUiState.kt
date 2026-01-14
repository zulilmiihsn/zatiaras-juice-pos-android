package com.zatiaras.pos.feature.reports.presentation.chat

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false
)

data class ReportChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ChatEvent {
    data class SendMessage(val message: String) : ChatEvent
    data object ClearChat : ChatEvent
}
