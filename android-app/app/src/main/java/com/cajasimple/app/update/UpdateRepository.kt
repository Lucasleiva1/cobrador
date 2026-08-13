package com.cajasimple.app.update

import android.content.Context
import com.cajasimple.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpdateRepository(private val context: Context) {
    suspend fun latest(): AppUpdate? = withContext(Dispatchers.IO) {
        val json = requestText(BuildConfig.UPDATE_API_URL, "application/vnd.github+json")
        val release = JSONObject(json)
        val remoteVersion = release.getString("tag_name").removePrefix("v")
        if (!VersionNumber.isNewer(remoteVersion, BuildConfig.VERSION_NAME)) return@withContext null

        val assets = release.getJSONArray("assets")
        var apk: JSONObject? = null
        var checksum: JSONObject? = null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            when {
                asset.getString("name") == APK_FILE_NAME -> apk = asset
                asset.getString("name") == CHECKSUM_FILE_NAME -> checksum = asset
            }
        }
        val apkAsset = apk ?: error("La versión $remoteVersion todavía no tiene el instalador Android.")
        val digest = apkAsset.optString("digest")
            .takeIf { it.startsWith("sha256:") }
            ?.removePrefix("sha256:")

        AppUpdate(
            versionName = remoteVersion,
            title = release.optString("name").ifBlank { "Caja Simple $remoteVersion" },
            notes = release.optString("body"),
            apkUrl = apkAsset.getString("browser_download_url"),
            sha256 = digest,
            checksumUrl = checksum?.getString("browser_download_url"),
            sizeBytes = apkAsset.optLong("size"),
        )
    }

    suspend fun download(update: AppUpdate, onProgress: (Float?) -> Unit): File = withContext(Dispatchers.IO) {
        val expectedChecksum = update.sha256 ?: update.checksumUrl
            ?.let { requestText(it, "text/plain").trim().substringBefore(' ').lowercase() }
            ?: error("La actualización no incluye una firma de integridad SHA-256.")
        require(expectedChecksum.matches(Regex("[0-9a-f]{64}"))) {
            "La firma de integridad de la actualización no es válida."
        }

        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val destination = File(directory, APK_FILE_NAME)
        val temporary = File(directory, "$APK_FILE_NAME.part")
        if (temporary.exists()) temporary.delete()

        val connection = connection(update.apkUrl, "application/vnd.android.package-archive")
        try {
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: update.sizeBytes.takeIf { it > 0 }
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        onProgress(total?.let { copied.toFloat() / it.toFloat() })
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val actualChecksum = temporary.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }
        if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
            temporary.delete()
            error("La descarga no superó la verificación de integridad.")
        }
        if (destination.exists()) destination.delete()
        check(temporary.renameTo(destination)) { "No se pudo preparar el instalador descargado." }
        destination
    }

    private fun requestText(url: String, accept: String): String {
        val connection = connection(url, accept)
        return try {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun connection(url: String, accept: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "Caja-Simple-Android/${BuildConfig.VERSION_NAME}")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            val code = responseCode
            if (code !in 200..299) {
                disconnect()
                error("El servicio de actualizaciones respondió con el código $code.")
            }
        }

    private companion object {
        const val APK_FILE_NAME = "caja-simple.apk"
        const val CHECKSUM_FILE_NAME = "caja-simple.apk.sha256"
    }
}
