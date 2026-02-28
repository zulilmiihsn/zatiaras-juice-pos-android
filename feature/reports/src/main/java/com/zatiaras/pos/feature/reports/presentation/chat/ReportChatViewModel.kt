package com.zatiaras.pos.feature.reports.presentation.chat

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.feature.reports.domain.usecase.AnalyzeReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ReportChatViewModel @Inject constructor(
    private val analyzeReportUseCase: AnalyzeReportUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportChatUiState(
            messages = listOf(
                ChatMessage(
                    content = "Halo! Saya asisten AI Zatiaras. Anda bisa bertanya tentang performa penjualan, tren produk, atau analisis laba rugi.",
                    isUser = false
                )
            )
        )
    )
    val uiState: StateFlow<ReportChatUiState> = _uiState.asStateFlow()

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SendMessage -> sendMessage(event.message)
            is ChatEvent.InputTextChanged -> _uiState.update { it.copy(inputText = event.text) }
            is ChatEvent.SelectImage -> _uiState.update { it.copy(selectedImageUri = event.uri) }
            is ChatEvent.ClearChat -> _uiState.update {
                it.copy(
                    messages = listOf(
                        ChatMessage(
                            content = "Halo! Saya asisten AI Zatiaras. Anda bisa bertanya tentang performa penjualan, tren produk, atau analisis laba rugi.",
                            isUser = false
                        )
                    ),
                    inputText = ""
                )
            }
        }
    }

    private fun sendMessage(content: String) {
        if (content.isBlank() && _uiState.value.selectedImageUri == null) return

        val imageUri = _uiState.value.selectedImageUri

        // Add user message
        val userMsg = ChatMessage(
            content = content,
            isUser = true,
            imageUrl = imageUri
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true,
                error = null,
                selectedImageUri = null,
                inputText = ""
            )
        }

        viewModelScope.launch {
            // Encode image if present
            val imageBase64 = imageUri?.let { encodeImageToBase64(it) }

            // Call use case with history (exclude the message we just added)
            analyzeReportUseCase(
                query = content,
                history = _uiState.value.messages.dropLast(1),
                imageBase64 = imageBase64
            ).fold(
                onSuccess = { aiResponse ->
                    val aiMsg = ChatMessage(content = aiResponse, isUser = false)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + aiMsg,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to get AI response")
                    val errorMsg = ChatMessage(
                        content = "Maaf, saya sedang mengalami kendala teknis (${error.message}). Silakan coba lagi nanti.",
                        isUser = false
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMsg,
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    private fun encodeImageToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to encode image")
            null
        }
    }
}
