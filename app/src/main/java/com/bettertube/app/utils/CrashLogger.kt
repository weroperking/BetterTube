package com.bettertube.app.utils

import android.content.Context
import android.os.Build
import com.example.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashLogger(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            writeCrashLog(t, e)
        } catch (ex: Exception) {
            // Avoid failing inside crash handler
        } finally {
            defaultHandler?.uncaughtException(t, e)
        }
    }

    private fun writeCrashLog(t: Thread, e: Throwable) {
        val crashDir = getCrashDir(context)
        if (!crashDir.exists()) {
            crashDir.mkdirs()
        }

        val timestampStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())

        val pInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (ex: Exception) {
            null
        }

        val versionName = pInfo?.versionName ?: "1.0.0"
        @Suppress("DEPRECATION")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo?.longVersionCode ?: 1L
        } else {
            pInfo?.versionCode?.toLong() ?: 1L
        }

        val logContent = buildString {
            appendLine("Timestamp: $isoTimestamp")
            appendLine("App Version: $versionName ($versionCode)")
            appendLine("Android Version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Thread: ${t.name} (id: ${t.id})")
            appendLine("Exception: ${e.javaClass.name}: ${e.message}")
            appendLine("Stacktrace:")
            appendLine(e.stackTraceToString())
        }

        val crashFile = File(crashDir, "crash_$timestampStr.txt")
        crashFile.writeText(logContent)

        trimOldLogs(crashDir, maxFiles = 10)
    }

    companion object {
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashLogger(context.applicationContext, defaultHandler))
        }

        fun getCrashDir(context: Context): File {
            return File(context.filesDir, "crash_logs")
        }

        fun getCrashLogs(context: Context): List<File> {
            val crashDir = getCrashDir(context)
            if (!crashDir.exists()) return emptyList()
            return crashDir.listFiles { file -> file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }

        fun getCrashLogContent(file: File): String {
            return try {
                file.readText()
            } catch (e: Exception) {
                "Error reading crash log: ${e.message}"
            }
        }

        fun clearAllCrashLogs(context: Context): Boolean {
            val crashDir = getCrashDir(context)
            if (!crashDir.exists()) return true
            return crashDir.listFiles()?.all { it.delete() } ?: true
        }

        private fun trimOldLogs(crashDir: File, maxFiles: Int) {
            val files = crashDir.listFiles { file -> file.isFile && file.name.startsWith("crash_") }
                ?.sortedByDescending { it.lastModified() }
                ?: return

            if (files.size > maxFiles) {
                files.drop(maxFiles).forEach { it.delete() }
            }
        }
    }
}
