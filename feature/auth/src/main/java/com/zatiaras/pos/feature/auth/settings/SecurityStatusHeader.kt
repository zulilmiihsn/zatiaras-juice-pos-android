package com.zatiaras.pos.feature.auth.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.theme.WarningAmber
import com.zatiaras.pos.feature.auth.R

@Composable
internal fun SecurityStatusHeader(
    isLockEnabled: Boolean,
    isPinSet: Boolean,
    isBiometricEnabled: Boolean,
) {
    val statusColor by animateColorAsState(
        targetValue = if (isLockEnabled) SuccessGreen else WarningAmber,
        animationSpec = tween(300),
        label = "statusColor",
    )
    val securityLevel = securityLevelText(
        isLockEnabled = isLockEnabled,
        isPinSet = isPinSet,
        isBiometricEnabled = isBiometricEnabled,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecurityHeaderIcon(
                isLockEnabled = isLockEnabled,
                statusColor = statusColor,
            )
            Spacer(modifier = Modifier.width(16.dp))
            SecurityHeaderText(
                isLockEnabled = isLockEnabled,
                statusColor = statusColor,
                modifier = Modifier.weight(1f),
            )
            SecurityLevelBadge(
                securityLevel = securityLevel,
                statusColor = statusColor,
            )
        }
    }
}

@Composable
private fun securityLevelText(
    isLockEnabled: Boolean,
    isPinSet: Boolean,
    isBiometricEnabled: Boolean,
): String = when {
    isLockEnabled && isPinSet && isBiometricEnabled -> stringResource(R.string.sec_level_high)
    isLockEnabled && (isPinSet || isBiometricEnabled) -> stringResource(R.string.sec_level_medium)
    isLockEnabled -> stringResource(R.string.sec_level_basic)
    else -> stringResource(R.string.sec_level_inactive)
}

@Composable
private fun SecurityHeaderIcon(
    isLockEnabled: Boolean,
    statusColor: Color,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(AppShapes.M)
            .background(statusColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isLockEnabled) Icons.Outlined.VerifiedUser else Icons.Outlined.GppMaybe,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun SecurityHeaderText(
    isLockEnabled: Boolean,
    statusColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.sec_status_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (isLockEnabled) {
                stringResource(R.string.sec_status_protected)
            } else {
                stringResource(R.string.sec_status_unlocked)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor,
        )
    }
}

@Composable
private fun SecurityLevelBadge(
    securityLevel: String,
    statusColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(AppShapes.S)
            .background(statusColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = securityLevel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
        )
    }
}
