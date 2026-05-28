package com.zatiaras.pos.feature.inventory.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes

@Composable
fun InventoryTabs(
    selectedTab: InventoryTab,
    onTabSelected: (InventoryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                AppShapes.L,
            ),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab.ordinal])
                            .height(4.dp)
                            .padding(horizontal = 24.dp)
                            .clip(AppShapes.XS)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            },
            divider = {},
        ) {
            InventoryTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = stringResource(id = tab.titleResId),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
