package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.LossRed
import com.zatiaras.pos.core.ui.theme.ProfitGreen
import com.zatiaras.pos.core.ui.theme.ProfitGreenLight
import com.zatiaras.pos.core.ui.theme.TaxBlue
import com.zatiaras.pos.core.ui.theme.WarningAmberDark
import com.zatiaras.pos.feature.reports.R
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport

@Composable
internal fun PnlRevenueSection(report: ProfitLossReport) {
    SectionHeader(
        title = stringResource(R.string.pnl_revenue),
        color = ProfitGreen,
    )
    Spacer(modifier = Modifier.height(8.dp))

    ExpandableLineItem(
        label = stringResource(R.string.pnl_operating_revenue),
        amount = report.operatingRevenue,
        icon = Icons.Default.ArrowUpward,
        iconColor = ProfitGreen,
        hasDetails = report.productSales.isNotEmpty() || report.posNetRevenue > 0 || report.manualIncomeItems.isNotEmpty(),
    ) {
        PosRevenueDetails(report = report)
        ManualRevenueDetails(report = report)
    }

    if (report.otherRevenue > 0) {
        ExpandableLineItem(
            label = stringResource(R.string.pnl_other_revenue),
            amount = report.otherRevenue,
            icon = Icons.Default.ArrowUpward,
            iconColor = ProfitGreenLight,
            hasDetails = report.otherIncomeItems.isNotEmpty(),
        ) {
            report.otherIncomeItems.forEach { item ->
                DetailLineItem(
                    label = item.description,
                    amount = item.amount,
                    isSubItem = true,
                )
            }
        }
    }

    PnlSectionDivider()
    PnlLineItem(
        label = stringResource(R.string.pnl_total_revenue),
        amount = report.grossRevenue,
        isBold = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun PosRevenueDetails(report: ProfitLossReport) {
    if (report.posNetRevenue <= 0) return

    DetailLineItem(
        label = stringResource(R.string.pnl_pos_sales),
        amount = report.posNetRevenue,
    )

    report.productSales.forEach { item ->
        DetailLineItem(
            label = stringResource(
                R.string.pnl_product_with_qty,
                item.productName,
                item.quantity,
            ),
            amount = item.revenue,
            isSubItem = true,
        )
    }
}

@Composable
private fun ManualRevenueDetails(report: ProfitLossReport) {
    if (report.manualIncomeItems.isEmpty()) return

    DetailLineItem(
        label = stringResource(R.string.pnl_manual_revenue),
        amount = report.manualOperatingIncome,
    )

    report.manualIncomeItems.forEach { item ->
        DetailLineItem(
            label = item.description,
            amount = item.amount,
            isSubItem = true,
        )
    }
}

@Composable
internal fun PnlExpenseSection(report: ProfitLossReport) {
    SectionHeader(
        title = stringResource(R.string.pnl_expenses),
        color = LossRed,
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (report.expensesByCategory.isNotEmpty()) {
        report.expensesByCategory.forEach { categoryItem ->
            ExpandableExpenseCategory(
                categoryItem = categoryItem,
                iconColor = LossRed,
            )
        }
    } else {
        PnlFallbackExpenseRows(report = report)
    }

    PnlSectionDivider()
    PnlLineItem(
        label = stringResource(R.string.pnl_total_expenses),
        amount = -report.totalExpenses,
        isBold = true,
        isNegative = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun PnlFallbackExpenseRows(report: ProfitLossReport) {
    if (report.operatingExpenses > 0) {
        PnlLineItem(
            label = stringResource(R.string.pnl_operating_expenses),
            amount = report.operatingExpenses,
            icon = Icons.Default.ArrowDownward,
            iconColor = LossRed,
            isNegative = true,
        )
    }

    if (report.otherExpenses > 0) {
        PnlLineItem(
            label = stringResource(R.string.pnl_other_expenses),
            amount = report.otherExpenses,
            icon = Icons.Default.ArrowDownward,
            iconColor = WarningAmberDark,
            isNegative = true,
        )
    }
}

@Composable
internal fun PnlProfitAndTaxSection(report: ProfitLossReport) {
    SectionHeader(
        title = stringResource(R.string.pnl_profit_and_tax),
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))

    PnlLineItem(
        label = stringResource(R.string.pnl_gross_profit),
        amount = report.grossProfit,
        isBold = true,
    )

    PnlLineItem(
        label = stringResource(R.string.pnl_tax, report.taxPercentage.toString()),
        amount = report.tax,
        icon = Icons.Default.Remove,
        iconColor = TaxBlue,
        isNegative = true,
    )

    Spacer(modifier = Modifier.height(10.dp))

    ProfitRow(
        label = stringResource(R.string.pnl_net_profit),
        amount = report.netProfit,
        isProfit = report.netProfit >= 0,
    )
}

@Composable
private fun PnlSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
