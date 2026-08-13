package com.cajasimple.app.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cajasimple.app.data.repository.SalesRepository
import com.cajasimple.app.domain.model.ConfirmedSale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SalesListViewModel(private val repository: SalesRepository) : ViewModel() {
    val selectedDate = MutableStateFlow(LocalDate.now())
    val sales: StateFlow<List<ConfirmedSale>> = selectedDate
        .flatMapLatest(repository::observeDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun previousDay() { selectedDate.value = selectedDate.value.minusDays(1) }
    fun nextDay() { if (selectedDate.value < LocalDate.now()) selectedDate.value = selectedDate.value.plusDays(1) }
    fun today() { selectedDate.value = LocalDate.now() }
}

