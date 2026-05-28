package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.LossRed
import com.zatiaras.pos.feature.reports.R
import com.zatiaras.pos.feature.reports.domain.model.ExpenseCategoryItem

@Composable
internal fun ExpandableLineItem(
    label: String,
    amount: Long,
    icon: ImageVector,
    iconColor: Color,
    hasDetails: Boolean = false,
    detailsContent: @Composable () -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by rememberExpansionRotation(isExpanded)

    Column(modifier = Modifier.animateContentSize()) {
        ExpandableRow(
            label = label,
            amountText = formatRupiah(amount),
            icon = icon,
            iconColor = iconColor,
            isExpandable = hasDetails,
            rotationAngle = rotationAngle,
            onToggleExpanded = { isExpanded = !isExpanded },
        )

        AnimatedVisibility(
            visible = isExpanded && hasDetails,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
            ) {
                detailsContent()
            }
        }
    }
}

@Composable
internal fun ExpandableExpenseCategory(
    categoryItem: ExpenseCategoryItem,
    iconColor: Color,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by rememberExpansionRotation(isExpanded)
    val hasItems = categoryItem.items.isNotEmpty()

    Column(modifier = Modifier.animateContentSize()) {
        ExpandableRow(
            label = categoryItem.category,
            amountText = stringResource(R.string.pnl_amount_in_parentheses, formatRupiah(categoryItem.amount)),
            icon = Icons.Default.ArrowDownward,
            iconColor = iconColor,
            amountColor = LossRed,
            isExpandable = hasItems,
            rotationAngle = rotationAngle,
            onToggleExpanded = { isExpanded = !isExpanded },
        )

        AnimatedVisibility(
            visible = isExpanded && hasItems,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 8.dp),
            ) {
                categoryItem.items.forEach { item ->
                    ExpenseDetailRow(
                        label = item.description,
                        amount = item.amount,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableRow(
    label: String,
    amountText: String,
    icon: ImageVector,
    iconColor: Color,
    isExpandable: Boolean,
    rotationAngle: Float,
    onToggleExpanded: () -> Unit,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isExpandable) Modifier.clickable(onClick = onToggleExpanded) else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            PnlIconBadge(icon = icon, iconColor = iconColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isExpandable) {
                ExpandIcon(rotationAngle = rotationAngle)
            }
        }

        Text(
            text = amountText,
            style = MaterialTheme.typography.bodyMedium,
            color = amountColor,
        )
    }
}

@Composable
private fun ExpandIcon(rotationAngle: Float) {
    Spacer(modifier = Modifier.width(4.dp))
    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(16.dp)
            .rotate(rotationAngle),
    )
}

@Composable
private fun ExpenseDetailRow(label: String, amount: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
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

@Composable
private fun rememberExpansionRotation(isExpanded: Boolean) = animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    label = "rotation",
)
