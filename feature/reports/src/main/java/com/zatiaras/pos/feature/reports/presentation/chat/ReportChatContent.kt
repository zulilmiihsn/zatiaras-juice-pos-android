package com.zatiaras.pos.feature.reports.presentation.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.LocalDimensions

@Composable
internal fun ReportChatContent(
    uiState: ReportChatUiState,
    paddingValues: PaddingValues,
    onSendMessage: (String) -> Unit,
    onSelectImage: (String?) -> Unit,
    onInputTextChanged: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: android.net.Uri? ->
        onSelectImage(uri?.toString())
    }

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .imePadding(),
    ) {
        ChatMessageList(
            uiState = uiState,
            listState = listState,
            modifier = Modifier.weight(1f),
        )
        ReportChatInputBar(
            uiState = uiState,
            onSendMessage = onSendMessage,
            onSelectImage = onSelectImage,
            onInputTextChanged = onInputTextChanged,
            onPickImage = { imagePicker.launch("image/*") },
        )
    }
}

@Composable
private fun ChatMessageList(
    uiState: ReportChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalDimensions.current

    Box(modifier = modifier) {
        ChatBackgroundAccent()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimensions.paddingM,
                end = dimensions.paddingM,
                top = dimensions.paddingM,
                bottom = 120.dp,
            ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(dimensions.spacingM),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                AnimatedChatMessage(message)
            }

            if (uiState.isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }
    }
}

@Composable
private fun ChatBackgroundAccent() {
    Box(
        modifier = Modifier
            .offset(x = (-50).dp, y = 100.dp)
            .size(300.dp)
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
