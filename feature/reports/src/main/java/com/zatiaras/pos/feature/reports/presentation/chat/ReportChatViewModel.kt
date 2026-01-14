package com.zatiaras.pos.feature.reports.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportChatViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ReportChatUiState())
    val uiState: StateFlow<ReportChatUiState> = _uiState.asStateFlow()

    init {
        // Add initial greeting
        _uiState.update { 
            it.copy(
                messages = listOf(
                    ChatMessage(
                        content = "Halo! Saya asisten AI Zatiaras. Anda bisa bertanya tentang performa penjualan, tren produk, atau analisis laba rugi.",
                        isUser = false
                    )
                )
            )
        }
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SendMessage -> sendMessage(event.message)
            is ChatEvent.ClearChat -> _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    private fun sendMessage(content: String) {
        if (content.isBlank()) return

        // Add user message
        val userMsg = ChatMessage(content = content, isUser = true)
        
        _uiState.update { 
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true
            )
        }

        // Simulate AI Response (Mock for now)
        viewModelScope.launch {
            delay(1500) // Simulate network/thinking delay
            
            val aiResponse = generateMockResponse(content)
            val aiMsg = ChatMessage(content = aiResponse, isUser = false)
            
            _uiState.update {
                it.copy(
                    messages = it.messages + aiMsg,
                    isLoading = false
                )
            }
        }
    }

    private fun generateMockResponse(query: String): String {
        return when {
            query.contains("laba", ignoreCase = true) -> 
                "Berdasarkan data hari ini, laba bersih Anda adalah Rp 1.500.000. Ini meningkat 15% dari kemarin."
            query.contains("terlaris", ignoreCase = true) -> 
                "Produk terlaris minggu ini adalah 'Kopi Susu Gula Aren' dengan 120 terjual, diikuti oleh 'Croissant' (85 terjual)."
            query.contains("total", ignoreCase = true) -> 
                "Total pendapatan bulan ini mencapai Rp 45.000.000. Anda sudah mencapai 85% dari target bulanan."
            else -> 
                "Saya mengerti Anda bertanya tentang '$query'. Untuk saat ini saya hanya bisa menjawab pertanyaan dasar tentang penjualan dan produk."
        }
    }
}
