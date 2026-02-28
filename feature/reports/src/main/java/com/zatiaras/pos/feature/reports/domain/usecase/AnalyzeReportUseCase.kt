package com.zatiaras.pos.feature.reports.domain.usecase

import com.zatiaras.pos.core.domain.model.StoreSession
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import com.zatiaras.pos.core.domain.util.DateUtils
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.reports.domain.model.AiChatMessage
import com.zatiaras.pos.feature.reports.domain.model.CashFlowItem
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import com.zatiaras.pos.feature.reports.domain.model.TransactionSummaryItem
import com.zatiaras.pos.feature.reports.domain.repository.AiChatRepository
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import com.zatiaras.pos.feature.reports.presentation.chat.ChatMessage
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase that orchestrates report analysis:
 * 1. Detects the date range from the user query
 * 2. Gathers all relevant data from repositories
 * 3. Builds a system prompt with business context
 * 4. Sends the request to the AI provider via AiChatRepository
 *
 * This replaces the god-class ReportChatAssistant which had layer boundary violations
 * (importing core:data entities and DAOs from the presentation layer).
 */
@Singleton
class AnalyzeReportUseCase @Inject constructor(
    private val generateProfitLossReportUseCase: GenerateProfitLossReportUseCase,
    private val reportRepository: ReportRepository,
    private val storeSessionRepository: StoreSessionRepository,
    private val aiChatRepository: AiChatRepository
) {

    /**
     * Analyze a report query and return the AI-generated response.
     *
     * @param query User's question/message
     * @param history Previous chat messages (ChatMessage from UI)
     * @param imageBase64 Optional base64-encoded image data
     * @return Result<String> containing the AI response or failure
     */
    suspend operator fun invoke(
        query: String,
        history: List<ChatMessage>,
        imageBase64: String? = null
    ): Result<String> {
        return try {
            // 1. Detect desired date range from query
            val (startDate, endDate) = detectDateRange(query)

            // 2. Gather ALL available data
            val reportResult = generateProfitLossReportUseCase(startDate, endDate)
            if (reportResult.isFailure) {
                return Result.failure(
                    reportResult.exceptionOrNull() ?: Exception("Gagal memuat data laporan")
                )
            }
            val report = reportResult.getOrThrow()

            // Fetch data through proper repository layer (no direct DAO access)
            val transactions = reportRepository.getTransactionsForAnalysis(startDate, endDate)
                .getOrDefault(emptyList())
            val cashRecords = reportRepository.getCashRecordsForAnalysis(startDate, endDate)
                .getOrDefault(emptyList())
            val dashboardStats = reportRepository.getDashboardStats().getOrNull()
            val dailyRevenue = reportRepository.getDailyRevenueHistory(7).getOrNull() ?: emptyList()
            val topProducts = reportRepository.getTopSellingProducts(startDate, endDate, 10)
                .getOrNull() ?: emptyList()
            val storeSession = storeSessionRepository.getActiveSessionOneShot()
            val recentSessions = storeSessionRepository.getLastSessions(5).first()

            // 3. Build system prompt with all contexts
            val systemPrompt = buildSystemPrompt(
                report = report,
                transactions = transactions,
                cashRecords = cashRecords,
                dashboardStats = dashboardStats,
                dailyRevenue = dailyRevenue,
                topProducts = topProducts,
                isStoreOpen = storeSession != null,
                recentSessions = recentSessions
            )

            // 4. Prepare message list for AI
            val messages = mutableListOf<AiChatMessage>()
            messages.add(AiChatMessage(role = "system", content = systemPrompt))

            // Add conversation history (last 10 messages)
            history.takeLast(10).forEach { msg ->
                messages.add(
                    AiChatMessage(
                        role = if (msg.isUser) "user" else "assistant",
                        content = msg.content
                    )
                )
            }

            // Add current query with optional image
            messages.add(
                AiChatMessage(
                    role = "user",
                    content = query,
                    imageBase64 = imageBase64
                )
            )

            // 5. Send to AI via repository
            aiChatRepository.sendChatMessage(messages)
        } catch (e: Exception) {
            Timber.e(e, "Report analysis failed")
            Result.failure(e)
        }
    }

    /**
     * Detect the user's desired date range from Indonesian natural language.
     */
    internal fun detectDateRange(query: String): Pair<Long, Long> {
        val q = query.lowercase()
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        return when {
            q.contains("hari ini") -> DateUtils.getTodayRange()
            q.contains("kemarin") -> DateUtils.getYesterdayRange()
            q.contains("minggu ini") -> DateUtils.getThisWeekRange()
            q.contains("minggu lalu") -> {
                val lastMonday = today.with(
                    java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
                ).minusWeeks(1)
                val thisMonday = lastMonday.plusWeeks(1)
                lastMonday.atStartOfDay(zoneId).toInstant().toEpochMilli() to
                        thisMonday.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            q.contains("bulan lalu") -> {
                val firstOfLastMonth = today.minusMonths(1).withDayOfMonth(1)
                val firstOfThisMonth = today.withDayOfMonth(1)
                firstOfLastMonth.atStartOfDay(zoneId).toInstant().toEpochMilli() to
                        firstOfThisMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            q.contains("tahun ini") -> {
                val firstOfYear = today.withDayOfYear(1)
                val tomorrow = today.plusDays(1)
                firstOfYear.atStartOfDay(zoneId).toInstant().toEpochMilli() to
                        tomorrow.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            q.contains("tahun lalu") -> {
                val firstOfLastYear = today.minusYears(1).withDayOfYear(1)
                val firstOfThisYear = today.withDayOfYear(1)
                firstOfLastYear.atStartOfDay(zoneId).toInstant().toEpochMilli() to
                        firstOfThisYear.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            q.contains("3 bulan") -> DateUtils.getLastNDaysRange(90)
            q.contains("6 bulan") -> DateUtils.getLastNDaysRange(180)
            q.contains("7 hari") || q.contains("seminggu terakhir") -> DateUtils.getLastNDaysRange(7)
            q.contains("30 hari") || q.contains("sebulan terakhir") -> DateUtils.getLastNDaysRange(30)
            else -> DateUtils.getThisMonthRange()
        }
    }

    /**
     * Build a comprehensive system prompt with all business data context.
     * Uses domain models (TransactionSummaryItem, CashFlowItem) instead of data layer entities.
     */
    private fun buildSystemPrompt(
        report: ProfitLossReport,
        transactions: List<TransactionSummaryItem>,
        cashRecords: List<CashFlowItem>,
        dashboardStats: DashboardStats?,
        dailyRevenue: List<DailyRevenue>,
        topProducts: List<TopProduct>,
        isStoreOpen: Boolean,
        recentSessions: List<StoreSession>
    ): String {
        val fmt = { amount: Long -> CurrencyFormatter.formatCurrency(amount) }
        val dateFmt = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id"))
        val zoneId = ZoneId.systemDefault()

        // --- Section I: P&L ---
        val margin = if (report.grossRevenue > 0) {
            (report.netProfit.toDouble() / report.grossRevenue * 100).toInt()
        } else 0

        val productSalesText = report.productSales.take(10).joinToString("\n") {
            "  - ${it.productName}: ${it.quantity} pcs (${fmt(it.revenue)})"
        }

        val expenseText = report.expensesByCategory.joinToString("\n") {
            "  - ${it.category}: ${fmt(it.amount)}"
        }

        val manualIncomeText = report.manualIncomeItems.joinToString("\n") {
            "  - ${it.description}: ${fmt(it.amount)}"
        }

        // --- Section II: Transaction Patterns (uses domain model) ---
        val peakHour = transactions.groupBy {
            Instant.ofEpochMilli(it.createdAt).atZone(zoneId).hour
        }.maxByOrNull { it.value.size }?.let { (hour, txns) ->
            "$hour:00 (${txns.size} transaksi)"
        } ?: "Belum ada data"

        val paymentBreakdown = transactions.groupBy { it.paymentMethod }
            .mapValues { (_, txns) -> txns.size to txns.sumOf { it.grandTotal } }
            .entries.sortedByDescending { it.value.first }
            .joinToString("\n") { (method, pair) ->
                "  - $method: ${pair.first} transaksi (${fmt(pair.second)})"
            }

        val avgOrderValue = if (transactions.isNotEmpty()) {
            fmt(transactions.sumOf { it.grandTotal } / transactions.size)
        } else "Rp0"

        // --- Section III: Daily Revenue Trend ---
        val trendText = dailyRevenue.joinToString("\n") { day ->
            val dayName = java.text.SimpleDateFormat("EEE dd/MM", java.util.Locale("id"))
                .format(java.util.Date(day.date))
            "  - $dayName: ${fmt(day.revenue)} (${day.transactionCount} trx)"
        }

        // --- Section IV: Cash Flow (uses domain model) ---
        val totalCashIncome = cashRecords.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalCashExpense = cashRecords.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val cashFlowItems = cashRecords.takeLast(10).joinToString("\n") {
            val typeLabel = if (it.type == "INCOME") "Pemasukan" else "Pengeluaran"
            "  - [$typeLabel] ${it.description}: ${fmt(it.amount)}${if (it.category != null) " (${it.category})" else ""}"
        }

        // --- Section V: Top Products ---
        val topProductsText = topProducts.take(10).joinToString("\n") {
            "  - ${it.productName}: ${it.quantitySold} terjual -> ${fmt(it.totalRevenue)}"
        }

        // --- Section VI: Store Operations ---
        val storeStatus = if (isStoreOpen) "BUKA" else "TUTUP"
        val sessionHistory = recentSessions.take(5).joinToString("\n") { session ->
            val openTime = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("id"))
                .format(java.util.Date(session.openingTime))
            val closeTime = session.closingTime?.let {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale("id"))
                    .format(java.util.Date(it))
            } ?: "masih buka"
            "  - $openTime -> $closeTime (Modal: ${fmt(session.openingCash)})"
        }

        // --- Section VII: Dashboard KPI ---
        val dashboardText = if (dashboardStats != null) """
            - Revenue Hari Ini: ${fmt(dashboardStats.todayRevenue)}
            - Transaksi Hari Ini: ${dashboardStats.todayTransactions}
            - Item Terjual Hari Ini: ${dashboardStats.todayItemsSold}
            - Revenue Mingguan: ${fmt(dashboardStats.weeklyRevenue)}
            - Revenue Bulanan: ${fmt(dashboardStats.monthlyRevenue)}
            - Pertumbuhan: ${String.format("%.1f", dashboardStats.revenueGrowthPercent)}%
        """.trimIndent() else "Data dashboard tidak tersedia"

        return """
Anda adalah **Asisten Zatiaras**, konsultan bisnis AI cerdas untuk aplikasi POS Zatiaras.
Anda memiliki akses PENUH ke 7 sumber data real-time dari database toko.

===================================
PERIODE ANALISIS: ${dateFmt.format(report.periodStart)} s/d ${dateFmt.format(report.periodEnd)}
STATUS TOKO: $storeStatus
===================================

[I. LAPORAN LABA RUGI]
- Pendapatan POS: ${fmt(report.posNetRevenue)}
- Pendapatan Manual: ${fmt(report.manualOperatingIncome)}
- Pendapatan Lain: ${fmt(report.otherRevenue)}
- TOTAL PENDAPATAN: ${fmt(report.grossRevenue)}
- Total Beban: ${fmt(report.totalExpenses)}
- Laba Kotor: ${fmt(report.grossProfit)}
- Pajak (${report.taxPercentage}%): ${fmt(report.tax)}
- LABA BERSIH: ${fmt(report.netProfit)}
- PROFIT MARGIN: ${margin}%

Rincian Penjualan POS:
$productSalesText

Rincian Beban per Kategori:
$expenseText

Pemasukan Manual:
${if (manualIncomeText.isNotEmpty()) manualIncomeText else "  Tidak ada"}

[II. ANALISIS POLA TRANSAKSI (${transactions.size} transaksi)]
- Jam Tersibuk: $peakHour
- Rata-rata Nilai Order: $avgOrderValue
- Metode Pembayaran:
$paymentBreakdown

[III. TREN REVENUE HARIAN (7 Hari)]
$trendText

[IV. ARUS KAS (BUKU KAS)]
- Total Pemasukan Manual: ${fmt(totalCashIncome)}
- Total Pengeluaran Manual: ${fmt(totalCashExpense)}
- Detail Terbaru:
$cashFlowItems

[V. TOP 10 PRODUK TERLARIS]
$topProductsText

[VI. RIWAYAT OPERASIONAL TOKO]
$sessionHistory

[VII. DASHBOARD KPI REAL-TIME]
$dashboardText

===================================
PANDUAN ANALISIS BISNIS:
===================================
1. KORELASI DATA: Hubungkan antar-section. Misal: produk terlaris + jam sibuk = saran persiapan stok di jam sebelumnya.
2. DETEKSI ANOMALI: Jika pengeluaran di Buku Kas tidak proporsional dengan revenue, peringatkan.
3. TREN: Gunakan data 7 hari untuk memprediksi tren naik/turun. Beri peringatan dini jika revenue menurun 2 hari berturut-turut.
4. METODE BAYAR: Sarankan optimasi berdasarkan preferensi pelanggan (misal: QRIS dominan -> pastikan koneksi stabil).
5. OPERASIONAL: Analisis jam buka-tutup toko, apakah sudah optimal dengan jam sibuk pelanggan.
6. GUNAKAN DATA SAJA: JANGAN mengarang angka. Jika data tidak tersedia, katakan "Data tidak tersedia untuk periode ini".
7. BAHASA: Indonesia profesional tapi ramah.
8. STRUKTUR: Gunakan format yang MINIMALIS. HANYA gunakan heading tingkat dua (## JUDUL) untuk bagian utama. JANGAN gunakan tingkat heading lebih dari itu (###, ####). Gunakan poin-poin (- Poin) untuk rincian. SANGAT PENTING: Jangan tampilkan simbol markdown seperti ## atau ** secara mentah. Gunakan penulisan yang sangat bersih dan manusiawi. Gunakan emoji secara bijak di awal judul bagian.
        """.trimIndent()
    }
}
