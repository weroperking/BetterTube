package com.bettertube.app.data.vault

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class VaultCipher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    open fun encryptFile(source: File, destination: File): Result<Unit> {
        return try {
            if (!source.exists()) {
                return Result.failure(IllegalArgumentException("Source file does not exist: ${source.absolutePath}"))
            }

            if (destination.exists()) {
                destination.delete()
            }

            destination.parentFile?.mkdirs()

            val encryptedFile = EncryptedFile.Builder(
                context,
                destination,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val buffer = ByteArray(CHUNK_SIZE)
            FileInputStream(source).use { input ->
                encryptedFile.openFileOutput().use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            // Verify encrypted file exists and has content before deleting source (Edge Case 7)
            if (destination.exists() && destination.length() > 0) {
                source.delete()
                Result.success(Unit)
            } else {
                destination.delete()
                Result.failure(IllegalStateException("Encrypted file was not written properly"))
            }
        } catch (e: Exception) {
            if (destination.exists()) {
                destination.delete()
            }
            Result.failure(e)
        }
    }

    open fun decryptFile(source: File, destination: File): Result<Unit> {
        return try {
            if (!source.exists()) {
                return Result.failure(IllegalArgumentException("Encrypted source file does not exist: ${source.absolutePath}"))
            }

            if (destination.exists()) {
                destination.delete()
            }

            destination.parentFile?.mkdirs()

            val encryptedFile = EncryptedFile.Builder(
                context,
                source,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val buffer = ByteArray(CHUNK_SIZE)
            encryptedFile.openFileInput().use { input ->
                FileOutputStream(destination).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (destination.exists()) {
                destination.delete()
            }
            Result.failure(e)
        }
    }

    open fun deleteSecurely(file: File): Result<Unit> {
        return try {
            if (!file.exists()) {
                return Result.success(Unit)
            }

            val length = file.length()
            if (length > 0) {
                RandomAccessFile(file, "rws").use { raf ->
                    val buffer = ByteArray(CHUNK_SIZE.coerceAtMost(length.toInt().coerceAtLeast(1)))
                    var remaining = length
                    while (remaining > 0) {
                        val toWrite = buffer.size.toLong().coerceAtMost(remaining).toInt()
                        raf.write(buffer, 0, toWrite)
                        remaining -= toWrite
                    }
                }
            }
            file.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            file.delete()
            Result.failure(e)
        }
    }

    companion object {
        const val MASTER_KEY_ALIAS = "bettertube_vault_master_key"
        const val CHUNK_SIZE = 64 * 1024 // 64KB
    }
}
