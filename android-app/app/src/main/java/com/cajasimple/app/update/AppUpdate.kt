package com.cajasimple.app.update

import java.io.File

data class AppUpdate(
    val versionName: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val sha256: String?,
    val checksumUrl: String?,
    val sizeBytes: Long,
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val update: AppUpdate) : UpdateUiState
    data class Downloading(val update: AppUpdate, val progress: Float?) : UpdateUiState
    data class ReadyToInstall(val update: AppUpdate, val file: File) : UpdateUiState
    data class PermissionRequired(val update: AppUpdate, val file: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

object VersionNumber {
    fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = parts(remote)
        val currentParts = parts(current)
        val count = maxOf(remoteParts.size, currentParts.size)
        return (0 until count).firstNotNullOfOrNull { index ->
            val difference = remoteParts.getOrElse(index) { 0 } - currentParts.getOrElse(index) { 0 }
            difference.takeIf { it != 0 }?.let { it > 0 }
        } ?: false
    }

    private fun parts(value: String): List<Int> = value
        .trim()
        .removePrefix("v")
        .substringBefore('-')
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
