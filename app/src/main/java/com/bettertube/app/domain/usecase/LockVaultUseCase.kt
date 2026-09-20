package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LockVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        repository.lock()
    }
}
