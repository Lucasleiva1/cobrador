package com.cajasimple.app.ui.screens.cash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cajasimple.app.data.repository.SalesRepository
import com.cajasimple.app.domain.model.ConfirmedSale
import com.cajasimple.app.domain.model.DraftItem
import com.cajasimple.app.domain.model.SaleDraft
import com.cajasimple.app.domain.usecase.SaleEngine
import com.cajasimple.app.worker.DriveBackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class CashUiState(
    val draft: SaleDraft = SaleDraft(),
    val lastConfirmed: ConfirmedSale? = null,
    val busy: Boolean = false,
    val notice: String? = null,
    val restored: Boolean = false,
    val guidedStep: GuidedStep = GuidedStep.PRICE,
)

enum class GuidedStep { PRICE, QUANTITY, DECISION }

class CashViewModel(private val repository: SalesRepository, private val appContext: Context) : ViewModel() {
    private val _state = MutableStateFlow(CashUiState())
    val state: StateFlow<CashUiState> = _state.asStateFlow()
    private val confirming = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            val draft = repository.loadDraft() ?: SaleDraft()
            val restoredStep = when {
                draft.paymentStep -> GuidedStep.DECISION
                draft.items.lastOrNull()?.unitPrice?.let { it > 0 } == true -> GuidedStep.QUANTITY
                else -> GuidedStep.PRICE
            }
            _state.value = _state.value.copy(draft = draft, restored = true, guidedStep = restoredStep)
        }
    }

    fun updateItem(id: String, description: String? = null, priceRaw: String? = null, quantity: Int? = null) = mutate { draft ->
        SaleEngine.updateItem(draft, id) { item -> item.copy(
            description = description ?: item.description,
            unitPrice = priceRaw?.let(SaleEngine::sanitizeMoney) ?: item.unitPrice,
            quantity = quantity?.coerceAtLeast(1) ?: item.quantity,
        ) }
    }

    fun addItem() = mutate(SaleEngine::addItem)
    fun addGuidedItem() {
        val current = _state.value
        if (current.guidedStep != GuidedStep.DECISION || current.draft.confirmed || current.busy) return
        _state.value = current.copy(
            draft = SaleEngine.addItem(current.draft),
            guidedStep = GuidedStep.PRICE,
            notice = null,
        )
        persist()
    }

    fun confirmGuidedPrice() {
        val current = _state.value
        if (current.draft.items.lastOrNull()?.unitPrice?.let { it > 0 } == true) {
            _state.value = current.copy(guidedStep = GuidedStep.QUANTITY, notice = null)
        }
    }

    fun confirmGuidedQuantity() {
        val current = _state.value
        if (current.draft.items.lastOrNull()?.valid == true) {
            _state.value = current.copy(guidedStep = GuidedStep.DECISION, notice = null)
        }
    }

    fun previousGuidedStep() {
        val current = _state.value
        val previous = when (current.guidedStep) {
            GuidedStep.PRICE -> return
            GuidedStep.QUANTITY -> GuidedStep.PRICE
            GuidedStep.DECISION -> GuidedStep.QUANTITY
        }
        _state.value = current.copy(guidedStep = previous, notice = null)
    }

    fun removeItem(id: String) = mutate { SaleEngine.removeItem(it, id) }
    fun setPayment(raw: String) = mutate { SaleEngine.enterPayment(it, raw) }
    fun goToPayment() = mutate { if (it.totalAmount > 0) it.copy(paymentStep = true) else it }
    fun editSale() = mutate { it.copy(paymentStep = false, receivedAmount = 0, paymentEntered = false) }

    fun confirm(showResult: Boolean = true, onDone: (() -> Unit)? = null) {
        val current = _state.value.draft
        if (!current.canConfirm || !confirming.compareAndSet(false, true)) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            val result = repository.confirm(current)
            if (result != null) {
                val confirmedDraft = current.copy(confirmed = true)
                _state.value = _state.value.copy(
                    draft = confirmedDraft,
                    lastConfirmed = if (showResult) result else null,
                    busy = false,
                    notice = if (showResult) null else "Venta anterior registrada",
                )
                DriveBackupWorker.enqueue(appContext)
                onDone?.invoke()
            } else _state.value = _state.value.copy(busy = false)
            confirming.set(false)
        }
    }

    fun newSale() {
        val current = _state.value.draft
        when {
            current.confirmed -> resetSale()
            current.canConfirm -> confirm(showResult = false, onDone = ::resetSaleKeepingNotice)
        }
    }

    fun clearNotice() { _state.value = _state.value.copy(notice = null) }

    private fun resetSale() {
        _state.value = CashUiState(draft = SaleDraft(id = UUID.randomUUID().toString()), restored = true)
        persist()
    }

    private fun resetSaleKeepingNotice() {
        val notice = _state.value.notice
        _state.value = CashUiState(draft = SaleDraft(id = UUID.randomUUID().toString()), notice = notice, restored = true)
        persist()
    }

    private fun mutate(transform: (SaleDraft) -> SaleDraft) {
        if (_state.value.draft.confirmed || _state.value.busy) return
        _state.value = _state.value.copy(draft = transform(_state.value.draft), notice = null)
        persist()
    }

    private fun persist() {
        val snapshot = _state.value.draft
        viewModelScope.launch { repository.saveDraft(snapshot) }
    }
}
