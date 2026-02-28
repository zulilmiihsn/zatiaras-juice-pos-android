package com.zatiaras.pos.core.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A wrapper class for strings that can be either hardcoded [DynamicString]
 * or a string resource [StringResource] with optional arguments.
 * 
 * This allows ViewModels to provide localized strings without having a 
 * reference to the Android Context, maintaining Clean Architecture boundaries.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    /**
     * Resolve the string in a Composable function.
     */
    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }
}
