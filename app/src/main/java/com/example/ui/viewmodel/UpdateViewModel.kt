package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ReleaseInfo
import com.example.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val updateRepository = UpdateRepository(application)
    private val downloadManager = DownloadManager(application)
    private val apkShareManager = APKShareManager(application)

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates = _isCheckingUpdates.asStateFlow()

    private val _latestReleaseInfo = MutableStateFlow<ReleaseInfo?>(null)
    val latestReleaseInfo = _latestReleaseInfo.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable = _isUpdateAvailable.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val downloadState: StateFlow<DownloadState> = downloadManager.state

    // Settings configurations
    val currentVersion: String = updateRepository.getCurrentVersion()
    val buildNumber: Long = updateRepository.getBuildNumber()

    private val _autoCheckOnStartup = MutableStateFlow(updateRepository.isAutoCheckOnStartup())
    val autoCheckOnStartup = _autoCheckOnStartup.asStateFlow()

    private val _lastCheckedTime = MutableStateFlow(updateRepository.getLastCheckedTime())
    val lastCheckedTime = _lastCheckedTime.asStateFlow()

    private val _skippedVersion = MutableStateFlow(updateRepository.getSkippedVersion())
    val skippedVersion = _skippedVersion.asStateFlow()

    init {
        // Automatically check for updates on startup if preference is enabled
        if (updateRepository.isAutoCheckOnStartup()) {
            checkForUpdates(isAutoCheck = true)
        }
    }

    fun checkForUpdates(isAutoCheck: Boolean = false) {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            _error.value = null

            val result = updateRepository.fetchLatestRelease()
            _isCheckingUpdates.value = false

            result.onSuccess { release ->
                _latestReleaseInfo.value = release
                updateRepository.setLastCheckedTime(System.currentTimeMillis())
                _lastCheckedTime.value = updateRepository.getLastCheckedTime()

                // If current version is less than the latest release version, an update is available
                val isNewer = VersionComparator.compare(currentVersion, release.tagName) < 0

                if (isNewer) {
                    val skipped = updateRepository.getSkippedVersion()
                    if (isAutoCheck && skipped == release.tagName) {
                        _isUpdateAvailable.value = false
                    } else {
                        _isUpdateAvailable.value = true
                    }
                } else {
                    _isUpdateAvailable.value = false
                }
            }.onFailure { exception ->
                if (!isAutoCheck) {
                    _error.value = exception.localizedMessage ?: "Failed to fetch update info from server."
                }
            }
        }
    }

    fun skipVersion(version: String) {
        updateRepository.setSkippedVersion(version)
        _skippedVersion.value = version
        _isUpdateAvailable.value = false
    }

    fun resetSkippedVersion() {
        updateRepository.setSkippedVersion(null)
        _skippedVersion.value = null
    }

    fun setAutoCheckOnStartup(enabled: Boolean) {
        updateRepository.setAutoCheckOnStartup(enabled)
        _autoCheckOnStartup.value = enabled
    }

    fun startDownload(url: String, fileName: String) {
        downloadManager.startDownload(url, fileName)
    }

    fun pauseDownload() {
        downloadManager.pauseDownload()
    }

    fun resumeDownload() {
        downloadManager.resumeDownload()
    }

    fun cancelDownload() {
        downloadManager.cancelDownload()
    }

    fun retryDownload() {
        downloadManager.retryDownload()
    }

    fun installApk(file: File) {
        downloadManager.installApk(file)
    }

    fun shareApp() {
        apkShareManager.shareInstalledAPK(currentVersion)
    }

    fun clearError() {
        _error.value = null
    }

    fun dismissUpdateDialog() {
        _isUpdateAvailable.value = false
    }
}
