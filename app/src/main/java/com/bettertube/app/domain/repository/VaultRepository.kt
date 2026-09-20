package com.bettertube.app.domain.repository

import com.bettertube.app.domain.model.VaultItem
import com.bettertube.app.domain.model.VaultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VaultRepository {
    fun getVaultState(): StateFlow<VaultState>
    suspend fun initializeVault(pin: String): Result<Unit>
    suspend fun unlockWithPin(pin: String): Result<Unit>
    suspend fun unlockWithBiometric(): Result<Unit>
    fun lock()
    suspend fun moveToVault(taskId: String): Result<VaultItem>
    suspend fun removeFromVault(vaultItemId: String): Result<Unit>
    suspend fun deleteVaultItem(vaultItemId: String): Result<Unit>
    fun getAllVaultItems(): Flow<List<VaultItem>>
    suspend fun changePin(oldPin: String, newPin: String): Result<Unit>
    fun isBiometricAvailable(): Boolean
    fun isBiometricEnabled(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit>
}
