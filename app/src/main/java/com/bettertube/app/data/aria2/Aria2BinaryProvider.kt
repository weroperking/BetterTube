package com.bettertube.app.data.aria2

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Aria2BinaryProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "Aria2BinaryProvider"
    }

    fun getAria2cPath(): File {
        val noBackupDir = context.noBackupFilesDir
        val libExtracted = File(noBackupDir, "youtubedl-android/packages/aria2c/aria2c")
        if (libExtracted.exists() && libExtracted.canExecute()) {
            return libExtracted
        }

        val manualExtracted = File(context.filesDir, "aria2c/aria2c")
        if (manualExtracted.exists() && manualExtracted.canExecute()) {
            return manualExtracted
        }

        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val aria2cZip = File(nativeLibDir, "libaria2c.zip.so")
        if (aria2cZip.exists()) {
            val extractDir = File(context.filesDir, "aria2c")
            extractDir.mkdirs()
            try {
                aria2cZip.inputStream().use { fis ->
                    ZipInputStream(fis).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val name = entry.name
                                if (name.contains("..")) {
                                    zis.closeEntry()
                                    entry = zis.nextEntry
                                    continue
                                }
                                val outFile = File(extractDir, name)
                                outFile.parentFile?.mkdirs()
                                outFile.outputStream().use { out ->
                                    zis.copyTo(out)
                                }
                                outFile.setExecutable(true, false)
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
                val extractedBinary = File(extractDir, "aria2c")
                if (extractedBinary.exists() && extractedBinary.canExecute()) {
                    return extractedBinary
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract aria2c binary", e)
            }
        }

        val filesInDir = nativeLibDir.list()?.joinToString(", ") ?: "null (directory does not exist or is empty)"
        Log.e(TAG, "aria2c binary not found. nativeLibraryDir: ${nativeLibDir.absolutePath}. Contents: [$filesInDir]")
        throw IllegalStateException("aria2c binary not found in nativeLibraryDir: ${nativeLibDir.absolutePath}")
    }

    fun ensureExecutable(): Boolean {
        return try {
            val file = getAria2cPath()
            val executable = file.setExecutable(true, false)
            if (!executable) {
                file.canExecute()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to make aria2c executable", e)
            false
        }
    }
}
