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
    val canNavigateBack: Boolean = false,
    val canNavigateForward: Boolean = false,
)

enum class GuidedStep { PRICE, QUANTITY, DECISION }

private data class GuidedHistoryEntry(
    val draft: SaleDraft,
    val step: GuidedStep,
)

class CashViewModel(private val repository: SalesRepository, private val appContext: Context) : ViewModel() {
    private val _state = MutableStateFlow(CashUiState())
    val state: StateFlow<CashUiState> = _state.asStateFlow()
    private val confirming = AtomicBoolean(false)
    private val guidedBackHistory = mutableListOf<GuidedHistoryEntry>()
    private val guidedForwardHistory = mutableListOf<GuidedHistoryEntry>()

    init {
        viewModelScope.launch {
            val draft = repository.loadDraft() ?: SaleDraft()
            val restoredStep = when {
                draft.paymentStep -> GuidedStep.DECISION
                draft.items.lastOrNull()?.unitPrice?.let { it > 0 } == true -> GuidedStep.QUANTITY
                else -> GuidedStep.PRICE
            }
            setState(_state.value.copy(draft = draft, restored = true, guidedStep = restoredStep))
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
        navigateGuided(current.copy(
            draft = SaleEngine.addItem(current.draft),
            guidedStep = GuidedStep.PRICE,
            notice = null,
        ))
    }

    fun confirmGuidedPrice() {
        val current = _state.value
        if (current.draft.items.lastOrNull()?.unitPrice?.let { it > 0 } == true) {
            navigateGuided(current.copy(guidedStep = GuidedStep.QUANTITY, notice = null))
        }
    }

    fun confirmGuidedQuantity() {
        val current = _state.value
        if (current.draft.items.lastOrNull()?.valid == true) {
            navigateGuided(current.copy(guidedStep = GuidedStep.DECISION, notice = null))
        }
    }

    fun navigateGuidedBack() {
        val current = _state.value
        if (current.draft.confirmed || current.busy || !canStepBack(current)) return
        guidedForwardHistory += current.toHistoryEntry()
        val previous = if (guidedBackHistory.isNotEmpty()) {
            guidedBackHistory.removeAt(guidedBackHistory.lastIndex)
        } else {
            fallbackPrevious(current) ?: return
        }
        setState(current.copy(draft = previous.draft, guidedStep = previous.step, notice = null))
        persist()
    }

    fun navigateGuidedForward() {
        val current = _state.value
        if (current.draft.confirmed || current.busy || guidedForwardHistory.isEmpty()) return
        guidedBackHistory += current.toHistoryEntry()
        val next = guidedForwardHistory.removeAt(guidedForwardHistory.lastIndex)
        setState(current.copy(draft = next.draft, guidedStep = next.step, notice = null))
        persist()
    }

    fun previousGuidedStep() = navigateGuidedBack()

    fun removeItem(id: String) = mutate { SaleEngine.removeItem(it, id) }
    fun setPayment(raw: String) = mutate { SaleEngine.enterPayment(it, raw) }
    fun goToPayment() {
        val current = _state.value
        if (current.draft.totalAmount > 0) {
            navigateGuided(current.copy(draft = current.draft.copy(paymentStep = true), notice = null))
        }
    }

    fun editSale() = navigateGuidedBack()

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

    fun resetCurrentSale() {
        if (_state.value.busy) return
        resetSale()
    }

    private fun resetSale() {
        clearGuidedHistory()
        setState(CashUiState(draft = SaleDraft(id = UUID.randomUUID().toString()), restored = true))
        persist()
    }

    private fun resetSaleKeepingNotice() {
        val notice = _state.value.notice
        clearGuidedHistory()
        setState(CashUiState(draft = SaleDraft(id = UUID.randomUUID().toString()), notice = notice, restored = true))
        persist()
    }

    private fun mutate(transform: (SaleDraft) -> SaleDraft) {
        if (_state.value.draft.confirmed || _state.value.busy) return
        guidedForwardHistory.clear()
        setState(_state.value.copy(draft = transform(_state.value.draft), notice = null))
        persist()
    }

    private fun navigateGuided(next: CashUiState) {
        val current = _state.value
        if (current.draft.confirmed || current.busy) return
        guidedBackHistory += current.toHistoryEntry()
        guidedForwardHistory.clear()
        setState(next)
        persist()
    }

    private fun CashUiState.toHistoryEntry() = GuidedHistoryEntry(draft, guidedStep)

    private fun fallbackPrevious(current: CashUiState): GuidedHistoryEntry? = when {
        current.draft.paymentStep -> GuidedHistoryEntry(
            current.draft.copy(paymentStep = false, receivedAmount = 0, paymentEntered = false),
            GuidedStep.DECISION,
        )
        current.guidedStep == GuidedStep.DECISION -> GuidedHistoryEntry(current.draft, GuidedStep.QUANTITY)
        current.guidedStep == GuidedStep.QUANTITY -> GuidedHistoryEntry(current.draft, GuidedStep.PRICE)
        current.guidedStep == GuidedStep.PRICE && current.draft.items.size > 1 -> GuidedHistoryEntry(
            current.draft.copy(items = current.draft.items.dropLast(1)),
            GuidedStep.DECISION,
        )
        else -> null
    }

    private fun canStepBack(state: CashUiState): Boolean = guidedBackHistory.isNotEmpty() ||
        state.draft.paymentStep ||
        state.guidedStep != GuidedStep.PRICE ||
        state.draft.items.size > 1

    private fun setState(state: CashUiState) {
        _state.value = state.copy(
            canNavigateBack = canStepBack(state),
            canNavigateForward = guidedForwardHistory.isNotEmpty(),
        )
    }

    private fun clearGuidedHistory() {
        guidedBackHistory.clear()
        guidedForwardHistory.clear()
    }

    private fun persist() {
        val snapshot = _state.value.draft
        viewModelScope.launch { repository.saveDraft(snapshot) }
    }
}
