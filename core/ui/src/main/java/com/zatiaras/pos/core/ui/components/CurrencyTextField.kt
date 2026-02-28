package com.zatiaras.pos.core.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.zatiaras.pos.core.ui.util.CurrencyFormatter

/**
 * A specialized text field for currency input that automatically formats
 * the input value with Indonesian Rupiah formatting (thousands separator).
 * 
 * As the user types "16000", it displays as "16.000" in the field.
 * 
 * @param value The current numeric value (unformatted)
 * @param onValueChange Callback when the numeric value changes
 * @param modifier Modifier for the text field
 * @param label Optional label for the text field
 * @param placeholder Optional placeholder text
 * @param enabled Whether the field is enabled
 * @param isError Whether to show error state
 * @param showPrefix Whether to show "Rp " prefix
 * @param shape Shape of the text field
 * @param colors Text field colors
 * @param keyboardActions Keyboard actions
 * @param imeAction IME action for the keyboard
 * @param supportingText Optional supporting text to be displayed below the text field
 */
@Composable
fun CurrencyTextField(
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    showPrefix: Boolean = true,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: ImeAction = ImeAction.Done,
    singleLine: Boolean = true
) {
    // Store the displayed text (formatted)
    var displayText by remember(value) {
        mutableStateOf(
            if (value > 0) CurrencyFormatter.formatNumber(value) else ""
        )
    }
    
    OutlinedTextField(
        value = displayText,
        onValueChange = { newValue ->
            // Extract only digits
            val digitsOnly = newValue.filter { it.isDigit() }
            
            if (digitsOnly.isEmpty()) {
                displayText = ""
                onValueChange(0L)
            } else {
                val numericValue = digitsOnly.toLongOrNull() ?: 0L
                // Format and display
                displayText = CurrencyFormatter.formatNumber(numericValue)
                onValueChange(numericValue)
            }
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        enabled = enabled,
        isError = isError,
        prefix = if (showPrefix) {{ Text("Rp ") }} else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        shape = shape,
        colors = colors
    )
}

/**
 * A variant that accepts String value for backward compatibility.
 * Internally converts string to Long and back.
 */
@Composable
fun CurrencyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    showPrefix: Boolean = true,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: ImeAction = ImeAction.Done,
    singleLine: Boolean = true
) {
    // Parse string to long
    val numericValue = value.filter { it.isDigit() }.toLongOrNull() ?: 0L
    
    CurrencyTextField(
        value = numericValue,
        onValueChange = { newValue ->
            onValueChange(newValue.toString())
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        enabled = enabled,
        isError = isError,
        showPrefix = showPrefix,
        shape = shape,
        colors = colors,
        keyboardActions = keyboardActions,
        imeAction = imeAction,
        singleLine = singleLine
    )
}
