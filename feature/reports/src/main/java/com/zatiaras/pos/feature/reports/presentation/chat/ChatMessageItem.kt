package com.zatiaras.pos.feature.reports.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.feature.reports.R

@Composable
internal fun AnimatedChatMessage(message: ChatMessage) {
    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally(
            initialOffsetX = { if (message.isUser) it else -it },
        ) + fadeIn(),
    ) {
        ChatMessageItem(message)
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            AiAvatar(isThinking = message.isThinking)
            Spacer(modifier = Modifier.width(10.dp))
        }

        MessageBubble(
            message = message,
            isUser = isUser,
        )

        if (isUser) {
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
private fun AiAvatar(isThinking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Surface(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                if (isThinking) {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = stringResource(R.string.chat_ai_avatar),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun RowScope.MessageBubble(
    message: ChatMessage,
    isUser: Boolean,
) {
    Surface(
        shape = AppShapes.XL,
        color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .weight(weight = 1f, fill = false)
            .background(
                brush = messageBubbleBrush(isUser),
                shape = AppShapes.XL,
            )
            .then(
                if (!isUser) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        AppShapes.XL,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier.background(
                brush = if (isUser) {
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    )
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                },
            ),
        ) {
            Column {
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = stringResource(R.string.chat_message_image),
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(AppShapes.M)
                            .fillMaxWidth(0.85f)
                            .heightIn(max = 260.dp),
                        contentScale = ContentScale.Crop,
                    )
                }

                if (message.content.isNotEmpty()) {
                    FormattedMessage(
                        content = message.content,
                        isUser = isUser,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun messageBubbleBrush(isUser: Boolean): Brush = if (isUser) {
    Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    )
} else {
    Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    )
}
