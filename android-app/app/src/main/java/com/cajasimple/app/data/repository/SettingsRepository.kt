package com.cajasimple.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cajasimple.app.domain.model.BusinessSettings
import com.cajasimple.app.domain.model.CashMode
import com.cajasimple.app.domain.model.ResetButtonStyle
import com.cajasimple.app.domain.model.ThemeMode
import com.cajasimple.app.domain.model.VisualTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("business_settings")

class SettingsRepository(private val context: Context) {
    private val businessName = stringPreferencesKey("business_name")
    private val selectedMode = stringPreferencesKey("selected_mode")
    private val selectedTheme = stringPreferencesKey("selected_theme")
    private val selectedThemeMode = stringPreferencesKey("selected_theme_mode")
    private val resetButtonStyle = stringPreferencesKey("reset_button_style")
    private val productQuickPrices = stringPreferencesKey("product_quick_prices")
    private val paymentQuickAmounts = stringPreferencesKey("payment_quick_amounts")

    val settings: Flow<BusinessSettings> = context.settingsStore.data.map { prefs ->
        BusinessSettings(
            businessName = prefs[businessName].orEmpty(),
            mode = prefs[selectedMode]?.let { runCatching { CashMode.valueOf(it) }.getOrNull() } ?: CashMode.GUIDED,
            theme = prefs[selectedTheme]?.let { runCatching { VisualTheme.valueOf(it) }.getOrNull() } ?: VisualTheme.RED_BLACK,
            themeMode = prefs[selectedThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            resetButtonStyle = prefs[resetButtonStyle]?.let { runCatching { ResetButtonStyle.valueOf(it) }.getOrNull() }
                ?: ResetButtonStyle.ICON_ONLY,
            productQuickPrices = prefs[productQuickPrices]?.toAmounts() ?: DEFAULT_PRODUCT_PRICES,
            paymentQuickAmounts = prefs[paymentQuickAmounts]?.toAmounts() ?: emptyList(),
        )
    }

    suspend fun setBusinessName(value: String) = context.settingsStore.edit { it[businessName] = value.trim().take(60) }
    suspend fun setMode(value: CashMode) = context.settingsStore.edit { it[selectedMode] = value.name }
    suspend fun setTheme(value: VisualTheme) = context.settingsStore.edit { it[selectedTheme] = value.name }
    suspend fun setThemeMode(value: ThemeMode) = context.settingsStore.edit { it[selectedThemeMode] = value.name }
    suspend fun setResetButtonStyle(value: ResetButtonStyle) = context.settingsStore.edit { it[resetButtonStyle] = value.name }
    suspend fun addProductQuickPrice(amount: Long) = updateAmounts(productQuickPrices, amount, add = true)
    suspend fun removeProductQuickPrice(amount: Long) = updateAmounts(productQuickPrices, amount, add = false)
    suspend fun addPaymentQuickAmount(amount: Long) = updateAmounts(paymentQuickAmounts, amount, add = true)
    suspend fun removePaymentQuickAmount(amount: Long) = updateAmounts(paymentQuickAmounts, amount, add = false)

    private suspend fun updateAmounts(key: androidx.datastore.preferences.core.Preferences.Key<String>, amount: Long, add: Boolean) {
        if (amount <= 0) return
        context.settingsStore.edit { prefs ->
            val defaults = if (key == productQuickPrices) DEFAULT_PRODUCT_PRICES else emptyList()
            val current = prefs[key]?.toAmounts() ?: defaults
            val updated = if (add) (current + amount).distinct().sorted() else current.filterNot { it == amount }
            prefs[key] = updated.joinToString(",")
        }
    }

    private fun String.toAmounts(): List<Long> = split(',')
        .mapNotNull { it.toLongOrNull() }
        .filter { it > 0 }
        .distinct()
        .sorted()

    companion object {
        private val DEFAULT_PRODUCT_PRICES = listOf(18_000L, 20_000L, 25_000L, 30_000L)
    }
}
