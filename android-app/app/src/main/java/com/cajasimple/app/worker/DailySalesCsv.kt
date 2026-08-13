package com.cajasimple.app.worker

import com.cajasimple.app.domain.model.ConfirmedSale
import com.cajasimple.app.domain.usecase.SaleEngine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DailySalesCsv {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun create(sales: List<ConfirmedSale>, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine("Venta,Fecha,Hora,Qué se vendió,Total,Pagó con,Vuelto")
        sales.forEachIndexed { index, sale ->
            val dateTime = Instant.ofEpochMilli(sale.createdAt).atZone(zone)
            val detail = sale.items.joinToString(" | ") { item ->
                buildString {
                    append(item.quantity)
                    append(" de ")
                    append(SaleEngine.formatMoney(item.unitPrice))
                    if (item.description.isNotBlank()) {
                        append(" (")
                        append(item.description.trim())
                        append(')')
                    }
                }
            }

            append(index + 1).append(',')
            append(dateTime.format(dateFormatter)).append(',')
            append(dateTime.format(timeFormatter)).append(',')
            append(csvCell(detail)).append(',')
            append(formatPesos(sale.totalAmount)).append(',')
            append(formatPesos(sale.receivedAmount)).append(',')
            append(formatPesos(sale.changeAmount)).appendLine()
        }
    }

    private fun formatPesos(amount: Long): String =
        "${SaleEngine.formatMoney(amount).removePrefix("$")} pesos"

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
