package com.bettertube.app.data.repository

import android.os.SystemClock

import android.content.Context
import androidx.biometric.BiometricManager
import com.bettertube.app.data.vault.VaultCipher
import com.bettertube.app.data.vault.VaultPinManager
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.MediaType
import com.bettertube.app.domain.model.VaultItem
import com.bettertube.app.domain.model.VaultState
import com.bettertube.app.domain.repository.DownloadRepository
import com.bettertube.app.domain.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultCipher: VaultCipher,
    private val pinManager: VaultPinManager,
    private val downloadRepository: DownloadRepository
) : VaultRepository {

    private val vaultDir: File = File(context.filesDir, "vault").apply {
        if (!exists()) mkdirs()
    }
    private val metadataFile: File = File(context.filesDir, "vault_metadata.json")
    private val tempDir: File = File(context.cacheDir, "vault_temp").apply {
        if (!exists()) mkdirs()
    }

    private val _vaultState = MutableStateFlow(
        if (pinManager.hasPin()) VaultState.LOCKED else VaultState.UNINITIALIZED
    )
    private val _vaultItems = MutableStateFlow<List<VaultItem>>(emptyList())

    init {
        loadMetadata()
    }

    override fun getVaultState(): StateFlow<VaultState> = _vaultState.asStateFlow()

    override suspend fun initializeVault(pin: String): Result<Unit> {
        if (_vaultState.value != VaultState.UNINITIALIZED) {
            return Result.failure(IllegalStateException("Vault is already initialized"))
        }
        if (pin.length < 4) {
            return Result.failure(IllegalArgumentException("PIN must be at least 4 digits"))
        }
        pinManager.setPin(pin)
        _vaultState.value = VaultState.UNLOCKED
        return Result.success(Unit)
    }

    override suspend fun unlockWithPin(pin: String): Result<Unit> {
        return if (pinManager.verifyPin(pin)) {
            _vaultState.value = VaultState.UNLOCKED
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Incorrect PIN"))
        }
    }

    override suspend fun unlockWithBiometric(): Result<Unit> {
        if (_vaultState.value != VaultState.LOCKED) {
            return Result.failure(IllegalStateException("Vault is not locked"))
        }
        if (!isBiometricEnabled()) {
            return Result.failure(IllegalStateException("Biometric unlock is not enabled"))
        }
        _vaultState.value = VaultState.UNLOCKED
        return Result.success(Unit)
    }

    override fun lock() {
        _vaultState.value = if (pinManager.hasPin()) VaultState.LOCKED else VaultState.UNINITIALIZED
        // Clear all decrypted temp files in cacheDir/vault_temp/
        try {
            tempDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            android.util.Log.e("VaultRepository", "Failed to clear temp files", e)
        }
    }

    override suspend fun moveToVault(taskId: String): Result<VaultItem> {
        if (_vaultState.value != VaultState.UNLOCKED) {
            return Result.failure(IllegalStateException("Vault is locked"))
        }

        val task = downloadRepository.getTask(taskId).firstOrNull()
            ?: return Result.failure(IllegalArgumentException("Task not found: $taskId"))

        if (task.status != DownloadStatus.COMPLETE || task.outputFilePath == null) {
            return Result.failure(IllegalStateException("Task is not completed or has no output file"))
        }

        val sourceFile = File(task.outputFilePath)
        if (!sourceFile.exists()) {
            return Result.failure(IllegalArgumentException("Source file does not exist: ${task.outputFilePath}"))
        }

        val extension = sourceFile.extension.let { if (it.isNotBlank()) ".$it" else "" }
        val encryptedFileName = "${UUID.randomUUID()}$extension"
        val encryptedDestination = File(vaultDir, encryptedFileName)
        val originalSize = sourceFile.length()

        val encryptResult = vaultCipher.encryptFile(sourceFile, encryptedDestination)
        if (encryptResult.isFailure) {
            return Result.failure(encryptResult.exceptionOrNull() ?: Exception("Encryption failed"))
        }

        val vaultItem = VaultItem(
            id = UUID.randomUUID().toString(),
            originalTaskId = taskId,
            fileName = sourceFile.name,
            encryptedPath = encryptedDestination.absolutePath,
            originalPath = task.outputFilePath,
            sizeBytes = originalSize,
            mediaType = task.mediaType,
            addedAtMillis = SystemClock.elapsedRealtime()
        )

        _vaultItems.update { it + vaultItem }
        persistMetadata()

        return Result.success(vaultItem)
    }

    override suspend fun removeFromVault(vaultItemId: String): Result<Unit> {
        if (_vaultState.value != VaultState.UNLOCKED) {
            return Result.failure(IllegalStateException("Vault is locked"))
        }

        val item = _vaultItems.value.find { it.id == vaultItemId }
            ?: return Result.failure(IllegalArgumentException("Vault item not found: $vaultItemId"))

        val encryptedFile = File(item.encryptedPath)
        if (!encryptedFile.exists()) {
            _vaultItems.update { it.filterNot { item -> item.id == vaultItemId } }
            persistMetadata()
            return Result.failure(IllegalStateException("Encrypted file missing"))
        }

        // Edge Case 2: Determine target restore directory and unique file name
        val targetDir = item.originalPath?.let { File(it).parentFile }?.takeIf { it.exists() || it.mkdirs() }
            ?: File(context.getExternalFilesDir(null) ?: context.filesDir, "BetterTube").apply { mkdirs() }

        var targetFile = File(targetDir, item.fileName)
        if (targetFile.exists()) {
            val baseName = item.fileName.substringBeforeLast(".")
            val ext = item.fileName.substringAfterLast(".", "")
            val extWithDot = if (ext.isNotEmpty()) ".$ext" else ""
            var counter = 1
            while (targetFile.exists()) {
                targetFile = File(targetDir, "${baseName}_$counter$extWithDot")
                counter++
            }
        }

        val decryptResult = vaultCipher.decryptFile(encryptedFile, targetFile)
        if (decryptResult.isFailure) {
            return Result.failure(decryptResult.exceptionOrNull() ?: Exception("Decryption failed"))
        }

        encryptedFile.delete()
        _vaultItems.update { it.filterNot { item -> item.id == vaultItemId } }
        persistMetadata()

        return Result.success(Unit)
    }

    override suspend fun deleteVaultItem(vaultItemId: String): Result<Unit> {
        if (_vaultState.value != VaultState.UNLOCKED) {
            return Result.failure(IllegalStateException("Vault is locked"))
        }

        val item = _vaultItems.value.find { it.id == vaultItemId }
            ?: return Result.failure(IllegalArgumentException("Vault item not found: $vaultItemId"))

        val encryptedFile = File(item.encryptedPath)
        vaultCipher.deleteSecurely(encryptedFile)
        _vaultItems.update { it.filterNot { it.id == vaultItemId } }
        persistMetadata()

        return Result.success(Unit)
    }

    override fun getAllVaultItems(): Flow<List<VaultItem>> {
        return _vaultItems.map { it.sortedByDescending { item -> item.addedAtMillis } }
    }

    override suspend fun changePin(oldPin: String, newPin: String): Result<Unit> {
        if (!pinManager.verifyPin(oldPin)) {
            return Result.failure(IllegalArgumentException("Incorrect current PIN"))
        }
        if (newPin.length < 4) {
            return Result.failure(IllegalArgumentException("New PIN must be at least 4 digits"))
        }
        pinManager.setPin(newPin)
        lock()
        return Result.success(Unit)
    }

    override fun isBiometricAvailable(): Boolean {
        return try {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            android.util.Log.w("VaultRepository", "Biometric check failed", e)
            false
        }
    }

    override fun isBiometricEnabled(): Boolean {
        return pinManager.isBiometricEnabled()
    }

    override suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit> {
        pinManager.setBiometricEnabled(enabled)
        return Result.success(Unit)
    }

    fun clearVaultCompletely() {
        try {
            vaultDir.listFiles()?.forEach { it.delete() }
            tempDir.listFiles()?.forEach { it.delete() }
            if (metadataFile.exists()) metadataFile.delete()
            _vaultItems.value = emptyList()
            pinManager.clearPin()
            _vaultState.value = VaultState.UNINITIALIZED
        } catch (e: Exception) {
            android.util.Log.e("VaultRepository", "Failed to clear vault completely", e)
        }
    }

    private fun loadMetadata() {
        try {
            if (!metadataFile.exists()) return
            val jsonString = metadataFile.readText()
            if (jsonString.isBlank()) return

            val array = JSONArray(jsonString)
            val items = mutableListOf<VaultItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val mediaTypeStr = obj.optString("mediaType", MediaType.OTHER.name)
                val mediaType = runCatching { MediaType.valueOf(mediaTypeStr) }.getOrDefault(MediaType.OTHER)

                items.add(
                    VaultItem(
                        id = obj.getString("id"),
                        originalTaskId = obj.getString("originalTaskId"),
                        fileName = obj.getString("fileName"),
                        encryptedPath = obj.getString("encryptedPath"),
                        originalPath = if (obj.has("originalPath")) obj.getString("originalPath") else null,
                        sizeBytes = obj.getLong("sizeBytes"),
                        mediaType = mediaType,
                        addedAtMillis = obj.getLong("addedAtMillis")
                    )
                )
            }
            _vaultItems.value = items
        } catch (e: Exception) {
            android.util.Log.e("VaultRepository", "Failed to load metadata", e)
        }
    }

    private fun persistMetadata() {
        try {
            val array = JSONArray()
            _vaultItems.value.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("originalTaskId", item.originalTaskId)
                    put("fileName", item.fileName)
                    put("encryptedPath", item.encryptedPath)
                    item.originalPath?.let { put("originalPath", it) }
                    put("sizeBytes", item.sizeBytes)
                    put("mediaType", item.mediaType.name)
                    put("addedAtMillis", item.addedAtMillis)
                }
                array.put(obj)
            }
            atomicWrite(metadataFile, array.toString())
        } catch (e: Exception) {
            android.util.Log.e("VaultRepository", "Failed to clear temp files", e)
        }
    }

    private fun atomicWrite(file: File, content: String) {
        val dir = file.parentFile ?: return
        if (!dir.exists()) dir.mkdirs()
        val tempFile = File(dir, "${file.name}.${SystemClock.elapsedRealtime()}.tmp")
        tempFile.writeText(content)
        if (!tempFile.renameTo(file)) {
            file.writeText(content)
            tempFile.delete()
        }
    }
}
