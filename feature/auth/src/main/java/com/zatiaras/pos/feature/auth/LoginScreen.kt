package com.zatiaras.pos.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Brand100
import com.zatiaras.pos.core.ui.theme.Brand200
import com.zatiaras.pos.core.ui.theme.Brand50
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.Brand600
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.Slate50
import com.zatiaras.pos.core.ui.theme.Slate200
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.theme.WarningAmber
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.auth.R
import com.zatiaras.pos.core.ui.R as CoreUiR

// Brand colors
private val PrimaryPink = Brand500
private val DarkPink = Brand600
private val LightPink = Slate50
private val SurfacePink = androidx.compose.ui.graphics.Color.White
private val InputBackground = Slate50
private val WarningOrange = WarningAmber

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
    }
    
    // Logic to show error snackbar
    val errorState = uiState as? AuthUiState.Error
    if (errorState != null) {
        val errorMessage = errorState.message.asString()
        LaunchedEffect(errorState) {
            snackbarHostState.showSnackbar(errorMessage)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    syncStatus: com.zatiaras.pos.core.ui.util.UiText?,
    onLoginClick: (String, String, String) -> Unit,
    onResyncClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    val dimensions = LocalDimensions.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Branch Selection State
    var branchExpanded by remember { mutableStateOf(false) }
    var selectedBranch by remember { mutableStateOf<String?>("samarinda_juanda") }
    val branches = listOf(
        "samarinda_juanda" to stringResource(R.string.auth_branch_samarinda_juanda),
        "samarinda_slamet" to stringResource(R.string.auth_branch_samarinda_slamet),
        "berau" to stringResource(R.string.auth_branch_berau)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { com.zatiaras.pos.core.ui.components.ZatSnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            LightPink,
                            SurfacePink,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensions.paddingXL),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo with glow effect
                Box(
                    modifier = Modifier
                        .size(dimensions.iconSizeHero + dimensions.paddingXL)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PrimaryPink.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = CoreUiR.drawable.zatiaras_logo),
                        contentDescription = stringResource(R.string.brand_logo_content_desc),
                        modifier = Modifier.size(dimensions.iconSizeHero + dimensions.paddingM)
                    )
                }
                
                Spacer(modifier = Modifier.height(dimensions.spacingL))
                
                // App title with gradient text effect (simulated)
                Text(
                    text = stringResource(R.string.auth_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPink
                )
                
                Text(
                    text = stringResource(R.string.auth_pos_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(dimensions.spacingM))
                
                // Sync Status Badge - Compact and stylish
                syncStatus?.let { status ->
                    SyncStatusBadge(
                        statusText = status,
                        isSyncing = uiState is AuthUiState.Syncing,
                        onResyncClick = onResyncClick
                    )
                }
                
                Spacer(modifier = Modifier.height(dimensions.paddingXXL))

                // Login Card with premium feel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.L,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(dimensions.paddingXL),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_login_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPink
                        )

                        // Branch Selection Dropdown - Enhanced
                        ExposedDropdownMenuBox(
                            expanded = branchExpanded,
                            onExpandedChange = { branchExpanded = !branchExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = branches.find { it.first == selectedBranch }?.second ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.auth_branch_label)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Store,
                                        contentDescription = null,
                                        tint = PrimaryPink
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPink,
                                    focusedLabelColor = PrimaryPink,
                                    cursorColor = PrimaryPink,
                                    unfocusedBorderColor = Slate200,
                                    unfocusedContainerColor = InputBackground,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = AppShapes.M,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = branchExpanded,
                                onDismissRequest = { branchExpanded = false }
                            ) {
                                branches.forEach { (id, label) ->
                                    DropdownMenuItem(
                                        text = { Text(text = label) },
                                        onClick = {
                                            selectedBranch = id
                                            branchExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Store,
                                                contentDescription = null,
                                                tint = if (selectedBranch == id) PrimaryPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                        // Username Field - Enhanced
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(R.string.auth_username)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryPink
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            enabled = uiState !is AuthUiState.Syncing,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPink,
                                focusedLabelColor = PrimaryPink,
                                cursorColor = PrimaryPink,
                                unfocusedBorderColor = Slate200,
                                unfocusedContainerColor = InputBackground,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = AppShapes.M
                        )

                        // Password Field - Enhanced with visibility toggle
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.auth_password_label)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PrimaryPink
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = uiState !is AuthUiState.Syncing,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPink,
                                focusedLabelColor = PrimaryPink,
                                cursorColor = PrimaryPink,
                                unfocusedBorderColor = Slate200,
                                unfocusedContainerColor = InputBackground,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = AppShapes.M
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimensions.spacingL))

                // Login Button - Premium gradient style
                when (uiState) {
                    is AuthUiState.Loading, is AuthUiState.Syncing -> {
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensions.buttonHeightLarge),
                            shape = AppShapes.M,
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = PrimaryPink.copy(alpha = 0.5f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (uiState is AuthUiState.Syncing) stringResource(R.string.auth_sync_progress) else stringResource(R.string.auth_login_progress),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = { 
                                if (selectedBranch != null) {
                                    onLoginClick(username, password, selectedBranch!!) 
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensions.buttonHeightLarge),
                            enabled = username.isNotEmpty() && password.isNotEmpty() && selectedBranch != null,
                            shape = AppShapes.M,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryPink,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            Text(
                                stringResource(R.string.auth_login_button),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(dimensions.paddingXXL + dimensions.spacingM))
            }
        }
    }
}

/**
 * Compact sync status badge with modern styling
 */
@Composable
private fun SyncStatusBadge(
    statusText: com.zatiaras.pos.core.ui.util.UiText,
    isSyncing: Boolean,
    onResyncClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val status = statusText.asString()
    val isOffline = status.contains("offline", ignoreCase = true) || 
                    status.contains("koneksi", ignoreCase = true)
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSyncing -> PrimaryPink.copy(alpha = 0.1f)
            isOffline -> WarningOrange.copy(alpha = 0.1f)
            else -> SuccessGreen.copy(alpha = 0.1f)
        },
        animationSpec = tween(300),
        label = "bgColor"
    )
    
    val contentColor = when {
        isSyncing -> PrimaryPink
        isOffline -> WarningOrange
        else -> SuccessGreen
    }
    
    Card(
        shape = AppShapes.XL,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimensions.paddingM, vertical = dimensions.paddingXS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
            
            // Show resync button when offline
            if (isOffline && !isSyncing) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onResyncClick,
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
