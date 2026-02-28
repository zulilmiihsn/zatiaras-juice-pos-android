package com.zatiaras.pos.feature.reports.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.zatiaras.pos.core.domain.util.LocaleUtils
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for exporting reports to CSV format (compatible with Excel).
 */
@Singleton
class CsvExportService @Inject constructor() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", LocaleUtils.LOCALE_ID)
    private val currencyFormat = NumberFormat.getNumberInstance(LocaleUtils.LOCALE_ID)

    /**
     * Export P&L report to CSV.
     */
    fun exportPnlToCsv(
        context: Context,
        report: ProfitLossReport,
        periodName: String
    ): Result<Uri> {
        return exportToCsv(context, "Laporan_Laba_Rugi") { writer ->
            // Header
            writer.appendLine("LAPORAN LABA RUGI")
            writer.appendLine("Periode,$periodName")
            writer.appendLine("Dibuat,${dateFormat.format(Date())}")
            writer.appendLine("Jumlah Transaksi,${report.transactionCount}")
            writer.appendLine()
            
            // P&L Data
            writer.appendLine("Kategori,Jumlah")
            writer.appendLine("Pendapatan Operasional,${report.operatingRevenue}")
            writer.appendLine("Pendapatan Lainnya,${report.otherRevenue}")
            writer.appendLine("Total Pendapatan,${report.grossRevenue}")
            writer.appendLine("Beban Operasional,${report.operatingExpenses}")
            writer.appendLine("Beban Lainnya,${report.otherExpenses}")
            writer.appendLine("Total Beban,${report.totalExpenses}")
            writer.appendLine("Laba Kotor,${report.grossProfit}")
            writer.appendLine("Pajak (${report.taxPercentage}%),${report.tax}")
            writer.appendLine("Laba Bersih,${report.netProfit}")
            writer.appendLine()
            
            // --- DETAIL PENDAPATAN (PRODUK) ---
            writer.appendLine("=== DETAIL PENDAPATAN (PRODUK TERJUAL) ===")
            if (report.productSales.isEmpty()) {
                writer.appendLine("Tidak ada produk terjual")
            } else {
                writer.appendLine("Nama Produk,Jumlah Terjual,Total Pendapatan")
                report.productSales.forEach { item ->
                    writer.appendLine("${escapeCSV(item.productName)},${item.quantity},${item.revenue}")
                }
            }
            writer.appendLine()
            
            // --- DETAIL PENDAPATAN TAMBAHAN (Jika Ada) ---
            if (report.manualIncomeItems.isNotEmpty() || report.otherIncomeItems.isNotEmpty()) {
                writer.appendLine("=== DETAIL PENDAPATAN TAMBAHAN ===")
                writer.appendLine("Deskripsi Kategori,Jumlah")
                report.manualIncomeItems.forEach { item ->
                    writer.appendLine("${escapeCSV(item.description)},${item.amount}")
                }
                report.otherIncomeItems.forEach { item ->
                    writer.appendLine("${escapeCSV(item.description)},${item.amount}")
                }
                writer.appendLine()
            }
            
            // --- DETAIL PENGELUARAN ---
            writer.appendLine("=== DETAIL BEBAN (PENGELUARAN) ===")
            if (report.expensesByCategory.isEmpty()) {
                writer.appendLine("Tidak ada data pengeluaran")
            } else {
                writer.appendLine("Kategori Pengeluaran,Deskripsi Pengeluaran,Jumlah")
                report.expensesByCategory.forEach { category ->
                    if (category.items.isEmpty()) {
                        writer.appendLine("${escapeCSV(category.category)},-,${category.amount}")
                    } else {
                        category.items.forEach { detail ->
                            writer.appendLine("${escapeCSV(category.category)},${escapeCSV(detail.description)},${detail.amount}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Export daily revenue data to CSV.
     */
    fun exportDailyRevenueToCsv(
        context: Context,
        data: List<DailyRevenue>,
        title: String = "Pendapatan Harian"
    ): Result<Uri> {
        return exportToCsv(context, "Pendapatan_Harian") { writer ->
            writer.appendLine(title)
            writer.appendLine("Dibuat,${dateFormat.format(Date())}")
            writer.appendLine()
            
            // Header row
            writer.appendLine("Tanggal,Pendapatan,Jumlah Transaksi")
            
            // Data rows
            data.forEach { item ->
                writer.appendLine("${dateFormat.format(Date(item.date))},${item.revenue},${item.transactionCount}")
            }
            
            // Summary
            writer.appendLine()
            writer.appendLine("Total,${data.sumOf { it.revenue }},${data.sumOf { it.transactionCount }}")
        }
    }

    /**
     * Export top products to CSV.
     */
    fun exportTopProductsToCsv(
        context: Context,
        products: List<TopProduct>,
        periodName: String
    ): Result<Uri> {
        return exportToCsv(context, "Produk_Terlaris") { writer ->
            writer.appendLine("PRODUK TERLARIS")
            writer.appendLine("Periode,$periodName")
            writer.appendLine("Dibuat,${dateFormat.format(Date())}")
            writer.appendLine()
            
            // Header row
            writer.appendLine("Peringkat,Nama Produk,Jumlah Terjual,Total Pendapatan")
            
            // Data rows
            products.forEachIndexed { index, product ->
                writer.appendLine("${index + 1},${escapeCSV(product.productName)},${product.quantitySold},${product.totalRevenue}")
            }
        }
    }

    // ==================== TEMPLATE METHOD ====================

    /**
     * Common CSV export template. Handles file creation, UTF-8 BOM,
     * FileProvider URI generation, and error handling.
     *
     * @param context Android context for file operations
     * @param filePrefix Prefix for the generated filename (timestamp is appended)
     * @param writeContent Lambda that writes the CSV content to the FileWriter
     */
    private fun exportToCsv(
        context: Context,
        filePrefix: String,
        writeContent: (FileWriter) -> Unit
    ): Result<Uri> {
        return try {
            val fileName = "${filePrefix}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // UTF-8 BOM for Excel compatibility
                writer.write("\uFEFF")
                writeContent(writer)
            }
            
            // Try attempting to copy to public Downloads directly
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val downloadsUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (downloadsUri != null) {
                        resolver.openOutputStream(downloadsUri)?.use { outStream ->
                            file.inputStream().use { inStream -> inStream.copyTo(outStream) }
                        }
                    }
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val destFile = File(downloadsDir, fileName)
                    file.copyTo(destFile, overwrite = true)
                }
            } catch (e: Exception) {
                Timber.w(e, "Gagal mengkopi file csv ke folder Downloads public, menggunakan versi Share saja")
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Timber.d("CSV exported successfully: ${file.absolutePath}")
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export CSV: $filePrefix")
            Result.failure(e)
        }
    }

    /**
     * Escape special characters for CSV format.
     */
    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Create an intent to share the CSV file.
     */
    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

