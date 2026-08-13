package com.cajasimple.app.worker

import android.content.Context
import android.content.ContentUris
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cajasimple.app.CajaSimpleApplication
import com.cajasimple.app.BuildConfig
import com.cajasimple.app.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class DriveBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as CajaSimpleApplication).container
        return try {
            val date = LocalDate.now()
            val sales = container.salesRepository.getDay(date)
            if (sales.isEmpty()) return Result.success()
            val fileName = "ventas-$date.csv"
            val content = DailySalesCsv.create(sales)
            writeLocalDocuments(fileName, content)
            if (!uploadToDrive(fileName, content)) return Result.retry()
            sales.forEach { container.database.salesDao().updateSyncStatus(it.id, SyncStatus.SYNCED.name) }
            Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun uploadToDrive(fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val connection = (URL(BuildConfig.DRIVE_BACKUP_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val body = "{\"token\":\"${jsonEscape(BuildConfig.DRIVE_BACKUP_TOKEN)}\",\"fileName\":\"${jsonEscape(fileName)}\",\"csv\":\"${jsonEscape(content)}\"}"
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            val code = connection.responseCode
            if (code !in 200..299) return@withContext false
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            response.contains("\"ok\":true")
        } finally {
            connection.disconnect()
        }
    }

    private fun jsonEscape(value: String): String = buildString(value.length + 16) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
            }
        }
    }

    private fun writeLocalDocuments(fileName: String, content: String) {
        val dayName = fileName.removePrefix("ventas-").removeSuffix(".csv")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = applicationContext.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val basePath = "${Environment.DIRECTORY_DOCUMENTS}/Caja Simple/"
            val relativePath = "$basePath$dayName/"
            val existingId = resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(fileName, relativePath),
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
            val legacyId = if (existingId == null) resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(fileName, basePath),
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null } else null
            val migratedUri = legacyId?.let { ContentUris.withAppendedId(collection, it) }?.also { uri ->
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath) },
                    null,
                    null,
                )
            }
            val uri = existingId?.let { ContentUris.withAppendedId(collection, it) } ?: migratedUri ?: resolver.insert(
                collection,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                },
            ) ?: error("No se pudo crear el CSV local")
            resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                ?: error("No se pudo escribir el CSV local")
        } else {
            val baseDirectory = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Caja Simple")
            val directory = File(baseDirectory, dayName).apply { mkdirs() }
            File(baseDirectory, fileName).takeIf { it.exists() }?.renameTo(File(directory, fileName))
            File(directory, fileName).writeText(content, Charsets.UTF_8)
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<DriveBackupWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("drive-daily-backup", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
