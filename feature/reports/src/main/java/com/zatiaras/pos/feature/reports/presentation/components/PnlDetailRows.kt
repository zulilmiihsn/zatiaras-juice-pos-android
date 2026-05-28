package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DetailLineItem(
    label: String,
    amount: Long,
    isSubItem: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isSubItem) 24.dp else 12.dp,
                top = 3.dp,
                bottom = 3.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (isSubItem) "- $label" else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatRupiah(amount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
