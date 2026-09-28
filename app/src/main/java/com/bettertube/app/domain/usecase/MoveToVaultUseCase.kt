package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.model.VaultItem
import com.bettertube.app.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MoveToVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(taskId: String): Result<VaultItem> = withContext(Dispatchers.IO) {
        repository.moveToVault(taskId)
    }
}
