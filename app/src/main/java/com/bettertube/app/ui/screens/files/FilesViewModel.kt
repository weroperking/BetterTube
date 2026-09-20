package com.bettertube.app.ui.screens.files

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaType
import com.bettertube.app.domain.model.VaultItem
import com.bettertube.app.domain.model.VaultState
import com.bettertube.app.domain.repository.DownloadRepository
import com.bettertube.app.domain.repository.VaultRepository
import com.bettertube.app.domain.usecase.ChangeVaultPinUseCase
import com.bettertube.app.domain.usecase.InitializeVaultUseCase
import com.bettertube.app.domain.usecase.LockVaultUseCase
import com.bettertube.app.domain.usecase.MoveToVaultUseCase
import com.bettertube.app.domain.usecase.ObserveVaultItemsUseCase
import com.bettertube.app.domain.usecase.RemoveFromVaultUseCase
import com.bettertube.app.domain.usecase.UnlockVaultWithBiometricUseCase
import com.bettertube.app.domain.usecase.UnlockVaultWithPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val downloadRepository: DownloadRepository,
    private val initializeVaultUseCase: InitializeVaultUseCase,
    private val unlockVaultWithPinUseCase: UnlockVaultWithPinUseCase,
    private val unlockVaultWithBiometricUseCase: UnlockVaultWithBiometricUseCase,
    private val lockVaultUseCase: LockVaultUseCase,
    private val moveToVaultUseCase: MoveToVaultUseCase,
    private val removeFromVaultUseCase: RemoveFromVaultUseCase,
    private val observeVaultItemsUseCase: ObserveVaultItemsUseCase,
    private val changeVaultPinUseCase: ChangeVaultPinUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val vaultState: StateFlow<VaultState> = vaultRepository.getVaultState()

    val vaultItems: StateFlow<List<VaultItem>> = observeVaultItemsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isBiometricSupported: Boolean = run {
        val biometricManager = BiometricManager.from(context)
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private val _isBiometricEnabled = MutableStateFlow(vaultRepository.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _confirmPinInput = MutableStateFlow("")
    val confirmPinInput: StateFlow<String> = _confirmPinInput.asStateFlow()

    private val _currentPinInput = MutableStateFlow("")
    val currentPinInput: StateFlow<String> = _currentPinInput.asStateFlow()

    private val _newPinInput = MutableStateFlow("")
    val newPinInput: StateFlow<String> = _newPinInput.asStateFlow()

    private val _confirmNewPinInput = MutableStateFlow("")
    val confirmNewPinInput: StateFlow<String> = _confirmNewPinInput.asStateFlow()

    private val _showChangePinDialog = MutableStateFlow(false)
    val showChangePinDialog: StateFlow<Boolean> = _showChangePinDialog.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    val completedDownloads: StateFlow<List<DownloadTask>> = downloadRepository.getAllTasks()
        .map { list ->
            list.filter { it.status == DownloadStatus.COMPLETE && !it.outputFilePath.isNullOrBlank() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredItems: StateFlow<List<VaultItem>> = combine(
        vaultItems,
        _selectedFilter,
        _searchQuery
    ) { items, filter, query ->
        items.filter { item ->
            val matchesFilter = when (filter) {
                "Videos" -> item.mediaType == MediaType.VIDEO
                "Audio" -> item.mediaType == MediaType.AUDIO
                else -> true
            }
            val matchesQuery = query.isBlank() || item.fileName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onPinInputChange(pin: String) {
        if (pin.length <= 8 && pin.all { it.isDigit() }) {
            _pinInput.value = pin
        }
    }

    fun onConfirmPinInputChange(pin: String) {
        if (pin.length <= 8 && pin.all { it.isDigit() }) {
            _confirmPinInput.value = pin
        }
    }

    fun onCurrentPinInputChange(pin: String) {
        if (pin.length <= 8 && pin.all { it.isDigit() }) {
            _currentPinInput.value = pin
        }
    }

    fun onNewPinInputChange(pin: String) {
        if (pin.length <= 8 && pin.all { it.isDigit() }) {
            _newPinInput.value = pin
        }
    }

    fun onConfirmNewPinInputChange(pin: String) {
        if (pin.length <= 8 && pin.all { it.isDigit() }) {
            _confirmNewPinInput.value = pin
        }
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setShowChangePinDialog(show: Boolean) {
        _showChangePinDialog.value = show
        if (!show) {
            _currentPinInput.value = ""
            _newPinInput.value = ""
            _confirmNewPinInput.value = ""
        }
    }

    fun setShowImportDialog(show: Boolean) {
        _showImportDialog.value = show
    }

    fun initializeVault(enableBiometrics: Boolean) {
        val pin = _pinInput.value
        val confirmPin = _confirmPinInput.value
        if (pin.length < 4) {
            viewModelScope.launch { _snackbarEvent.emit("PIN must be 4–8 digits") }
            return
        }
        if (pin != confirmPin) {
            viewModelScope.launch { _snackbarEvent.emit("PINs do not match") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val result = initializeVaultUseCase(pin)
            if (result.isSuccess && enableBiometrics) {
                vaultRepository.setBiometricEnabled(true)
            }
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _pinInput.value = ""
                    _confirmPinInput.value = ""
                    _isBiometricEnabled.value = vaultRepository.isBiometricEnabled()
                    _snackbarEvent.emit("Vault initialized successfully")
                },
                onFailure = { error ->
                    _snackbarEvent.emit(error.message ?: "Failed to initialize vault")
                }
            )
        }
    }

    fun unlockWithPin() {
        val pin = _pinInput.value
        if (pin.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val result = unlockVaultWithPinUseCase(pin)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _pinInput.value = ""
                },
                onFailure = { error ->
                    _snackbarEvent.emit(error.message ?: "Incorrect PIN")
                }
            )
        }
    }

    fun unlockWithBiometrics(activity: FragmentActivity) {
        if (!isBiometricSupported || !_isBiometricEnabled.value) return
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModelScope.launch(Dispatchers.IO) {
                    val res = unlockVaultWithBiometricUseCase()
                    res.fold(
                        onSuccess = {
                            _snackbarEvent.emit("Vault unlocked with biometric")
                        },
                        onFailure = { err ->
                            _snackbarEvent.emit(err.message ?: "Biometric unlock failed")
                        }
                    )
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    viewModelScope.launch { _snackbarEvent.emit(errString.toString()) }
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Secure Vault")
            .setSubtitle("Authenticate using your biometric credentials")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun lockVault() {
        viewModelScope.launch(Dispatchers.IO) {
            lockVaultUseCase()
            _pinInput.value = ""
            _snackbarEvent.emit("Vault locked")
        }
    }

    fun changePin() {
        val currentPin = _currentPinInput.value
        val newPin = _newPinInput.value
        val confirmNew = _confirmNewPinInput.value

        if (newPin.length < 4) {
            viewModelScope.launch { _snackbarEvent.emit("New PIN must be 4–8 digits") }
            return
        }
        if (newPin != confirmNew) {
            viewModelScope.launch { _snackbarEvent.emit("New PINs do not match") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val result = changeVaultPinUseCase(currentPin, newPin)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    setShowChangePinDialog(false)
                    _snackbarEvent.emit("PIN changed successfully")
                },
                onFailure = { err ->
                    _snackbarEvent.emit(err.message ?: "Failed to change PIN")
                }
            )
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = vaultRepository.setBiometricEnabled(enabled)
            if (res.isSuccess) {
                _isBiometricEnabled.value = enabled
                _snackbarEvent.emit(if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled")
            }
        }
    }

    fun importDownloadToVault(task: DownloadTask) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val result = moveToVaultUseCase(task.id)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _snackbarEvent.emit("Added \"${task.title}\" to Vault")
                },
                onFailure = { err ->
                    _snackbarEvent.emit(err.message ?: "Failed to import to vault")
                }
            )
        }
    }

    fun removeFromVault(item: VaultItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val result = removeFromVaultUseCase(item.id)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _snackbarEvent.emit("Restored \"${item.fileName}\" to downloads")
                },
                onFailure = { err ->
                    _snackbarEvent.emit(err.message ?: "Failed to restore file")
                }
            )
        }
    }

    fun deletePermanently(item: VaultItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val res = vaultRepository.deleteVaultItem(item.id)
            _isLoading.value = false
            if (res.isSuccess) {
                _snackbarEvent.emit("Permanently deleted \"${item.fileName}\"")
            } else {
                _snackbarEvent.emit("Failed to delete item")
            }
        }
    }
}
