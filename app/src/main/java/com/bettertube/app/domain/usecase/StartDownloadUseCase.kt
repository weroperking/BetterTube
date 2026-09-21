package com.bettertube.app.domain.usecase

import android.os.SystemClock
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaFormat
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID
import javax.inject.Inject

class StartDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(
        url: String,
        metadata: MediaMetadata,
        format: MediaFormat
    ): Flow<Result<String>> = flow {
        val taskId = UUID.randomUUID().toString()
        val task = DownloadTask(
            id = taskId,
            url = url,
            title = metadata.title,
            thumbnailUrl = metadata.thumbnailUrl,
            formatId = format.formatId,
            status = DownloadStatus.QUEUED,
            progressPercent = 0.0f,
            downloadedBytes = 0L,
            totalBytes = format.fileSizeBytes ?: 0L,
            speedBytesPerSecond = 0L,
            etaSeconds = 0L,
            outputFilePath = null,
            errorMessage = null,
            createdAtMillis = SystemClock.elapsedRealtime()
        )
        emit(repository.startDownload(task))
    }.flowOn(Dispatchers.IO)
}
