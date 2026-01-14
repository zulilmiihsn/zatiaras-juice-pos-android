package com.zatiaras.pos.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape

/**
 * Scale factor when button is pressed.
 * Keep subtle (0.95-0.98) for professional feel.
 */
private const val PRESSED_SCALE = 0.96f
private const val DEFAULT_SCALE = 1f
private const val ANIMATION_DURATION_MS = 100

/**
 * Animated Button with subtle scale animation on press.
 * Provides tactile feedback for better user experience.
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else DEFAULT_SCALE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "button_scale"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Animated Elevated Button with subtle scale animation on press.
 */
@Composable
fun AnimatedElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.elevatedShape,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else DEFAULT_SCALE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "elevated_button_scale"
    )
    
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Animated Outlined Button with subtle scale animation on press.
 */
@Composable
fun AnimatedOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else DEFAULT_SCALE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "outlined_button_scale"
    )
    
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Animated Tonal Button with subtle scale animation on press.
 */
@Composable
fun AnimatedTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else DEFAULT_SCALE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "tonal_button_scale"
    )
    
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Animated Text Button with subtle scale animation on press.
 */
@Composable
fun AnimatedTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else DEFAULT_SCALE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "text_button_scale"
    )
    
    TextButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}
