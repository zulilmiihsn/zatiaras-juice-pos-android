package com.zatiaras.pos.feature.reports.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.feature.reports.R
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Image

@Composable
internal fun MessageComposerRow(
    inputText: String,
    selectedImageUri: String?,
    isLoading: Boolean,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onPickImage: () -> Unit,
) {
    val sendEnabled = (inputText.isNotBlank() || selectedImageUri != null) && !isLoading

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AttachImageButton(onClick = onPickImage)
        Spacer(modifier = Modifier.width(12.dp))
        ChatTextField(
            inputText = inputText,
            selectedImageUri = selectedImageUri,
            onInputTextChanged = onInputTextChanged,
            onSendMessage = onSendMessage,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        SendButton(
            sendEnabled = sendEnabled,
            onClick = { onSendMessage(inputText) },
        )
    }
}

@Composable
private fun AttachImageButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier.size(44.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = EvaIcons.Outline.Image,
                contentDescription = stringResource(R.string.chat_attach_image),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ChatTextField(
    inputText: String,
    selectedImageUri: String?,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = inputText,
        onValueChange = onInputTextChanged,
        modifier = modifier,
        placeholder = {
            Text(
                stringResource(R.string.chat_input_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        maxLines = 4,
        shape = AppShapes.XXL,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
            onSend = {
                if (inputText.isNotBlank() || selectedImageUri != null) {
                    onSendMessage(inputText)
                }
            },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            unfocusedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SendButton(
    sendEnabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = {
            if (sendEnabled) {
                onClick()
            }
        },
        enabled = sendEnabled,
        modifier = Modifier
            .size(48.dp)
            .background(
                brush = if (sendEnabled) {
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    )
                } else {
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant),
                    )
                },
                shape = CircleShape,
            ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(R.string.chat_send),
            tint = if (sendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
