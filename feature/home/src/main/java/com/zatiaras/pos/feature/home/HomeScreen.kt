package com.zatiaras.pos.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.feature.home.R

data class HomeMenuItem(
    val id: String,
    val titleResId: Int,
    val subtitleResId: Int,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val enabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    onNavigateToPOS: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    HomeScreen(
        userName = uiState.userName,
        branchName = uiState.branchName,
        onMenuClick = { menuId ->
            when (menuId) {
                "pos" -> onNavigateToPOS()
                "inventory" -> onNavigateToInventory()
                "transactions" -> onNavigateToTransactions()
                "reports" -> onNavigateToReports()
                "settings" -> onNavigateToSettings()
            }
        },
        onLogoutClick = viewModel::logout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    branchName: String,
    onMenuClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val menuItems = remember {
        listOf(
            HomeMenuItem(
                id = "pos",
                titleResId = R.string.menu_pos,
                subtitleResId = R.string.menu_pos_subtitle,
                icon = Icons.Filled.ShoppingCart,
                gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
            ),
            HomeMenuItem(
                id = "inventory",
                titleResId = R.string.menu_inventory,
                subtitleResId = R.string.menu_inventory_subtitle,
                icon = Icons.Filled.Inventory2,
                gradientColors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
            ),
            HomeMenuItem(
                id = "transactions",
                titleResId = R.string.menu_transactions,
                subtitleResId = R.string.menu_transactions_subtitle,
                icon = Icons.Filled.Receipt,
                gradientColors = listOf(Color(0xFFf093fb), Color(0xFFf5576c))
                // Now enabled - Buku Kas feature
            ),
            HomeMenuItem(
                id = "reports",
                titleResId = R.string.menu_reports,
                subtitleResId = R.string.menu_reports_subtitle,
                icon = Icons.Filled.Analytics,
                gradientColors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
                enabled = true // Reports feature is ready!
            ),
            HomeMenuItem(
                id = "settings",
                titleResId = R.string.menu_settings,
                subtitleResId = R.string.menu_settings_subtitle,
                icon = Icons.Filled.Settings,
                gradientColors = listOf(Color(0xFF636363), Color(0xFFa2ab58)),
                enabled = true // Now available!
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = stringResource(R.string.home_logout)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val dimensions = LocalDimensions.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(dimensions.paddingM)
        ) {
            // Greeting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = AppShapes.L
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensions.paddingL),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_greeting),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = userName.ifEmpty { stringResource(R.string.home_user_fallback) },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconSizeXL),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensions.spacingL))

            Text(
                text = stringResource(R.string.home_main_menu),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = dimensions.spacingS)
            )

            // Menu Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingS),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingS),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(menuItems) { item ->
                    MenuCard(
                        item = item,
                        onClick = { onMenuClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    item: HomeMenuItem,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val alpha = if (item.enabled) 1f else 0.5f
    val title = stringResource(item.titleResId)
    val subtitle = if (item.enabled) {
        stringResource(item.subtitleResId)
    } else {
        stringResource(R.string.menu_coming_soon)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clip(AppShapes.L)
            .clickable(enabled = item.enabled, onClick = onClick),
        shape = AppShapes.L,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = item.gradientColors.map { it.copy(alpha = alpha) }
                    )
                )
                .padding(dimensions.paddingM)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White.copy(alpha = alpha)
                )
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = alpha)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = alpha * 0.8f)
                    )
                }
            }
        }
    }
}
