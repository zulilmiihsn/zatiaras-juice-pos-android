package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.components.ZatDialog
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.Slate200
import com.zatiaras.pos.core.ui.theme.Slate500
import com.zatiaras.pos.feature.pos.R

@Composable
fun CustomItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableLongStateOf(0L) }
    val isValid = name.isNotBlank() && price > 0L

    ZatDialog(onDismissRequest = onDismiss) { dismiss ->
        Box(
            modifier = Modifier.fillMaxWidth(0.95f),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = AppShapes.XXL,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CustomItemDialogContent(
                        name = name,
                        price = price,
                        isValid = isValid,
                        onNameChange = { name = it },
                        onPriceChange = { price = it },
                        onDismiss = dismiss,
                        onConfirm = { onConfirm(name.trim(), price) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomItemDialogContent(
    name: String,
    price: Long,
    isValid: Boolean,
    onNameChange: (String) -> Unit,
    onPriceChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Text(
        text = stringResource(R.string.pos_custom_item),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Brand500,
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.pos_custom_item_name)) },
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.M,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand500,
            focusedLabelColor = Brand500,
        ),
    )

    Spacer(modifier = Modifier.height(16.dp))

    CurrencyTextField(
        value = price,
        onValueChange = onPriceChange,
        label = { Text(stringResource(R.string.pos_custom_item_price)) },
        showPrefix = true,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.M,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand500,
            focusedLabelColor = Brand500,
        ),
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = AppShapes.M,
            border = BorderStroke(1.dp, Slate200),
        ) {
            Text(stringResource(R.string.dialog_cancel), color = Slate500)
        }

        Button(
            onClick = onConfirm,
            enabled = isValid,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = AppShapes.M,
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand500,
                disabledContainerColor = Slate200,
            ),
        ) {
            Text(stringResource(R.string.pos_add), fontWeight = FontWeight.Bold)
        }
    }
}
