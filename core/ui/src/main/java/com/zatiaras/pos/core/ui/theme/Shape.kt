package com.zatiaras.pos.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), // Tags/Badges
    small = RoundedCornerShape(6.dp),      // Cards/Inputs
    medium = RoundedCornerShape(8.dp),     // Buttons/Dialogs
    large = RoundedCornerShape(12.dp),     // Modal Sheets / Large Cards
    extraLarge = RoundedCornerShape(16.dp) // Drawer / Full Screen Cards
)
