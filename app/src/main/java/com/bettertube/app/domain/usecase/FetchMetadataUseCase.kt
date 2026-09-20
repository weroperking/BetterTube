package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class FetchMetadataUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(url: String): Flow<Result<MediaMetadata>> = flow {
        emit(repository.fetchMetadata(url))
    }.flowOn(Dispatchers.IO)
}
