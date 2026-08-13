package com.cajasimple.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cajasimple.app.data.local.CajaDatabase
import com.cajasimple.app.data.repository.SalesRepository
import com.cajasimple.app.domain.model.DraftItem
import com.cajasimple.app.domain.model.SaleDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class SalesDaoTest {
    private lateinit var database: CajaDatabase
    private lateinit var repository: SalesRepository

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CajaDatabase::class.java).allowMainThreadQueries().build()
        repository = SalesRepository(database.salesDao())
    }

    @After fun close() = database.close()

    @Test fun guardaYRecuperaBorradorSinContarloComoVenta() = runTest {
        val draft = SaleDraft(items = listOf(DraftItem(description = "Pan", unitPrice = 2_500)))
        repository.saveDraft(draft)
        assertEquals(draft, repository.loadDraft())
        assertEquals(0, repository.observeDay(LocalDate.now()).first().size)
    }

    @Test fun confirmaAtomicamenteYPrefiereElUuidParaEvitarDuplicados() = runTest {
        val draft = SaleDraft(items = listOf(DraftItem(unitPrice = 10_000)), receivedAmount = 20_000, paymentEntered = true)
        assertNotNull(repository.confirm(draft))
        assertNull(repository.confirm(draft))
        assertEquals(1, repository.observeDay(LocalDate.now()).first().size)
    }

    @Test fun conservaLaVentaPendienteHastaQueElServidorLaConfirma() = runTest {
        val draft = SaleDraft(items = listOf(DraftItem(unitPrice = 18_000)), receivedAmount = 20_000, paymentEntered = true)
        val sale = requireNotNull(repository.confirm(draft))

        assertEquals(listOf(sale.id), repository.pendingSales().map { it.id })
        repository.markSynced(sale.id)
        assertEquals(emptyList<String>(), repository.pendingSales().map { it.id })
        assertEquals(1, repository.observeDay(LocalDate.now()).first().size)
    }
}
