package com.zatiaras.pos.feature.auth.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.feature.auth.R

@Composable
internal fun ChangePasswordDialog(
    isLoading: Boolean,
    errorMessageType: PasswordChangeMessage?,
    errorMessageDetail: String?,
    successMessageType: PasswordChangeMessage?,
    onDismiss: () -> Unit,
    onSubmit: (currentPassword: String, newPassword: String, confirmPassword: String) -> Unit,
    onSuccessAcknowledge: () -> Unit,
) {
    val successMessage = successMessageType.toSuccessText()
    if (successMessage != null) {
        PasswordChangeSuccessDialog(
            message = successMessage,
            onSuccessAcknowledge = onSuccessAcknowledge,
        )
        return
    }

    PasswordChangeFormDialog(
        isLoading = isLoading,
        errorMessage = errorMessageType.toErrorText(errorMessageDetail),
        onDismiss = onDismiss,
        onSubmit = onSubmit,
    )
}

@Composable
private fun PasswordChangeSuccessDialog(
    message: String,
    onSuccessAcknowledge: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSuccessAcknowledge,
        title = { Text(text = stringResource(R.string.sec_change_password)) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onSuccessAcknowledge) {
                Text(text = stringResource(R.string.auth_back))
            }
        },
    )
}

@Composable
private fun PasswordChangeFormDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (currentPassword: String, newPassword: String, confirmPassword: String) -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.sec_change_password)) },
        text = {
            PasswordChangeFields(
                currentPassword = currentPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onCurrentPasswordChange = { currentPassword = it },
                onNewPasswordChange = { newPassword = it },
                onConfirmPasswordChange = { confirmPassword = it },
            )
        },
        confirmButton = {
            Button(
                enabled = !isLoading,
                onClick = {
                    onSubmit(currentPassword, newPassword, confirmPassword)
                },
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(R.string.auth_change))
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isLoading, onClick = onDismiss) {
                Text(text = stringResource(R.string.auth_back))
            }
        },
    )
}

@Composable
private fun PasswordChangeFields(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    isLoading: Boolean,
    errorMessage: String?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PasswordField(
            value = currentPassword,
            onValueChange = onCurrentPasswordChange,
            enabled = !isLoading,
            label = stringResource(R.string.sec_current_password),
        )
        PasswordField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            enabled = !isLoading,
            label = stringResource(R.string.sec_new_password),
        )
        PasswordField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            enabled = !isLoading,
            label = stringResource(R.string.sec_confirm_new_password),
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    label: String,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun PasswordChangeMessage?.toSuccessText(): String? = when (this) {
    PasswordChangeMessage.PASSWORD_CHANGED -> stringResource(R.string.sec_password_changed_success)
    else -> null
}

@Composable
private fun PasswordChangeMessage?.toErrorText(detail: String?): String? = when (this) {
    PasswordChangeMessage.REQUIRED_FIELDS -> stringResource(R.string.sec_error_required_fields)
    PasswordChangeMessage.MIN_LENGTH -> stringResource(R.string.sec_error_min_length)
    PasswordChangeMessage.CONFIRMATION_MISMATCH -> stringResource(R.string.sec_error_confirmation_mismatch)
    PasswordChangeMessage.SAME_AS_CURRENT -> stringResource(R.string.sec_error_same_as_current)
    PasswordChangeMessage.GENERIC_FAILURE -> detail ?: stringResource(R.string.sec_error_change_failed)
    else -> null
}
