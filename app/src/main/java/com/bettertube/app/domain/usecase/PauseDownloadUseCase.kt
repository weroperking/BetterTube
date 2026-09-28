package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PauseDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(id: String): Flow<Result<Unit>> = flow {
        emit(repository.pauseDownload(id))
    }.flowOn(Dispatchers.IO)
}
