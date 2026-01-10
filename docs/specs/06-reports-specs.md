# 📊 Reports & Dashboard Specifications

> **Module**: `feature:reports`
> **Status**: 🟢 Implemented
> **Last Updated**: 2026-01-10

---

## Overview

The Reports module provides comprehensive business analytics and insights for the POS system. It aggregates transaction data to display daily statistics, revenue trends, top-selling products, and profit/loss reports.

---

## Features

### 1. Dashboard Statistics
Real-time overview of business performance:
- **Today's Revenue**: Total sales for current day
- **Transaction Count**: Number of completed transactions
- **Items Sold**: Total quantity of products sold
- **Weekly Revenue**: Cumulative revenue for current week
- **Monthly Revenue**: Cumulative revenue for current month
- **Growth Indicator**: Percentage change vs previous period

### 2. Revenue Line Chart
Interactive 7-day revenue trend visualization:
- Animated line chart using Compose Canvas
- Gradient fill under the line
- Day labels (Senin, Selasa, etc.)
- Dot indicators for each data point
- Empty state when no data available

### 3. Top Selling Products
Ranked list of best-selling products:
- Top 5 products by quantity sold
- Animated progress bars
- Gold/Silver/Bronze rank badges
- Revenue per product display

### 4. Profit & Loss Report (Future)
Comprehensive financial summary:
- Gross Revenue
- Discounts Applied
- Net Revenue
- Tax Collected
- Estimated Cost (when available)
- Gross Profit

---

## Technical Architecture

### Domain Models

```kotlin
// Daily revenue for chart visualization
data class DailyRevenue(
    val date: Long,
    val revenue: Long,
    val transactionCount: Int
)

// Top selling product
data class TopProduct(
    val productId: String,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Long
)

// Dashboard statistics
data class DashboardStats(
    val todayRevenue: Long,
    val todayTransactions: Int,
    val todayItemsSold: Int,
    val weeklyRevenue: Long,
    val monthlyRevenue: Long,
    val revenueGrowthPercent: Double
)
```

### Repository Interface

```kotlin
interface ReportRepository {
    suspend fun getDashboardStats(): DashboardStats
    suspend fun getDailyRevenueHistory(days: Int = 7): List<DailyRevenue>
    suspend fun getTopSellingProducts(startDate: Long, endDate: Long, limit: Int): List<TopProduct>
    suspend fun getProfitLossReport(startDate: Long, endDate: Long): ProfitLossReport
}
```

### Database Queries (TransactionDao)

New queries added for reports:
- `getDailyRevenue(startDate, endDate)` - Groups by day
- `getTopSellingProducts(startDate, endDate, limit)` - JOIN with transaction_items
- `getRevenueSummary(startDate, endDate)` - Aggregates totals

---

## UI Components

### StatCard
Gradient card with icon for displaying statistics:
- Configurable gradient colors
- Optional trend indicator (+/-%)
- Icon badge in corner
- Animated appearance

### RevenueLineChart
Custom Compose Canvas chart:
- Smooth line with rounded caps
- Vertical gradient fill
- Animated drawing effect
- Point indicators

### TopProductsList
Ranked product list:
- Medal-style rank badges
- Animated progress bars
- Revenue display

---

## Navigation

```kotlin
// Route constant
const val REPORT_DASHBOARD_ROUTE = "reports/dashboard"

// Navigate to reports
navController.navigateToReports()

// Add to NavGraph
reportsScreen(onNavigateBack = { navController.popBackStack() })
```

---

## Dependencies

The module depends on:
- `:core:ui` - Theme and shared components
- `:core:data` - TransactionDao for queries
- `:core:domain` - Shared domain models

---

## Future Enhancements

1. **Date Range Picker**: Custom period selection
2. **Export to PDF/Excel**: Report export functionality
3. **Category Breakdown**: Sales by product category
4. **Hourly Analysis**: Peak hours identification
5. **Comparison Charts**: Compare periods side-by-side
6. **AI Insights**: Smart recommendations based on data

---

## Testing Checklist

- [ ] Dashboard loads without errors
- [ ] Revenue chart displays correctly
- [ ] Top products sorted by quantity
- [ ] Pull-to-refresh works
- [ ] Empty states display properly
- [ ] Growth percentage calculated correctly
- [ ] Navigation works from Home menu
