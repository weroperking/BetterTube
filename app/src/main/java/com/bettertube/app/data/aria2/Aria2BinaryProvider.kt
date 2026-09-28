package com.bettertube.app.data.aria2

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val aria2cFile = File(nativeLibDir, "libaria2c.so")
        if (aria2cFile.exists()) {
            return aria2cFile
        }

        // Diagnostic logging
        val filesInDir = nativeLibDir.list()?.joinToString(", ") ?: "null (directory does not exist or is empty)"
        Log.e(TAG, "aria2c binary not found in nativeLibraryDir: ${nativeLibDir.absolutePath}. Contents: [$filesInDir]")
        throw IllegalStateException("aria2c binary not found in nativeLibraryDir: ${nativeLibDir.absolutePath}")
    }

    fun ensureExecutable(): Boolean {
        return try {
            val file = getAria2cPath()
            val executable = file.setExecutable(true, false)
            if (!executable) {
                // If setExecutable returned false, check if it is already executable
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
