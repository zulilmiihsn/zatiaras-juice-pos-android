package com.zatiaras.pos.core.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== BRAND COLORS ====================
// Pink - Zatiaras Identity
val Brand50 = Color(0xFFFDF2F8)
val Brand100 = Color(0xFFFCE7F3)
val Brand200 = Color(0xFFFBCFE8)
val Brand300 = Color(0xFFF9A8D4)
val Brand400 = Color(0xFFF472B6)
val Brand500 = Color(0xFFEC4899) // Primary
val Brand600 = Color(0xFFDB2777)
val Brand700 = Color(0xFFBE185D)
val Brand800 = Color(0xFF9D174D)
val Brand900 = Color(0xFF831843)
val Brand950 = Color(0xFF500724) // Primary Dark

// Compatibility aliases for legacy naming still used in feature modules
val LightPink = Brand100
val SurfacePink = Brand50

// ==================== NEUTRAL COLORS ====================
// Slate - ShadCN Style
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)
val Slate950 = Color(0xFF020617)

// ==================== SEMANTIC COLORS ====================
val SuccessGreen = Color(0xFF10B981)
val SuccessGreenDark = Color(0xFF059669)
val SuccessGreenLight = Color(0xFF34D399)
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberDark = Color(0xFF92400E)
val WarningAmberBg = Color(0xFFFEF3C7)
val ErrorRed = Color(0xFFEF4444)
val ErrorRedDark = Color(0xFFDC2626)
val ErrorRedLight = Color(0xFFF87171)
val InfoBlue = Color(0xFF3B82F6)
val IndigoAccent = Color(0xFF6366F1)
val PurpleAccent = Color(0xFF8B5CF6)

// ==================== FINANCIAL COLORS ====================
val IncomeGreen = Color(0xFF10B981)
val ExpenseRed = Color(0xFFEF4444)
val PdfRed = Color(0xFFDC2626)
val ProfitGreen = Color(0xFF4CAF50)
val ProfitGreenLight = Color(0xFF8BC34A)
val ProfitGreenDark = Color(0xFF2E7D32)
val LossRed = Color(0xFFE53935)
val LossRedLight = Color(0xFFFF5252)
val TaxBlue = Color(0xFF2196F3)

// ==================== GRADIENT PRESETS ====================
/**
 * Pre-defined gradient color pairs used for stat cards and period cards.
 * Eliminates duplicate Color(0x...) declarations across composables.
 */
object GradientColors {
    val Revenue = listOf(Color(0xFF667eea), Color(0xFF764ba2))       // Purple
    val Transaction = listOf(Color(0xFF11998e), Color(0xFF38ef7d))   // Teal-Green
    val ProductSold = listOf(Color(0xFFf093fb), Color(0xFFf5576c))   // Pink
    val WeeklyPeriod = listOf(Color(0xFF4facfe), Color(0xFF00f2fe))  // Blue
    val MonthlyPeriod = listOf(Color(0xFFfa709a), Color(0xFFfee140)) // Orange-Pink
}

// ==================== ICON ACCENT COLORS ====================
/**
 * Consistent icon tint colors for settings/feature cards.
 */
object IconColors {
    val Printer = SuccessGreen
    val Settings = IndigoAccent
    val Store = Brand500
    val Preview = PurpleAccent
    val Logo = WarningAmber
    val Bluetooth = InfoBlue
}

// ==================== MEDAL / RANK COLORS ====================
object MedalColors {
    val Gold = Color(0xFFFFD700)
    val Silver = Color(0xFFC0C0C0)
    val Bronze = Color(0xFFCD7F32)
    val Star = Color(0xFFFFB800)
}

// ==================== RECEIPT PREVIEW COLORS ====================
object ReceiptColors {
    val TextBlack = Color(0xFF1A1A1A)
    val TextGray = Color(0xFF555555)
    val TextLightGray = Color(0xFF888888)
    val Divider = Color(0xFF444444)
    val PaperWhite = Color(0xFFFFFEFC)
    val PaperMid = Color(0xFFFFFDF8)
    val PaperEdge = Color(0xFFFFFCF0)
    val TornEdge = Color(0xFFEEEEEE)
}

// Legacy compatibility (kept for build stability)
val ZatiarasPink = Brand500
val Purple80 = Brand300
val PurpleGrey80 = Slate300
val Pink80 = ErrorRed
val Purple40 = Brand600
val PurpleGrey40 = Slate600
val Pink40 = ErrorRed
