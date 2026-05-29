package com.zatiaras.pos.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material shape scale mapped to the app's component conventions.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), // Tags and badges.
    small = RoundedCornerShape(6.dp), // Compact cards and text inputs.
    medium = RoundedCornerShape(8.dp), // Buttons and dialogs.
    large = RoundedCornerShape(12.dp), // Modal sheets and large cards.
    extraLarge = RoundedCornerShape(16.dp), // Drawers and full-screen cards.
)
