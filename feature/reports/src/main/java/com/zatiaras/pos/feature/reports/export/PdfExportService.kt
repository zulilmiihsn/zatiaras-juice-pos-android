package com.zatiaras.pos.feature.reports.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.zatiaras.pos.core.domain.util.LocaleUtils
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
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
        color = Color.rgb(102, 126, 234) // Primary Blue
        textSize = 14f
        isFakeBoldText = true
    }
    
    private val expenseHeaderPaint = Paint().apply {
        color = Color.rgb(239, 68, 68) // ErrorRed
        textSize = 14f
        isFakeBoldText = true
    }
    
    private val incomeHeaderPaint = Paint().apply {
        color = Color.rgb(16, 185, 129) // SuccessGreen
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
        color = Color.rgb(239, 68, 68) // ErrorRed
        textSize = 12f
    }
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", LocaleUtils.LOCALE_ID)
    private val currencyFormat: NumberFormat = CurrencyFormatter.getCurrencyFormatter()
    
    private var currentPage: PdfDocument.Page? = null
    private var currentPageNum = 0

    /**
     * Export P&L report to PDF and return the file URI.
     */
    fun exportToPdf(
        context: Context,
        report: ProfitLossReport,
        periodName: String,
        storeLogoUri: String? = null
    ): Result<Uri> {
        return try {
            val document = PdfDocument()
            currentPageNum = 0
            
            // Start first page
            currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++currentPageNum).create()
            )
            
            drawPage(context, document, currentPage!!.canvas, report, periodName, storeLogoUri)
            
            // Finish last page
            document.finishPage(currentPage!!)
            currentPage = null
            
            // Always save to cache directory first to guarantee the file is created for sharing
            val fileName = "Laporan_Laba_Rugi_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            
            document.close()
            
            // Try attempting to copy to public Downloads directly
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
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
                Timber.w(e, "Gagal mengkopi file pdf ke folder Downloads public, menggunakan versi Share saja")
            }
            
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

    /**
     * Check if we need a new page and start one if necessary.
     */
    private fun checkAndStartNewPage(document: PdfDocument, yPos: Float): Pair<Canvas, Float> {
        // If yPos exceeds threshold, start new page
        if (yPos > pageHeight - margin - 60f) {
            // Finish current page
            document.finishPage(currentPage!!)
            
            // Start new page
            currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++currentPageNum).create()
            )
            
            // Return new canvas with reset yPos
            return currentPage!!.canvas to (margin + 40f)
        }
        
        return currentPage!!.canvas to yPos
    }
    
    private fun drawPage(context: Context, document: PdfDocument, canvas: Canvas, report: ProfitLossReport, periodName: String, storeLogoUri: String?) {
        var currentCanvas = canvas
        
        // Brand Pink color reference
        val brandPink = Color.rgb(236, 72, 153) // Brand500
        val brandPinkDark = Color.rgb(219, 39, 119) // Brand600
        
        // --- 1. Top Pink Accent Stripe ---
        val accentPaint = Paint().apply {
            color = brandPink
            style = Paint.Style.FILL
        }
        currentCanvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, accentPaint)
        
        // --- 2. Title Area ---
        var yPos = margin + 50f
        
        // Title text in bold black
        val titleBlackPaint = Paint().apply {
            color = Color.BLACK
            textSize = 26f
            isFakeBoldText = true
        }
        currentCanvas.drawText("LAPORAN LABA RUGI", margin, yPos, titleBlackPaint)
        
        // Try drawing the store logo on the right
        var logoDrawn = false
        if (!storeLogoUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(storeLogoUri)
                val isContentUri = uri.scheme == "content" || uri.scheme == "file"
                
                val bitmap: Bitmap? = if (isContentUri) {
                    val stream = context.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(stream)
                } else null
                
                if (bitmap != null) {
                    val targetHeight = 50f
                    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val targetWidth = targetHeight * aspectRatio
                    
                    val destRect = Rect(
                        (pageWidth - margin - targetWidth).toInt(),
                        (yPos - 35f).toInt(),
                        (pageWidth - margin).toInt(),
                        (yPos + 15f).toInt()
                    )
                    
                    val paint = Paint()
                    paint.isAntiAlias = true
                    paint.isFilterBitmap = true
                    
                    currentCanvas.drawBitmap(bitmap, null, destRect, paint)
                    logoDrawn = true
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                Timber.w(e, "Gagal meload storeLogoUri untuk PDF export: $storeLogoUri")
            }
        }
        
        // --- 3. Pink divider line under the title ---
        yPos += 18f
        val pinkLinePaint = Paint().apply {
            color = brandPinkDark
            strokeWidth = 2.5f
        }
        currentCanvas.drawLine(margin, yPos, pageWidth - margin, yPos, pinkLinePaint)
        
        // Setup spacing for the subtitle
        yPos += 28f
        
        // Subtitle - Period
        labelPaint.textSize = 12f
        currentCanvas.drawText("Periode: $periodName", margin, yPos, labelPaint)
        yPos += lineHeight * 1.2f
        
        // Generated date
        currentCanvas.drawText("Dibuat: ${dateFormat.format(Date())}", margin, yPos, labelPaint)
        yPos += lineHeight * 1.2f
        
        // Transaction count
        currentCanvas.drawText("Jumlah Transaksi: ${report.transactionCount}", margin, yPos, labelPaint)
        yPos += lineHeight * 2f
        
        // Divider line
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate200 (Subtle gray)
            strokeWidth = 2f
        }
        currentCanvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += lineHeight
        
        // === PENDAPATAN SECTION ===
        val (canvas1, yPos1) = checkAndStartNewPage(document, yPos)
        currentCanvas = canvas1
        yPos = yPos1
        
        currentCanvas.drawText("PENDAPATAN", margin, yPos, incomeHeaderPaint)
        yPos += lineHeight * 1.2f
        
        val result2 = drawLineItemWithPagination(document, currentCanvas, "Pendapatan Operasional", report.operatingRevenue, yPos, false)
        currentCanvas = result2.first
        yPos = result2.second
        
        val result3 = drawLineItemWithPagination(document, currentCanvas, "Pendapatan Lainnya", report.otherRevenue, yPos, false)
        currentCanvas = result3.first
        yPos = result3.second
        
        yPos += lineHeight * 0.5f
        currentCanvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += lineHeight
        
        val result4 = drawLineItemWithPagination(document, currentCanvas, "Total Pendapatan", report.grossRevenue, yPos, false, true)
        currentCanvas = result4.first
        yPos = result4.second
        yPos += lineHeight
        
        // === BEBAN SECTION ===
        val (canvas5, yPos5) = checkAndStartNewPage(document, yPos)
        currentCanvas = canvas5
        yPos = yPos5
        
        currentCanvas.drawText("BEBAN", margin, yPos, expenseHeaderPaint)
        yPos += lineHeight * 1.2f
        
        val result6 = drawLineItemWithPagination(document, currentCanvas, "Beban Operasional", -report.operatingExpenses, yPos, true)
        currentCanvas = result6.first
        yPos = result6.second
        
        val result7 = drawLineItemWithPagination(document, currentCanvas, "Beban Lainnya", -report.otherExpenses, yPos, true)
        currentCanvas = result7.first
        yPos = result7.second
        
        yPos += lineHeight * 0.5f
        currentCanvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += lineHeight
        
        val result8 = drawLineItemWithPagination(document, currentCanvas, "Total Beban", -report.totalExpenses, yPos, true, true)
        currentCanvas = result8.first
        yPos = result8.second
        yPos += lineHeight
        
        // === LABA SECTION ===
        val (canvas9, yPos9) = checkAndStartNewPage(document, yPos)
        currentCanvas = canvas9
        yPos = yPos9
        
        currentCanvas.drawText("RINGKASAN LABA", margin, yPos, headerPaint)
        yPos += lineHeight        // Profit/Tax
        yPos -= 10f
        val result10 = drawLineItemWithPagination(document, currentCanvas, "Laba Kotor", report.grossProfit, yPos, true)
        currentCanvas = result10.first; yPos = result10.second
        
        val result11 = drawLineItemWithPagination(document, currentCanvas, "Pajak (${report.taxPercentage}%)", -report.tax, yPos, true)
        currentCanvas = result11.first; yPos = result11.second
        
        yPos += lineHeight
        
        // Ensure there's space for the profit box
        val (canvasFinal, yPosFinal) = checkAndStartNewPage(document, yPos + 70f)
        currentCanvas = canvasFinal
        yPos = yPosFinal
        
        // Draw net profit box
        val isProfit = report.netProfit >= 0
        // Profit green = 16, 185, 129, Loss red = Brand50 background / ErrorRed Foreground
        val boxPaint = Paint().apply {
            color = if (isProfit) Color.rgb(209, 250, 229) else Color.rgb(253, 242, 248) 
            style = Paint.Style.FILL
        }
        // Make the rect slightly taller and rounded
        currentCanvas.drawRoundRect(
            margin, yPos - 25f, 
            pageWidth - margin, yPos + 35f, 
            8f, 8f, 
            boxPaint
        )
        
        val profitPaint = Paint().apply {
            color = if (isProfit) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
            textSize = 16f
            isFakeBoldText = true
        }
        
        currentCanvas.drawText(if (isProfit) "Laba Bersih" else "Rugi Bersih", margin + 15f, yPos + 12f, profitPaint)
        val profitValue = currencyFormat.format(report.netProfit)
        val profitWidth = profitPaint.measureText(profitValue)
        currentCanvas.drawText(profitValue, pageWidth - margin - 15f - profitWidth, yPos + 12f, profitPaint)
        
        // Footer (Bottom center of last page)
        val footerY = pageHeight - margin
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184) // Slate400
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        currentCanvas.drawText("ZatiarasPOS - Laporan Keuangan", pageWidth / 2f, footerY, footerPaint)
    }
    
    /**
     * Draw line item with automatic pagination support.
     */
    private fun drawLineItemWithPagination(
        document: PdfDocument,
        canvas: Canvas,
        label: String,
        amount: Long,
        yPos: Float,
        isNegative: Boolean,
        isBold: Boolean = false
    ): Pair<Canvas, Float> {
        val (newCanvas, newYPos) = checkAndStartNewPage(document, yPos)
        val finalYPos = drawLineItem(newCanvas, label, amount, newYPos, isNegative, isBold)
        return newCanvas to finalYPos
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
