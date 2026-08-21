package com.cajasimple.app.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.cajasimple.app.MainActivity
import org.junit.Rule
import org.junit.Test

class CashFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun dineroInsuficienteMuestraFaltanteYBloqueaCobro() {
        resetSale()
        compose.onNodeWithTag("guided-price").performClick().performTextInput("50000")
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Ir a cobrar").assertIsDisplayed().performClick()
        compose.onNodeWithTag("received").performTextInput("30000")
        compose.onNodeWithText("El dinero no alcanza · faltan $20.000").assertIsDisplayed()
        compose.onNodeWithTag("received").performImeAction()
        compose.onNodeWithTag("received").assertIsNotFocused()
        compose.onNodeWithText("Cobrar").assertIsNotEnabled()
    }

    @Test fun ventaCompletaSeRegistraAunqueNoHayaInternet() {
        resetSale()
        compose.onNodeWithTag("guided-price").performClick().performTextInput("18000")
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Ir a cobrar").performClick()
        compose.onNodeWithTag("received").performTextInput("20000")
        compose.onNodeWithTag("received").performImeAction()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("Venta registrada").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Venta registrada").assertIsDisplayed()
        compose.onNodeWithText("$2.000").assertIsDisplayed()
    }

    @Test fun precioPreestablecidoNoEntraEnEdicionHastaTocarElCampo() {
        resetSale()
        val priceField = compose.onNodeWithTag("guided-price")

        priceField.assertIsNotFocused()
        compose.onAllNodesWithTag("quick-amount").onFirst().performClick()
        priceField.assertIsNotFocused()

        priceField.performClick()
        priceField.assertIsFocused()

        compose.onAllNodesWithTag("quick-amount").onFirst().performClick()
        priceField.assertIsNotFocused()
    }

    @Test fun cobrarManualSigueDisponibleSiSeBajaElTecladoSinUsarHecho() {
        resetSale()
        compose.onAllNodesWithTag("quick-amount").onFirst().performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Ir a cobrar").performClick()
        compose.onNodeWithTag("received").performTextInput("20000")

        closeSoftKeyboard()
        compose.onNodeWithText("Cobrar")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("Venta registrada").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Venta registrada").assertIsDisplayed()
    }

    @Test fun flechasRecorrenLosPasosEntreProductosYPermitenRehacer() {
        resetSale()
        compose.onNodeWithTag("guided-back").assertIsNotEnabled()
        compose.onNodeWithTag("guided-forward").assertIsNotEnabled()

        compose.onNodeWithTag("guided-price").performClick().performTextInput("18000")
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Cantidad").assertIsDisplayed()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Agregar otro producto").performClick()
        compose.onAllNodesWithTag("quick-amount").onFirst().performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Cantidad").assertIsDisplayed()

        compose.onNodeWithTag("guided-back").performClick()
        compose.onNodeWithTag("guided-price").assertIsDisplayed()
        compose.onNodeWithTag("guided-back").performClick()
        compose.onNodeWithText("Agregar otro producto").assertIsDisplayed()
        compose.onNodeWithTag("guided-back").performClick()
        compose.onNodeWithText("Cantidad").assertIsDisplayed()
        compose.onNodeWithTag("guided-back").performClick()
        compose.onNodeWithTag("guided-price").assertIsDisplayed()

        compose.onNodeWithTag("guided-forward").performClick()
        compose.onNodeWithText("Cantidad").assertIsDisplayed()
        compose.onNodeWithTag("guided-forward").performClick()
        compose.onNodeWithText("Agregar otro producto").assertIsDisplayed()
        compose.onNodeWithTag("guided-forward").performClick()
        compose.onNodeWithTag("guided-price").assertIsDisplayed()
        compose.onNodeWithTag("guided-forward").performClick()
        compose.onNodeWithText("Cantidad").assertIsDisplayed()
        compose.onNodeWithTag("guided-forward").assertIsNotEnabled()
    }

    private fun resetSale() {
        compose.onNodeWithTag("reset-sale").performClick()
        compose.onNodeWithText("Sí").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("guided-price").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
