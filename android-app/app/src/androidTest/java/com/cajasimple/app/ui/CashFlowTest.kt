package com.cajasimple.app.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.cajasimple.app.MainActivity
import org.junit.Rule
import org.junit.Test

class CashFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun dineroInsuficienteMuestraFaltanteYBloqueaCobro() {
        resetSale()
        compose.onNodeWithTag("guided-price").performTextInput("50000")
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Ir a cobrar").assertIsDisplayed().performClick()
        compose.onNodeWithTag("received").performTextInput("30000")
        compose.onNodeWithText("El dinero no alcanza · faltan $20.000").assertIsDisplayed()
        compose.onNodeWithText("Cobrar").assertIsNotEnabled()
    }

    @Test fun ventaCompletaSeRegistraAunqueNoHayaInternet() {
        resetSale()
        compose.onNodeWithTag("guided-price").performTextInput("18000")
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Ir a cobrar").performClick()
        compose.onNodeWithTag("received").performTextInput("20000")
        compose.onNodeWithTag("received").performImeAction()
        closeSoftKeyboard()
        compose.onNodeWithText("Cobrar").assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick) { click -> click() }
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("Venta registrada").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Venta registrada").assertIsDisplayed()
        compose.onNodeWithText("$2.000").assertIsDisplayed()
    }

    private fun resetSale() {
        compose.onNodeWithTag("reset-sale").performClick()
        compose.onNodeWithText("Sí").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("guided-price").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
