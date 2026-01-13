package com.zatiaras.pos.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.feature.auth.R

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
        if (uiState is AuthUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
            viewModel.resetState()
        }
    }

    LoginScreen(
        uiState = uiState,
        syncStatus = syncStatus,
        onLoginClick = viewModel::login,
        onResyncClick = viewModel::resync,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    syncStatus: String?,
    onLoginClick: (String, String) -> Unit,
    onResyncClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val dimensions = LocalDimensions.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(dimensions.paddingXL),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(dimensions.spacingM))
            
            // Sync Status Indicator
            syncStatus?.let { status ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val isOffline = status.contains("offline", ignoreCase = true) || 
                                        status.contains("koneksi", ignoreCase = true)
                        val icon = when {
                            uiState is AuthUiState.Syncing -> Icons.Default.Sync
                            isOffline -> Icons.Default.CloudOff
                            else -> Icons.Default.CloudDone
                        }
                        val color = when {
                            uiState is AuthUiState.Syncing -> MaterialTheme.colorScheme.primary
                            isOffline -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        
                        if (uiState is AuthUiState.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensions.iconSizeXS),
                                strokeWidth = 2.dp,
                                color = color
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(dimensions.iconSizeXS)
                            )
                        }
                        Spacer(modifier = Modifier.width(dimensions.spacingXS))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    }

                    // Show refresh button only when offline
                    val isOffline = status.contains("offline", ignoreCase = true) || 
                                    status.contains("koneksi", ignoreCase = true)
                    if (isOffline && uiState !is AuthUiState.Syncing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.TextButton(
                            onClick = onResyncClick
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Ulang", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(dimensions.paddingXXL))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                enabled = uiState !is AuthUiState.Syncing
            )

            Spacer(modifier = Modifier.height(dimensions.spacingM))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password_label)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = uiState !is AuthUiState.Syncing
            )

            Spacer(modifier = Modifier.height(dimensions.spacingL))

            when (uiState) {
                is AuthUiState.Loading, is AuthUiState.Syncing -> {
                    CircularProgressIndicator()
                }
                else -> {
                    Button(
                        onClick = { onLoginClick(username, password) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = username.isNotEmpty() && password.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.auth_login_button))
                    }
                }
            }
        }
    }
}
