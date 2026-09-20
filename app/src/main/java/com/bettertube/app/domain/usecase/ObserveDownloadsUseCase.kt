package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class ObserveDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadTask>> = repository.getAllTasks().flowOn(Dispatchers.IO)
}
