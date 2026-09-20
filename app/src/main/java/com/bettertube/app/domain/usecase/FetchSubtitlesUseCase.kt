package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.model.SubtitleTrack
import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FetchSubtitlesUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(url: String): Result<List<SubtitleTrack>> = withContext(Dispatchers.IO) {
        repository.fetchSubtitles(url)
    }
}
