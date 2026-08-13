package com.cajasimple.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.cajasimple.app.domain.model.ConfirmedSale
import com.cajasimple.app.domain.usecase.SaleEngine
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DailyPdf {
    fun create(context: Context, businessName: String, date: LocalDate, sales: List<ConfirmedSale>): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 11f }
        val titlePaint = Paint(paint).apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD }
        var y = 50f

        fun newPageIfNeeded(required: Float = 90f) {
            if (y + required < pageHeight - 40) return
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = 45f
        }
        fun line(text: String, bold: Boolean = false, indent: Float = 0f) {
            newPageIfNeeded(20f)
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(text.take(92), 42f + indent, y, paint)
            y += 17f
        }

        canvas.drawText(businessName, 42f, y, titlePaint); y += 30f
        line("Reporte diario · ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
        line("Generado: ${java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")
        y += 12f
        sales.sortedBy { it.createdAt }.forEachIndexed { index, sale ->
            newPageIfNeeded()
            val time = Instant.ofEpochMilli(sale.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
            line("Venta ${index + 1} · $time · ${SaleEngine.formatMoney(sale.totalAmount)}", bold = true)
            sale.items.forEach { item ->
                val detail = item.description.ifBlank { "Sin detalle" }
                line("$detail — ${item.quantity} × ${SaleEngine.formatMoney(item.unitPrice)} = ${SaleEngine.formatMoney(item.subtotal)}", indent = 12f)
            }
            line("Recibido ${SaleEngine.formatMoney(sale.receivedAmount)} · Vuelto ${SaleEngine.formatMoney(sale.changeAmount)}", indent = 12f)
            y += 8f
        }
        y += 8f
        line("Cantidad de ventas: ${sales.size}", bold = true)
        line("Total vendido: ${SaleEngine.formatMoney(sales.sumOf { it.totalAmount })}", bold = true)
        document.finishPage(page)

        val directory = File(context.cacheDir, "reports").apply { mkdirs() }
        val output = File(directory, "ventas-$date.pdf")
        FileOutputStream(output).use(document::writeTo)
        document.close()
        return output
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte de ventas"))
    }
}

