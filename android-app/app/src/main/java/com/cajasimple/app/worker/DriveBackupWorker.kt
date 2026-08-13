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
import com.cajasimple.app.domain.model.ConfirmedSale
import com.cajasimple.app.util.DeviceIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
            if (sales.isNotEmpty()) {
                val fileName = "ventas-$date.csv"
                writeLocalDocuments(fileName, DailySalesCsv.create(sales))
            }

            val pending = container.salesRepository.pendingSales(SYNC_BATCH_SIZE)
            if (pending.isEmpty()) return Result.success()
            val deviceId = DeviceIdentity.id(applicationContext)
            for (sale in pending) {
                when (uploadSale(deviceId, sale)) {
                    UploadResult.ACCEPTED -> container.salesRepository.markSynced(sale.id)
                    UploadResult.DEVICE_NOT_AUTHORIZED -> return Result.success()
                    UploadResult.RETRY -> return Result.retry()
                }
            }
            if (pending.size == SYNC_BATCH_SIZE) Result.retry() else Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun uploadSale(deviceId: String, sale: ConfirmedSale): UploadResult = withContext(Dispatchers.IO) {
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
            val items = JSONArray().apply {
                sale.items.forEach { item ->
                    put(JSONObject().apply {
                        put("description", item.description.trim())
                        put("unitPrice", item.unitPrice)
                        put("quantity", item.quantity)
                        put("subtotal", item.subtotal)
                    })
                }
            }
            val body = JSONObject().apply {
                put("token", BuildConfig.DRIVE_BACKUP_TOKEN)
                put("deviceId", deviceId)
                put("sale", JSONObject().apply {
                    put("id", sale.id)
                    put("createdAt", sale.createdAt)
                    put("totalAmount", sale.totalAmount)
                    put("receivedAmount", sale.receivedAmount)
                    put("changeAmount", sale.changeAmount)
                    put("items", items)
                })
            }.toString()
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            val code = connection.responseCode
            if (code !in 200..299) return@withContext UploadResult.RETRY
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = runCatching { JSONObject(response) }.getOrNull()
                ?: return@withContext UploadResult.RETRY
            when {
                json.optBoolean("ok") -> UploadResult.ACCEPTED
                json.optString("error") == "device_not_authorized" -> UploadResult.DEVICE_NOT_AUTHORIZED
                else -> UploadResult.RETRY
            }
        } finally {
            connection.disconnect()
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
        private const val SYNC_BATCH_SIZE = 25

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<DriveBackupWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("drive-daily-backup", ExistingWorkPolicy.REPLACE, request)
        }
    }

    private enum class UploadResult { ACCEPTED, DEVICE_NOT_AUTHORIZED, RETRY }
}
