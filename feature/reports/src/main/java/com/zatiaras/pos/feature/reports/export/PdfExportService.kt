package com.zatiaras.pos.feature.reports.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for exporting P&L reports to PDF format.
 * Uses Android's built-in PdfDocument API.
 */
@Singleton
class PdfExportService @Inject constructor() {

    private val pageWidth = 595  // A4 width in points (72 dpi)
    private val pageHeight = 842 // A4 height in points
    private val margin = 40f
    private val lineHeight = 24f
    
    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isFakeBoldText = true
    }
    
    private val headerPaint = Paint().apply {
        color = Color.rgb(102, 126, 234) // Primary color
        textSize = 14f
        isFakeBoldText = true
    }
    
    private val labelPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 12f
    }
    
    private val valuePaint = Paint().apply {
        color = Color.BLACK
        textSize = 12f
    }
    
    private val totalPaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        isFakeBoldText = true
    }
    
    private val negativePaint = Paint().apply {
        color = Color.rgb(229, 57, 53) // Red for negative values
        textSize = 12f
    }
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    /**
     * Export P&L report to PDF and return the file URI.
     */
    fun exportToPdf(
        context: Context,
        report: ProfitLossReport,
        periodName: String
    ): Result<Uri> {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            
            drawPage(page.canvas, report, periodName)
            
            document.finishPage(page)
            
            // Save to cache directory
            val fileName = "Laporan_Laba_Rugi_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            
            document.close()
            
            // Get URI via FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Timber.d("PDF exported successfully: ${file.absolutePath}")
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export PDF")
            Result.failure(e)
        }
    }

    private fun drawPage(canvas: Canvas, report: ProfitLossReport, periodName: String) {
        var yPos = margin + 40f
        
        // Title
        canvas.drawText("LAPORAN LABA RUGI", margin, yPos, titlePaint)
        yPos += lineHeight * 1.5f
        
        // Subtitle - Period
        labelPaint.textSize = 11f
        canvas.drawText("Periode: $periodName", margin, yPos, labelPaint)
        yPos += lineHeight * 0.8f
        
        // Generated date
        canvas.drawText("Dibuat: ${dateFormat.format(Date())}", margin, yPos, labelPaint)
        yPos += lineHeight * 0.8f
        
        // Transaction count
        canvas.drawText("Jumlah Transaksi: ${report.transactionCount}", margin, yPos, labelPaint)
        yPos += lineHeight * 2f
        
        // Divider line
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += lineHeight
        
        // === PENDAPATAN SECTION ===
        canvas.drawText("PENDAPATAN", margin, yPos, headerPaint)
        yPos += lineHeight * 1.2f
        
        yPos = drawLineItem(canvas, "Pendapatan Kotor", report.grossRevenue, yPos, false)
        yPos = drawLineItem(canvas, "Diskon", -report.totalDiscount, yPos, true)
        
        yPos += lineHeight * 0.5f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += lineHeight
        
        yPos = drawLineItem(canvas, "Pendapatan Bersih", report.netRevenue, yPos, false, true)
        yPos += lineHeight
        
        // === PAJAK SECTION ===
        canvas.drawText("PAJAK", margin, yPos, headerPaint)
        yPos += lineHeight * 1.2f
        
        yPos = drawLineItem(canvas, "PPN (11%)", report.totalTax, yPos, false)
        yPos += lineHeight
        
        // === TOTAL SECTION ===
        canvas.drawText("TOTAL PENERIMAAN", margin, yPos, headerPaint)
        yPos += lineHeight * 1.2f
        
        // Draw total box
        val boxPaint = Paint().apply {
            color = Color.rgb(240, 240, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRect(margin, yPos - 20f, pageWidth - margin, yPos + 30f, boxPaint)
        
        canvas.drawText("Total Diterima", margin + 10f, yPos + 10f, totalPaint)
        val totalValue = currencyFormat.format(report.grandTotal)
        val totalWidth = totalPaint.measureText(totalValue)
        canvas.drawText(totalValue, pageWidth - margin - 10f - totalWidth, yPos + 10f, totalPaint)
        
        yPos += lineHeight * 3f
        
        // === ESTIMASI LABA SECTION (if cost data available) ===
        if (report.estimatedCost > 0) {
            canvas.drawText("ESTIMASI LABA", margin, yPos, headerPaint)
            yPos += lineHeight * 1.2f
            
            yPos = drawLineItem(canvas, "Harga Pokok Penjualan", -report.estimatedCost, yPos, true)
            
            yPos += lineHeight * 0.5f
            canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
            yPos += lineHeight
            
            val isProfit = report.grossProfit >= 0
            val profitBoxPaint = Paint().apply {
                color = if (isProfit) Color.rgb(232, 245, 233) else Color.rgb(255, 235, 238)
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin, yPos - 20f, pageWidth - margin, yPos + 30f, profitBoxPaint)
            
            val profitPaint = Paint().apply {
                color = if (isProfit) Color.rgb(46, 125, 50) else Color.rgb(198, 40, 40)
                textSize = 14f
                isFakeBoldText = true
            }
            
            canvas.drawText("Laba Kotor", margin + 10f, yPos + 10f, profitPaint)
            val profitValue = currencyFormat.format(report.grossProfit)
            val profitWidth = profitPaint.measureText(profitValue)
            canvas.drawText(profitValue, pageWidth - margin - 10f - profitWidth, yPos + 10f, profitPaint)
        }
        
        // Footer
        val footerY = pageHeight - margin
        labelPaint.textSize = 10f
        labelPaint.color = Color.GRAY
        canvas.drawText("Zatiaras POS - Generated automatically", margin, footerY, labelPaint)
    }

    private fun drawLineItem(
        canvas: Canvas,
        label: String,
        amount: Long,
        yPos: Float,
        isNegative: Boolean,
        isBold: Boolean = false
    ): Float {
        val paint = when {
            isBold -> totalPaint
            isNegative && amount != 0L -> negativePaint
            else -> valuePaint
        }
        
        canvas.drawText(label, margin + 20f, yPos, if (isBold) totalPaint else labelPaint)
        
        val formattedValue = if (isNegative && amount != 0L) {
            "(${currencyFormat.format(kotlin.math.abs(amount))})"
        } else {
            currencyFormat.format(amount)
        }
        
        val valueWidth = paint.measureText(formattedValue)
        canvas.drawText(formattedValue, pageWidth - margin - valueWidth, yPos, paint)
        
        return yPos + lineHeight
    }

    /**
     * Create an intent to share the PDF file.
     */
    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
