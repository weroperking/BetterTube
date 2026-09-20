package com.bettertube.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertube.app.data.preferences.OnboardingPreferences
import com.bettertube.app.data.schedule.ScheduleConfigStore
import com.bettertube.app.data.schedule.ScheduleScheduler
import com.bettertube.app.domain.model.ProxyConfig
import com.bettertube.app.domain.model.ScheduleConfig
import com.bettertube.app.domain.repository.Aria2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aria2Repository: Aria2Repository,
    private val scheduleScheduler: ScheduleScheduler,
    private val scheduleConfigStore: ScheduleConfigStore,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    val daemonRunning: StateFlow<Boolean> = aria2Repository.isDaemonRunning()
    val proxyConfig: StateFlow<ProxyConfig> = aria2Repository.getProxyConfig()
    val scheduleConfig: StateFlow<ScheduleConfig> = aria2Repository.getScheduleConfig()
    val globalSpeedLimit: StateFlow<Long?> = aria2Repository.getGlobalSpeedLimit()

    private val _reduceMotion = MutableStateFlow(onboardingPreferences.isReduceMotionEnabled())
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    fun toggleReduceMotion(enabled: Boolean) {
        onboardingPreferences.setReduceMotionEnabled(enabled)
        _reduceMotion.value = enabled
    }

    private val _aria2Version = MutableStateFlow("Unknown")
    val aria2Version: StateFlow<String> = _aria2Version.asStateFlow()

    private val _aria2Features = MutableStateFlow<List<String>>(emptyList())
    val aria2Features: StateFlow<List<String>> = _aria2Features.asStateFlow()

    private val _maxPeers = MutableStateFlow(128)
    val maxPeers: StateFlow<Int> = _maxPeers.asStateFlow()

    private val _seedTime = MutableStateFlow(0)
    val seedTime: StateFlow<Int> = _seedTime.asStateFlow()

    private val _customHeaders = MutableStateFlow<Map<String, String>>(emptyMap())
    val customHeaders: StateFlow<Map<String, String>> = _customHeaders.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    init {
        loadVersionInfo()
    }

    fun loadVersionInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = aria2Repository.getVersionInfo()
            if (info != null) {
                _aria2Version.value = info.first
                _aria2Features.value = info.second
            }
        }
    }

    fun toggleDaemon(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (enabled) {
                val res = aria2Repository.startDaemon()
                if (res.isSuccess) {
                    _snackbarEvent.emit("aria2 daemon started")
                    loadVersionInfo()
                } else {
                    _snackbarEvent.emit("Failed to start aria2: ${res.exceptionOrNull()?.message}")
                }
            } else {
                aria2Repository.stopDaemon()
                _snackbarEvent.emit("aria2 daemon stopped")
            }
        }
    }

    fun setGlobalSpeedLimit(bytes: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = aria2Repository.setGlobalSpeedLimit(bytes)
            if (res.isSuccess) {
                _snackbarEvent.emit(if (bytes == null || bytes <= 0) "Speed limit set to Unlimited" else "Speed limit updated")
            } else {
                _snackbarEvent.emit("Failed to set speed limit")
            }
        }
    }

    fun saveProxy(config: ProxyConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = aria2Repository.setProxy(config)
            if (res.isSuccess) {
                _snackbarEvent.emit(if (config.enabled) "Proxy enabled: ${config.host}:${config.port}" else "Proxy disabled")
            } else {
                _snackbarEvent.emit("Failed to update proxy settings")
            }
        }
    }

    fun saveHeaders(headers: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = aria2Repository.setCustomHeaders(headers)
            if (res.isSuccess) {
                _customHeaders.value = headers
                _snackbarEvent.emit("Custom headers saved")
            } else {
                _snackbarEvent.emit("Failed to save custom headers")
            }
        }
    }

    fun importCookies(content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = aria2Repository.setCookieFile(content)
            if (res.isSuccess) {
                _snackbarEvent.emit("Cookies imported")
            } else {
                _snackbarEvent.emit("Failed to import cookies")
            }
        }
    }

    fun saveSchedule(config: ScheduleConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = aria2Repository.setScheduleConfig(config)
            if (res.isSuccess) {
                scheduleScheduler.apply(config)
                _snackbarEvent.emit(if (config.enabled) "Download schedule enabled" else "Download schedule disabled")
            } else {
                _snackbarEvent.emit("Failed to save schedule")
            }
        }
    }

    fun setMaxPeers(count: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = aria2Repository.setMaxPeers(count)
            if (res.isSuccess) {
                _maxPeers.value = count
                _snackbarEvent.emit("Max peers set to $count")
            }
        }
    }

    fun setSeedTime(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = aria2Repository.setSeedTime(minutes)
            if (res.isSuccess) {
                _seedTime.value = minutes
                _snackbarEvent.emit("Seed time set to $minutes min")
            }
        }
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarEvent.emit(message)
        }
    }
}
