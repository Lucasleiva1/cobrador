package com.cajasimple.app.worker

import com.cajasimple.app.domain.model.ConfirmedSale
import com.cajasimple.app.domain.model.DraftItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DailySalesCsvTest {
    @Test
    fun `agrupa cada venta y muestra pesos argentinos legibles`() {
        val zone = ZoneId.of("America/Argentina/Buenos_Aires")
        val createdAt = ZonedDateTime.of(2026, 8, 13, 17, 49, 3, 0, zone).toInstant().toEpochMilli()
        val sale = ConfirmedSale(
            id = "identificador-interno-que-no-debe-mostrarse",
            createdAt = createdAt,
            totalAmount = 63_000,
            receivedAmount = 80_000,
            changeAmount = 17_000,
            items = listOf(
                DraftItem(unitPrice = 20_000, quantity = 1),
                DraftItem(unitPrice = 25_000, quantity = 1),
                DraftItem(unitPrice = 18_000, quantity = 1),
            ),
        )

        assertEquals(
            "Venta,Fecha,Hora,Qué se vendió,Total,Pagó con,Vuelto\n" +
                "1,13/08/2026,17:49:03,\"1 de $20.000 | 1 de $25.000 | 1 de $18.000\",63.000 pesos,80.000 pesos,17.000 pesos\n",
            DailySalesCsv.create(listOf(sale), zone),
        )
    }
}
