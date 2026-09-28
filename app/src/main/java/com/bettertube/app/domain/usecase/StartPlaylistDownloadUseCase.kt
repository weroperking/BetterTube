package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.model.ExtractionPreset
import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StartPlaylistDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        url: String,
        selectedIndices: List<Int>,
        formatId: String,
        preset: ExtractionPreset,
        downloadSubtitles: Boolean,
        subtitleLanguages: List<String>,
        embedSubtitles: Boolean
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        repository.startPlaylistDownload(
            url = url,
            selectedIndices = selectedIndices,
            formatId = formatId,
            preset = preset,
            downloadSubtitles = downloadSubtitles,
            subtitleLanguages = subtitleLanguages,
            embedSubtitles = embedSubtitles
        )
    }
}
