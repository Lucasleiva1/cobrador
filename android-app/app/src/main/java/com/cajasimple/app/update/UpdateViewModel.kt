package com.cajasimple.app.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository(application)
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    private var activeJob: Job? = null

    fun check(silent: Boolean = false) {
        if (activeJob?.isActive == true) return
        activeJob = viewModelScope.launch {
            if (!silent) _state.value = UpdateUiState.Checking
            try {
                val update = repository.latest()
                _state.value = update?.let(UpdateUiState::Available)
                    ?: if (silent) UpdateUiState.Idle else UpdateUiState.UpToDate
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = if (silent) UpdateUiState.Idle
                else UpdateUiState.Error(error.message ?: "No se pudo buscar actualizaciones.")
            }
        }
    }

    fun download(update: AppUpdate) {
        if (activeJob?.isActive == true) return
        activeJob = viewModelScope.launch {
            _state.value = UpdateUiState.Downloading(update, null)
            try {
                val file = repository.download(update) { progress ->
                    _state.value = UpdateUiState.Downloading(update, progress)
                }
                _state.value = UpdateUiState.ReadyToInstall(update, file)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = UpdateUiState.Error(error.message ?: "No se pudo descargar la actualización.")
            }
        }
    }

    fun cancelDownload() {
        activeJob?.cancel()
        activeJob = null
        _state.value = UpdateUiState.Idle
    }

    fun dismiss() {
        if (_state.value !is UpdateUiState.Downloading) _state.value = UpdateUiState.Idle
    }

    fun permissionRequired(update: AppUpdate, file: File) {
        _state.value = UpdateUiState.PermissionRequired(update, file)
    }

    fun retryInstall() {
        val current = _state.value as? UpdateUiState.PermissionRequired ?: return
        _state.value = UpdateUiState.ReadyToInstall(current.update, current.file)
    }

    fun installationError(message: String) {
        _state.value = UpdateUiState.Error(message)
    }

    fun installationLaunched() {
        _state.value = UpdateUiState.Idle
    }
}
