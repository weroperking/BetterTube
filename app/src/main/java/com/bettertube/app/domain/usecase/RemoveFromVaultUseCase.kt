package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemoveFromVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(vaultItemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        repository.removeFromVault(vaultItemId)
    }
}
