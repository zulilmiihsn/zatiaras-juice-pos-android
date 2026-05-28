package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.ErrorRedDark
import com.zatiaras.pos.core.ui.theme.LossRed
import com.zatiaras.pos.core.ui.theme.ProfitGreen
import com.zatiaras.pos.core.ui.theme.ProfitGreenDark

@Composable
internal fun SectionHeader(
    title: String,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(AppShapes.XS)
                .background(color),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
internal fun PnlLineItem(
    label: String,
    amount: Long,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isBold: Boolean = false,
    isNegative: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PnlLineLabel(
            label = label,
            icon = icon,
            iconColor = iconColor,
            isBold = isBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatPnlAmount(amount, isNegative),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isNegative) LossRed else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PnlLineLabel(
    label: String,
    icon: ImageVector?,
    iconColor: Color,
    isBold: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            PnlIconBadge(icon = icon, iconColor = iconColor)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun PnlIconBadge(
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(iconColor.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
internal fun ProfitRow(
    label: String,
    amount: Long,
    isProfit: Boolean,
) {
    val textColor = if (isProfit) ProfitGreenDark else ErrorRedDark
    val backgroundColor = if (isProfit) ProfitGreen.copy(alpha = 0.1f) else LossRed.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.M)
            .background(backgroundColor)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isProfit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }

            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
    }
}

private fun formatPnlAmount(amount: Long, isNegative: Boolean): String = if (isNegative && amount != 0L) {
    "(${formatRupiah(kotlin.math.abs(amount))})"
} else {
    formatRupiah(amount)
}
