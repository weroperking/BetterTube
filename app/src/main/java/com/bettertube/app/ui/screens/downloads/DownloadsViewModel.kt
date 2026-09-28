package com.bettertube.app.ui.screens.downloads

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertube.app.domain.model.Aria2Download
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.repository.Aria2Repository
import com.bettertube.app.domain.usecase.CancelDownloadUseCase
import com.bettertube.app.domain.usecase.MoveToVaultUseCase
import com.bettertube.app.domain.usecase.ObserveDownloadsUseCase
import com.bettertube.app.domain.usecase.PauseDownloadUseCase
import com.bettertube.app.domain.usecase.ReorderQueueUseCase
import com.bettertube.app.domain.usecase.ResumeDownloadUseCase
import com.bettertube.app.domain.usecase.RetryDownloadUseCase
import com.bettertube.app.domain.usecase.SetGlobalSpeedLimitUseCase
import com.bettertube.app.domain.usecase.SetSpeedLimitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeDownloadsUseCase: ObserveDownloadsUseCase,
    private val pauseDownloadUseCase: PauseDownloadUseCase,
    private val resumeDownloadUseCase: ResumeDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val retryDownloadUseCase: RetryDownloadUseCase,
    private val reorderQueueUseCase: ReorderQueueUseCase,
    private val setSpeedLimitUseCase: SetSpeedLimitUseCase,
    private val setGlobalSpeedLimitUseCase: SetGlobalSpeedLimitUseCase,
    private val moveToVaultUseCase: MoveToVaultUseCase,
    private val aria2Repository: Aria2Repository
) : ViewModel() {

    val tasks: StateFlow<List<DownloadTask>> = observeDownloadsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val aria2Downloads: StateFlow<List<Aria2Download>> = aria2Repository.observeActiveDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            observeDownloadsUseCase().collect {
                _isLoading.value = false
            }
        }
    }

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun onAddMagnet(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = aria2Repository.addMagnet(uri, null)
            result.fold(
                onSuccess = { _snackbarEvent.emit("Magnet download added to aria2") },
                onFailure = { _snackbarEvent.emit("Failed to add magnet: ${it.message}") }
            )
        }
    }

    fun onAddHttpUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = aria2Repository.addHttpDownload(url, null)
            result.fold(
                onSuccess = { _snackbarEvent.emit("Download added to aria2") },
                onFailure = { _snackbarEvent.emit("Failed to add URL: ${it.message}") }
            )
        }
    }

    fun onAddTorrentFile(bytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = aria2Repository.addTorrentFile(bytes, null)
            result.fold(
                onSuccess = { _snackbarEvent.emit("Torrent added to aria2") },
                onFailure = { _snackbarEvent.emit("Failed to add torrent: ${it.message}") }
            )
        }
    }

    fun onAddMetalinkFile(bytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = aria2Repository.addMetalinkFile(bytes)
            result.fold(
                onSuccess = { _snackbarEvent.emit("Metalink added to aria2") },
                onFailure = { _snackbarEvent.emit("Failed to add metalink: ${it.message}") }
            )
        }
    }

    fun onPauseAria2(gid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = aria2Repository.pauseDownload(gid)
            if (result.isFailure) {
                _snackbarEvent.emit("Failed to pause download")
            }
        }
    }

    fun onResumeAria2(gid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = aria2Repository.resumeDownload(gid)
            if (result.isFailure) {
                _snackbarEvent.emit("Failed to resume download")
            }
        }
    }

    fun onRemoveAria2(gid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = aria2Repository.removeDownload(gid)
            if (result.isFailure) {
                _snackbarEvent.emit("Failed to remove download")
            }
        }
    }

    fun onPause(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pauseDownloadUseCase(id).collect {}
        }
    }

    fun onResume(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            resumeDownloadUseCase(id).collect {}
        }
    }

    fun onCancel(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelDownloadUseCase(id).collect {}
        }
    }

    fun onRetry(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            retryDownloadUseCase(id).collect {}
        }
    }

    fun onOpen(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _snackbarEvent.emit("Open in external app — coming soon")
        }
    }

    fun onMoveUp(id: String) {
        val currentWaiting = tasks.value.filter { it.status == DownloadStatus.WAITING }.map { it.id }.toMutableList()
        val index = currentWaiting.indexOf(id)
        if (index > 0) {
            val temp = currentWaiting[index]
            currentWaiting[index] = currentWaiting[index - 1]
            currentWaiting[index - 1] = temp
            viewModelScope.launch(Dispatchers.IO) {
                reorderQueueUseCase(currentWaiting).collect {}
            }
        }
    }

    fun onMoveDown(id: String) {
        val currentWaiting = tasks.value.filter { it.status == DownloadStatus.WAITING }.map { it.id }.toMutableList()
        val index = currentWaiting.indexOf(id)
        if (index >= 0 && index < currentWaiting.size - 1) {
            val temp = currentWaiting[index]
            currentWaiting[index] = currentWaiting[index + 1]
            currentWaiting[index + 1] = temp
            viewModelScope.launch(Dispatchers.IO) {
                reorderQueueUseCase(currentWaiting).collect {}
            }
        }
    }

    fun onSetSpeedLimit(id: String, bps: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            setSpeedLimitUseCase(id, bps).collect {}
        }
    }

    fun onSetGlobalSpeedLimit(bps: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            setGlobalSpeedLimitUseCase(bps).collect {}
        }
    }

    fun onMoveToVault(task: DownloadTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = moveToVaultUseCase(task.id)
            result.fold(
                onSuccess = {
                    _snackbarEvent.emit("Moved \"${task.title}\" to Secure Vault")
                },
                onFailure = { err ->
                    _snackbarEvent.emit(err.message ?: "Failed to move to vault (make sure vault is unlocked)")
                }
            )
        }
    }
}
