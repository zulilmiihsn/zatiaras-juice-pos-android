package com.zatiaras.pos.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design Tokens for consistent spacing, padding, and sizing across the app.
 * 
 * Usage:
 * ```
 * val dimensions = LocalDimensions.current
 * Modifier.padding(dimensions.paddingMedium)
 * ```
 * 
 * Hierarchy:
 * - XXS (4dp): Micro spacing, icon padding
 * - XS (8dp): Tight spacing, compact elements
 * - S (12dp): Secondary elements, compact cards
 * - M (16dp): Standard content padding
 * - L (20dp): Hero/Featured elements
 * - XL (24dp): Modal/Sheet content, auth screens
 * - XXL (32dp): Section dividers, bottom spacing
 */
@Immutable
data class Dimensions(
    // ===========================================
    // PADDING VALUES
    // ===========================================
    
    /** 4.dp - Micro padding for icons, badges */
    val paddingXXS: Dp = 4.dp,
    
    /** 8.dp - Tight padding for chips, compact elements */
    val paddingXS: Dp = 8.dp,
    
    /** 12.dp - Secondary padding for compact cards, list items */
    val paddingS: Dp = 12.dp,
    
    /** 16.dp - Standard content padding (most common) */
    val paddingM: Dp = 16.dp,
    
    /** 20.dp - Hero/Featured card padding */
    val paddingL: Dp = 20.dp,
    
    /** 24.dp - Modal/Sheet/Auth screen padding */
    val paddingXL: Dp = 24.dp,
    
    /** 32.dp - Large section dividers */
    val paddingXXL: Dp = 32.dp,
    
    // ===========================================
    // SPACING (between items)
    // ===========================================
    
    /** 4.dp - Micro spacing */
    val spacingXXS: Dp = 4.dp,
    
    /** 8.dp - Tight spacing for compact lists */
    val spacingXS: Dp = 8.dp,
    
    /** 12.dp - Standard item spacing in lists */
    val spacingS: Dp = 12.dp,
    
    /** 16.dp - Section spacing */
    val spacingM: Dp = 16.dp,
    
    /** 24.dp - Large section divider */
    val spacingL: Dp = 24.dp,
    
    // ===========================================
    // ICON SIZES
    // ===========================================
    
    /** 16.dp - Small inline icons */
    val iconSizeXS: Dp = 16.dp,
    
    /** 18.dp - Button icons */
    val iconSizeS: Dp = 18.dp,
    
    /** 20.dp - List item icons */
    val iconSizeM: Dp = 20.dp,
    
    /** 24.dp - Standard icons (default) */
    val iconSizeL: Dp = 24.dp,
    
    /** 48.dp - Large icons (avatars, empty states) */
    val iconSizeXL: Dp = 48.dp,
    
    /** 72.dp - Hero icons (empty state illustrations) */
    val iconSizeXXL: Dp = 72.dp,
    
    /** 80.dp - Extra large icons */
    val iconSizeHero: Dp = 80.dp,
    
    // ===========================================
    // CORNER RADIUS
    // ===========================================
    
    /** 4.dp - Tiny radius for progress bars, small indicators */
    val radiusXS: Dp = 4.dp,
    
    /** 8.dp - Small radius for thumbnails, avatars */
    val radiusS: Dp = 8.dp,
    
    /** 12.dp - Medium radius for buttons, text fields, small cards */
    val radiusM: Dp = 12.dp,
    
    /** 16.dp - Large radius for major cards, containers */
    val radiusL: Dp = 16.dp,
    
    /** 20.dp - Extra large radius for chips, pills */
    val radiusXL: Dp = 20.dp,
    
    // ===========================================
    // COMPONENT HEIGHTS
    // ===========================================
    
    /** 48.dp - Standard button height */
    val buttonHeight: Dp = 48.dp,
    
    /** 56.dp - Large button height */
    val buttonHeightLarge: Dp = 56.dp,
    
    /** 120.dp - Text area minimum height */
    val textAreaMinHeight: Dp = 120.dp,
    
    /** 200.dp - Card minimum height for loading states */
    val cardMinHeight: Dp = 200.dp,
    
    // ===========================================
    // SIDEBAR/PANEL WIDTHS
    // ===========================================
    
    /** 320.dp - Cart sidebar width */
    val sidebarWidth: Dp = 320.dp,
    
    /** 150.dp - Minimum grid cell size */
    val gridCellMinSize: Dp = 150.dp
)

/**
 * Pre-defined RoundedCornerShape values for consistency.
 */
object AppShapes {
    /** 4.dp - Tiny radius for progress bars */
    val XS = RoundedCornerShape(4.dp)
    
    /** 8.dp - Small radius for thumbnails */
    val S = RoundedCornerShape(8.dp)
    
    /** 12.dp - Medium radius for buttons, text fields */
    val M = RoundedCornerShape(12.dp)
    
    /** 16.dp - Large radius for cards */
    val L = RoundedCornerShape(16.dp)
    
    /** 20.dp - Extra large radius for chips */
    val XL = RoundedCornerShape(20.dp)

    /** 24.dp - XXL radius for dialogs/sheets */
    val XXL = RoundedCornerShape(24.dp)
    
    /** 50% - Fully rounded (circles, pills) */
    val Full = RoundedCornerShape(percent = 50)
    
    /** Cart sidebar shape - flat rectangle */
    val CartSidebar = RoundedCornerShape(0.dp)
    
    /** Top rounded for image covers */
    val TopRounded = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    /** Left panel rounded on start side only */
    val StartPanel = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 0.dp,
        bottomStart = 24.dp,
        bottomEnd = 0.dp
    )

    /** Bottom panel rounded on top side only */
    val TopPanel = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    /** Corner notch used on overlays/badges */
    val BottomStartNotch = RoundedCornerShape(bottomStart = 16.dp)
}

/**
 * CompositionLocal for accessing Dimensions throughout the app.
 */
val LocalDimensions = staticCompositionLocalOf { Dimensions() }
