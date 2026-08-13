package com.cajasimple.app.util

import android.content.Context
import android.provider.Settings
import com.cajasimple.app.BuildConfig
import java.security.MessageDigest

object DeviceIdentity {
    fun id(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty().ifBlank { "sin-identificador" }
        val source = "${context.packageName}:$androidId"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(16)
        return if (BuildConfig.DEBUG) "debug-$hash" else hash
    }
}
