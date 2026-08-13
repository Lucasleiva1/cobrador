package com.cajasimple.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cajasimple.app.data.repository.SettingsRepository
import com.cajasimple.app.domain.model.BusinessSettings
import com.cajasimple.app.domain.model.CashMode
import com.cajasimple.app.domain.model.ResetButtonStyle
import com.cajasimple.app.domain.model.ThemeMode
import com.cajasimple.app.domain.model.VisualTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<BusinessSettings> = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, BusinessSettings())
    fun setName(value: String) { viewModelScope.launch { repository.setBusinessName(value) } }
    fun setMode(value: CashMode) { viewModelScope.launch { repository.setMode(value) } }
    fun setTheme(value: VisualTheme) { viewModelScope.launch { repository.setTheme(value) } }
    fun setThemeMode(value: ThemeMode) { viewModelScope.launch { repository.setThemeMode(value) } }
    fun setResetButtonStyle(value: ResetButtonStyle) { viewModelScope.launch { repository.setResetButtonStyle(value) } }
    fun addProductQuickPrice(amount: Long) { viewModelScope.launch { repository.addProductQuickPrice(amount) } }
    fun removeProductQuickPrice(amount: Long) { viewModelScope.launch { repository.removeProductQuickPrice(amount) } }
    fun addPaymentQuickAmount(amount: Long) { viewModelScope.launch { repository.addPaymentQuickAmount(amount) } }
    fun removePaymentQuickAmount(amount: Long) { viewModelScope.launch { repository.removePaymentQuickAmount(amount) } }
}
