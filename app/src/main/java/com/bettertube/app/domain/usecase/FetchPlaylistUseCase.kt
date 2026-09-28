package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.model.PlaylistInfo
import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FetchPlaylistUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(url: String): Result<PlaylistInfo> = withContext(Dispatchers.IO) {
        repository.fetchPlaylist(url)
    }
}
