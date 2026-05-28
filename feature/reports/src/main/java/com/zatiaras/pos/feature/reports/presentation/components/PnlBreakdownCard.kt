package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.feature.reports.R
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport

/**
 * P&L breakdown shell.
 *
 * The card only owns ordering and shared chrome. Each section lives in a
 * smaller sibling file so future changes can target revenue, expenses, or
 * profit/tax rows without scanning the whole report layout.
 */
@Composable
fun PnlBreakdownCard(
    report: ProfitLossReport,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PnlBreakdownTitle(transactionCount = report.transactionCount)
            Spacer(modifier = Modifier.height(14.dp))

            PnlRevenueSection(report = report)
            PnlExpenseSection(report = report)
            PnlProfitAndTaxSection(report = report)
        }
    }
}

@Composable
private fun PnlBreakdownTitle(transactionCount: Int) {
    Text(
        text = stringResource(R.string.pnl_summary),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )

    Text(
        text = stringResource(R.string.pnl_transactions_count, transactionCount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
