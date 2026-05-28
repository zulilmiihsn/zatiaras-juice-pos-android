package com.zatiaras.pos.feature.auth.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.InfoBlue
import com.zatiaras.pos.core.ui.theme.PurpleAccent
import com.zatiaras.pos.core.ui.theme.WarningAmber
import com.zatiaras.pos.feature.auth.R

private val LockIconColor = Brand500
private val BiometricIconColor = PurpleAccent
private val PinIconColor = InfoBlue
private val PasswordIconColor = WarningAmber

@Composable
internal fun SecuritySettingsContent(
    uiState: SettingsUiState,
    onLockEnabledChange: (Boolean) -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onChangePinClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
) {
    SecurityStatusHeader(
        isLockEnabled = uiState.lockEnabled,
        isPinSet = uiState.pinSet,
        isBiometricEnabled = uiState.biometricEnabled,
    )

    Spacer(modifier = Modifier.height(24.dp))

    SecuritySectionHeader(
        title = stringResource(R.string.sec_app_lock),
        subtitle = stringResource(R.string.sec_app_lock_desc),
    )

    Spacer(modifier = Modifier.height(12.dp))

    SecuritySettingsCard {
        AppLockItem(
            isLockEnabled = uiState.lockEnabled,
            onLockEnabledChange = onLockEnabledChange,
        )

        if (uiState.lockEnabled) {
            SecurityDivider()
            BiometricItem(
                isBiometricEnabled = uiState.biometricEnabled,
                isBiometricAvailable = uiState.biometricAvailable,
                onBiometricEnabledChange = onBiometricEnabledChange,
            )
        }

        SecurityDivider()
        SecuritySubsectionLabel(
            title = stringResource(R.string.sec_pin_module_title),
            subtitle = stringResource(R.string.sec_pin_module_desc),
        )
        PinItem(
            isPinSet = uiState.pinSet,
            onChangePinClick = onChangePinClick,
        )

        SecurityDivider()
        SecuritySubsectionLabel(
            title = stringResource(R.string.sec_password_module_title),
            subtitle = stringResource(R.string.sec_password_module_desc),
        )
        PasswordItem(onChangePasswordClick = onChangePasswordClick)
    }

    Spacer(modifier = Modifier.height(24.dp))
    SecurityTipsCard()
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
private fun SecuritySettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.XL),
        shape = AppShapes.XL,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            content()
        }
    }
}

@Composable
private fun AppLockItem(
    isLockEnabled: Boolean,
    onLockEnabledChange: (Boolean) -> Unit,
) {
    EnhancedSecurityItem(
        icon = Icons.Outlined.Lock,
        iconColor = LockIconColor,
        title = stringResource(R.string.sec_app_lock_switch),
        subtitle = if (isLockEnabled) {
            stringResource(R.string.sec_active_app_lock)
        } else {
            stringResource(R.string.sec_app_lock_switch_desc)
        },
        isEnabled = isLockEnabled,
        trailing = {
            Switch(
                checked = isLockEnabled,
                onCheckedChange = onLockEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = LockIconColor,
                ),
            )
        },
    )
}

@Composable
private fun BiometricItem(
    isBiometricEnabled: Boolean,
    isBiometricAvailable: Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
) {
    EnhancedSecurityItem(
        icon = Icons.Outlined.Fingerprint,
        iconColor = BiometricIconColor,
        title = stringResource(R.string.sec_biometric),
        subtitle = when {
            !isBiometricAvailable -> stringResource(R.string.sec_bio_not_avail)
            isBiometricEnabled -> stringResource(R.string.sec_bio_active)
            else -> stringResource(R.string.sec_bio_desc)
        },
        isEnabled = isBiometricEnabled,
        trailing = {
            Switch(
                checked = isBiometricEnabled,
                onCheckedChange = onBiometricEnabledChange,
                enabled = isBiometricAvailable,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = BiometricIconColor,
                ),
            )
        },
    )
}

@Composable
private fun PinItem(
    isPinSet: Boolean,
    onChangePinClick: () -> Unit,
) {
    EnhancedSecurityItem(
        icon = Icons.Outlined.Pin,
        iconColor = PinIconColor,
        title = if (isPinSet) stringResource(R.string.sec_change_pin) else stringResource(R.string.sec_set_pin),
        subtitle = if (isPinSet) stringResource(R.string.sec_pin_set) else stringResource(R.string.sec_pin_not_set),
        isEnabled = isPinSet,
        trailing = {
            SecurityActionButton(
                text = if (isPinSet) stringResource(R.string.auth_change) else stringResource(R.string.auth_configure),
                color = PinIconColor,
                onClick = onChangePinClick,
            )
        },
    )
}

@Composable
private fun PasswordItem(onChangePasswordClick: () -> Unit) {
    EnhancedSecurityItem(
        icon = Icons.Outlined.Password,
        iconColor = PasswordIconColor,
        title = stringResource(R.string.sec_change_password),
        subtitle = stringResource(R.string.sec_change_password_desc),
        isEnabled = true,
        trailing = {
            SecurityActionButton(
                text = stringResource(R.string.auth_change),
                color = PasswordIconColor,
                onClick = onChangePasswordClick,
            )
        },
    )
}

@Composable
private fun SecurityActionButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = AppShapes.M,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SecurityDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
