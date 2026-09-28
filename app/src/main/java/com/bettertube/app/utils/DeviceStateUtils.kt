package com.bettertube.app.utils

import android.content.Context
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.util.Log

object DeviceStateUtils {

    const val MIN_STORAGE_BYTES = 500L * 1024L * 1024L // 500 MB

    fun hasAdequateStorage(context: Context, minRequiredBytes: Long = MIN_STORAGE_BYTES): Boolean {
        return try {
            val path = context.getExternalFilesDir(null) ?: context.filesDir
            val stat = StatFs(path.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= minRequiredBytes
        } catch (e: Exception) {
            Log.w("DeviceStateUtils", "Storage check failed, assuming adequate", e)
            true // fallback to true if stat fails
        }
    }

    fun isPowerSaveMode(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isPowerSaveMode == true
        } catch (e: Exception) {
            Log.w("DeviceStateUtils", "Power save mode check failed", e)
            false
        }
    }
}
