package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class ReorderQueueUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(orderedIds: List<String>): Flow<Result<Unit>> = flow {
        emit(repository.reorderQueue(orderedIds))
    }.flowOn(Dispatchers.IO)
}
