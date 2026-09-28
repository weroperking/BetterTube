package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.model.VaultItem
import com.bettertube.app.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class ObserveVaultItemsUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(): Flow<List<VaultItem>> {
        return repository.getAllVaultItems().flowOn(Dispatchers.IO)
    }
}
