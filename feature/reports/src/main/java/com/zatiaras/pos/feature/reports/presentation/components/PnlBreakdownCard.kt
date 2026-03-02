package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.reports.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.ErrorRedDark
import com.zatiaras.pos.core.ui.theme.LossRed
import com.zatiaras.pos.core.ui.theme.ProfitGreen
import com.zatiaras.pos.core.ui.theme.ProfitGreenDark
import com.zatiaras.pos.core.ui.theme.ProfitGreenLight
import com.zatiaras.pos.core.ui.theme.TaxBlue
import com.zatiaras.pos.core.ui.theme.WarningAmberDark
import com.zatiaras.pos.feature.reports.domain.model.ExpenseCategoryItem
import com.zatiaras.pos.feature.reports.domain.model.ProductSaleItem
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import androidx.compose.foundation.border

/**
 * Comprehensive P&L breakdown card.
 */
@Composable
fun PnlBreakdownCard(
    report: ProfitLossReport,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.pnl_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.pnl_transactions_count, report.transactionCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // --- 1. INCOME SECTION ---
            SectionHeader(
                title = stringResource(R.string.pnl_revenue),
                color = ProfitGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Expandable Operating Revenue
            ExpandableLineItem(
                label = stringResource(R.string.pnl_operating_revenue),
                amount = report.operatingRevenue,
                icon = Icons.Default.ArrowUpward,
                iconColor = ProfitGreen,
                hasDetails = report.productSales.isNotEmpty() || report.posNetRevenue > 0 || report.manualIncomeItems.isNotEmpty()
            ) {
                // POS Sales Detail
                if (report.posNetRevenue > 0) {
                    DetailLineItem(
                        label = stringResource(R.string.pnl_pos_sales),
                        amount = report.posNetRevenue
                    )
                    
                    // Product Sales Breakdown - display all items without truncation
                    report.productSales.forEach { item ->
                        DetailLineItem(
                            label = stringResource(
                                R.string.pnl_product_with_qty,
                                item.productName,
                                item.quantity
                            ),
                            amount = item.revenue,
                            isSubItem = true
                        )
                    }
                }
                
                // Manual Operating Income with breakdown
                if (report.manualIncomeItems.isNotEmpty()) {
                    DetailLineItem(
                        label = stringResource(R.string.pnl_manual_revenue),
                        amount = report.manualOperatingIncome
                    )
                    
                    // Detail per item (e.g., "Sangu Ilham", "Titipan dari X")
                    report.manualIncomeItems.forEach { item ->
                        DetailLineItem(
                            label = item.description,
                            amount = item.amount,
                            isSubItem = true
                        )
                    }
                }
            }
            
            // Expandable Other Revenue (if any)
            if (report.otherRevenue > 0) {
                ExpandableLineItem(
                    label = stringResource(R.string.pnl_other_revenue),
                    amount = report.otherRevenue,
                    icon = Icons.Default.ArrowUpward,
                    iconColor = ProfitGreenLight,
                    hasDetails = report.otherIncomeItems.isNotEmpty()
                ) {
                    report.otherIncomeItems.forEach { item ->
                        DetailLineItem(
                            label = item.description,
                            amount = item.amount,
                            isSubItem = true
                        )
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            PnlLineItem(
                label = stringResource(R.string.pnl_total_revenue),
                amount = report.grossRevenue,
                isBold = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- 2. EXPENSE SECTION ---
            SectionHeader(
                title = stringResource(R.string.pnl_expenses),
                color = LossRed
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Dynamic expense categories from data
            if (report.expensesByCategory.isNotEmpty()) {
                report.expensesByCategory.forEach { categoryItem ->
                    ExpandableExpenseCategory(
                        categoryItem = categoryItem,
                        iconColor = LossRed
                    )
                }
            } else {
                // Fallback to summary view if no details
                if (report.operatingExpenses > 0) {
                    PnlLineItem(
                        label = stringResource(R.string.pnl_operating_expenses),
                        amount = report.operatingExpenses,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = LossRed,
                        isNegative = true
                    )
                }
                
                if (report.otherExpenses > 0) {
                    PnlLineItem(
                        label = stringResource(R.string.pnl_other_expenses),
                        amount = report.otherExpenses,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = WarningAmberDark,
                        isNegative = true
                    )
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            PnlLineItem(
                label = stringResource(R.string.pnl_total_expenses),
                amount = -report.totalExpenses, // Display as negative
                isBold = true,
                isNegative = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- 3. PROFIT / TAX SECTION ---
            SectionHeader(
                title = stringResource(R.string.pnl_profit_and_tax),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            PnlLineItem(
                label = stringResource(R.string.pnl_gross_profit),
                amount = report.grossProfit,
                isBold = true
            )

            PnlLineItem(
                label = stringResource(R.string.pnl_tax, report.taxPercentage.toString()),
                amount = report.tax, 
                icon = Icons.Default.Remove,
                iconColor = TaxBlue,
                isNegative = true
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            ProfitRow(
                label = stringResource(R.string.pnl_net_profit),
                amount = report.netProfit,
                isProfit = report.netProfit >= 0
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(AppShapes.XS)
                .background(color)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PnlLineItem(
    label: String,
    amount: Long,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isBold: Boolean = false,
    isNegative: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = if (isNegative && amount != 0L) {
                "(${formatRupiah(kotlin.math.abs(amount))})"
            } else {
                formatRupiah(amount)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isNegative) LossRed else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GrandTotalRow(
    amount: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.M)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pnl_total_received),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ProfitRow(
    label: String,
    amount: Long,
    isProfit: Boolean
) {
    val backgroundColor = if (isProfit) {
        ProfitGreen.copy(alpha = 0.1f)
    } else {
        LossRed.copy(alpha = 0.1f)
    }
    
    val textColor = if (isProfit) {
        ProfitGreenDark
    } else {
        ErrorRedDark
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.M)
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isProfit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * Expandable line item that shows details when clicked.
 */
@Composable
private fun ExpandableLineItem(
    label: String,
    amount: Long,
    icon: ImageVector,
    iconColor: Color,
    hasDetails: Boolean = false,
    detailsContent: @Composable () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )
    
    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasDetails) {
                        Modifier.clickable { isExpanded = !isExpanded }
                    } else Modifier
                )
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (hasDetails) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(rotationAngle)
                    )
                }
            }
            
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded && hasDetails,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
            ) {
                detailsContent()
            }
        }
    }
}

/**
 * Expandable expense category with items.
 */
@Composable
private fun ExpandableExpenseCategory(
    categoryItem: ExpenseCategoryItem,
    iconColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )
    val hasItems = categoryItem.items.isNotEmpty()
    
    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasItems) Modifier.clickable { isExpanded = !isExpanded } else Modifier
                )
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = categoryItem.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (hasItems) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(rotationAngle)
                    )
                }
            }
            
            Text(
                text = stringResource(R.string.pnl_amount_in_parentheses, formatRupiah(categoryItem.amount)),
                style = MaterialTheme.typography.bodyMedium,
                color = LossRed
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded && hasItems,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 8.dp)
            ) {
                categoryItem.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatRupiah(item.amount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detail line item for expanded sections.
 */
@Composable
private fun DetailLineItem(
    label: String,
    amount: Long,
    isSubItem: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isSubItem) 24.dp else 12.dp,
                top = 3.dp,
                bottom = 3.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isSubItem) "• $label" else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatRupiah(amount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
