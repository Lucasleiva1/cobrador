package com.cajasimple.app.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import com.cajasimple.app.MainActivity
import org.junit.Rule
import org.junit.Test

class CashFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun dineroInsuficienteMuestraFaltanteYBloqueaCobro() {
        compose.onNodeWithTag("guided-price").performTextInput("50000")
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Ir a cobrar").assertIsDisplayed().performClick()
        compose.onNodeWithTag("received").performTextInput("30000")
        compose.onNodeWithText("El dinero no alcanza · faltan $20.000").assertIsDisplayed()
        compose.onNodeWithText("Cobrar").assertIsNotEnabled()
    }
}
