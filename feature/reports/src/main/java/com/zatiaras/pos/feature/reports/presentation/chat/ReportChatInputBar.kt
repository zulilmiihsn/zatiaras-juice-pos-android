package com.zatiaras.pos.feature.reports.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.feature.reports.R

private val SmartSuggestions = listOf(
    "Analisis Penjualan",
    "Produk Terlaris",
    "Analisis Laba Rugi",
    "Tips Operasional",
    "Pola Transaksi",
)

@Composable
internal fun ReportChatInputBar(
    uiState: ReportChatUiState,
    onSendMessage: (String) -> Unit,
    onSelectImage: (String?) -> Unit,
    onInputTextChanged: (String) -> Unit,
    onPickImage: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = AppShapes.XXL,
        modifier = Modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            AppShapes.XXL,
        ),
    ) {
        Column(
            modifier = Modifier.padding(bottom = if (android.os.Build.VERSION.SDK_INT >= 30) 0.dp else 8.dp),
        ) {
            SmartSuggestionRow(
                visible = !uiState.isLoading && uiState.inputText.isEmpty(),
                onSendMessage = onSendMessage,
            )
            SelectedImagePreview(
                imageUri = uiState.selectedImageUri,
                onRemove = { onSelectImage(null) },
            )
            MessageComposerRow(
                inputText = uiState.inputText,
                selectedImageUri = uiState.selectedImageUri,
                isLoading = uiState.isLoading,
                onInputTextChanged = onInputTextChanged,
                onSendMessage = onSendMessage,
                onPickImage = onPickImage,
            )
        }
    }
}

@Composable
private fun SmartSuggestionRow(
    visible: Boolean,
    onSendMessage: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            items(SmartSuggestions) { suggestion ->
                AssistChip(
                    onClick = { onSendMessage(suggestion) },
                    label = { Text(suggestion) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = AppShapes.L,
                    border = null,
                )
            }
        }
    }
}

@Composable
private fun SelectedImagePreview(
    imageUri: String?,
    onRemove: () -> Unit,
) {
    AnimatedVisibility(
        visible = imageUri != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .size(100.dp)
                .clip(AppShapes.L)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), AppShapes.L),
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .offset(x = (-4).dp, y = 4.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.chat_remove_image),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
