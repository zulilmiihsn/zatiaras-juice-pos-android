package com.zatiaras.pos.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zatiaras.pos.NavRoutes
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.feature.pos.domain.model.CartHolder
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.navigation.PosRoutes
import com.zatiaras.pos.feature.pos.navigation.cashRecordScreen
import com.zatiaras.pos.feature.pos.navigation.posScreen
import com.zatiaras.pos.feature.reports.navigation.homeDashboardScreen
import com.zatiaras.pos.feature.reports.navigation.reportsScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
) {
    data object Home : BottomNavItem(
        NavRoutes.HOME,
        "Beranda",
        Icons.Filled.Home,
        Icons.Outlined.Home,
    )
    data object Pos : BottomNavItem(
        PosRoutes.POS,
        "Kasir",
        Icons.Filled.ShoppingCart,
        Icons.Outlined.ShoppingCart,
    )
    data object CashRecord : BottomNavItem(
        NavRoutes.CASH_RECORD,
        "Catat",
        Icons.Filled.Receipt,
        Icons.Outlined.Receipt,
    )
    data object Reports : BottomNavItem(
        NavRoutes.REPORTS,
        "Laporan",
        Icons.Filled.Analytics,
        Icons.Outlined.Analytics,
    )
}

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    cartHolder: CartHolder,
    onNavigateBackFromMain: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToReceipt: (Transaction) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    accessControlManager: com.zatiaras.pos.core.domain.access.AccessChecker? = null,
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Pos,
        BottomNavItem.CashRecord,
        BottomNavItem.Reports,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Only show bottom bar on top-level destinations
            val isTopLevel = items.any { it.route == currentDestination?.route }

            if (isTopLevel) {
                EnhancedBottomNavigationBar(
                    items = items,
                    currentRoute = currentDestination?.route,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Tab 1: Home (Complete Business Dashboard with stats, charts, top products)
            homeDashboardScreen(
                route = NavRoutes.HOME,
                onNavigateToSettings = onNavigateToSettings,
            )

            // Tab 2: POS
            posScreen(
                cartHolder = cartHolder,
                onNavigateBack = { /* No back action for tab */ },
                onNavigateToCheckout = onNavigateToCheckout,
            )

            // Tab 3: Cash Record (Buku Kas) - Protected by Access Control
            cashRecordScreen(
                onNavigateBack = {
                    // Navigate to Home when access denied
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToReceipt = onNavigateToReceipt,
                accessControlManager = accessControlManager,
            )

            // Tab 4: Reports (P&L Report with Tanya AI) - Protected by Access Control
            reportsScreen(
                route = NavRoutes.REPORTS,
                onNavigateBack = {
                    // Navigate to Home when access denied
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToChat = onNavigateToChat,
                accessControlManager = accessControlManager,
            )
        }
    }
}

@Composable
private fun EnhancedBottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                EnhancedNavigationBarItem(
                    item = item,
                    selected = isSelected,
                    onClick = { onItemClick(item.route) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.EnhancedNavigationBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Smooth animations
    val animationSpec: AnimationSpec<Float> = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = animationSpec,
        label = "iconScale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = animationSpec,
        label = "alpha",
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 300),
        label = "iconColor",
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 300),
        label = "textColor",
    )

    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 32.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing,
        ),
        label = "indicatorWidth",
    )

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Animated indicator pill on top of icon
                Box(
                    modifier = Modifier
                        .size(width = indicatorWidth, height = 3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = AppShapes.XS,
                        ),
                )

                // Icon with scale animation
                Icon(
                    imageVector = if (selected) item.iconFilled else item.iconOutlined,
                    contentDescription = item.title,
                    tint = iconColor,
                    modifier = Modifier
                        .size(26.dp)
                        .scale(scale)
                        .alpha(alpha),
                )
            }
        },
        label = {
            Text(
                text = item.title,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.alpha(alpha),
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = Color.Transparent, // We use custom indicator
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        alwaysShowLabel = true,
    )
}
