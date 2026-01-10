package com.zatiaras.pos.feature.reports.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import com.zatiaras.pos.feature.reports.domain.model.TopProduct
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for exporting reports to CSV format (compatible with Excel).
 */
@Singleton
class CsvExportService @Inject constructor() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
    private val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    /**
     * Export P&L report to CSV.
     */
    fun exportPnlToCsv(
        context: Context,
        report: ProfitLossReport,
        periodName: String
    ): Result<Uri> {
        return try {
            val fileName = "Laporan_Laba_Rugi_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // Header
                writer.appendLine("LAPORAN LABA RUGI")
                writer.appendLine("Periode,$periodName")
                writer.appendLine("Dibuat,${dateFormat.format(Date())}")
                writer.appendLine("Jumlah Transaksi,${report.transactionCount}")
                writer.appendLine()
                
                // P&L Data
                writer.appendLine("Kategori,Jumlah")
                writer.appendLine("Pendapatan Kotor,${report.grossRevenue}")
                writer.appendLine("Diskon,${report.totalDiscount}")
                writer.appendLine("Pendapatan Bersih,${report.netRevenue}")
                writer.appendLine("PPN (11%),${report.totalTax}")
                writer.appendLine("Total Penerimaan,${report.grandTotal}")
                
                if (report.estimatedCost > 0) {
                    writer.appendLine()
                    writer.appendLine("Harga Pokok Penjualan,${report.estimatedCost}")
                    writer.appendLine("Laba Kotor,${report.grossProfit}")
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Timber.d("CSV exported successfully: ${file.absolutePath}")
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export CSV")
            Result.failure(e)
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
        return try {
            val fileName = "Pendapatan_Harian_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
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
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Timber.d("Daily revenue CSV exported: ${file.absolutePath}")
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export daily revenue CSV")
            Result.failure(e)
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
        return try {
            val fileName = "Produk_Terlaris_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
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
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Timber.d("Top products CSV exported: ${file.absolutePath}")
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export top products CSV")
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
