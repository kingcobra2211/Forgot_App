package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ReleaseInfo
import com.example.data.model.UpdateState
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
    private val updatePreferences = UpdatePreferences(application)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

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
        // Collect downloadManager state changes and update updateState
        viewModelScope.launch {
            downloadManager.state.collect { dState ->
                val release = _latestReleaseInfo.value
                when (dState) {
                    is DownloadState.Downloading -> {
                        if (release != null) {
                            _updateState.value = UpdateState.Downloading(
                                progress = dState.progress,
                                speedKb = dState.speedKb,
                                downloadedBytes = dState.downloadedBytes,
                                totalBytes = dState.totalBytes,
                                etaSeconds = dState.etaSeconds,
                                release = release
                            )
                        }
                    }
                    is DownloadState.Completed -> {
                        if (release != null) {
                            updatePreferences.saveDownloadInfo(release.tagName, dState.file.absolutePath)
                            _updateState.value = UpdateState.DownloadCompleted(dState.file, release)
                        }
                    }
                    is DownloadState.Failed -> {
                        _updateState.value = UpdateState.Error(dState.error)
                        _error.value = dState.error
                    }
                    else -> {}
                }
            }
        }

        // Automatically check for updates on startup if preference is enabled
        if (updateRepository.isAutoCheckOnStartup()) {
            checkForUpdates(isAutoCheck = true)
        }
    }

    fun checkForUpdates(isAutoCheck: Boolean = false) {
        if (_isCheckingUpdates.value) return
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            _updateState.value = UpdateState.Checking
            _error.value = null

            val result = updateRepository.fetchLatestRelease()
            _isCheckingUpdates.value = false

            result.onSuccess { release ->
                _latestReleaseInfo.value = release
                updateRepository.setLastCheckedTime(System.currentTimeMillis())
                _lastCheckedTime.value = updateRepository.getLastCheckedTime()

                // Check version comparison
                val isNewer = VersionComparator.compare(currentVersion, release.tagName) < 0

                if (isNewer) {
                    val skipped = updateRepository.getSkippedVersion()
                    if (isAutoCheck && skipped == release.tagName) {
                        _isUpdateAvailable.value = false
                        _updateState.value = UpdateState.UpToDate
                    } else {
                        _isUpdateAvailable.value = true
                        
                        // Check if we already have this specific version downloaded locally
                        val downloadedVer = updatePreferences.getDownloadedVersion()
                        val downloadedPath = updatePreferences.getDownloadedApkPath()

                        if (downloadedVer == release.tagName && downloadedPath != null) {
                            val file = File(downloadedPath)
                            if (file.exists() && file.length() > 0) {
                                _updateState.value = UpdateState.DownloadCompleted(file, release)
                                return@onSuccess
                            }
                        }

                        _updateState.value = UpdateState.UpdateAvailable(release)
                    }
                } else {
                    _isUpdateAvailable.value = false
                    _updateState.value = UpdateState.UpToDate

                    // --- Post-Update Cleanup ---
                    // If current app version matches what we last downloaded, delete the old APK
                    val lastDownloadedVersion = updatePreferences.getDownloadedVersion()
                    val lastDownloadedPath = updatePreferences.getDownloadedApkPath()

                    if (lastDownloadedPath != null) {
                        val file = File(lastDownloadedPath)
                        if (file.exists()) {
                            file.delete()
                        }
                        updatePreferences.clearDownloadInfo()
                    }
                }
            }.onFailure { exception ->
                _isUpdateAvailable.value = false
                val errorMsg = exception.localizedMessage ?: "Failed to fetch update info from server."
                _updateState.value = UpdateState.Error(errorMsg)
                if (!isAutoCheck) {
                    _error.value = errorMsg
                }
            }
        }
    }

    fun startDownload(release: ReleaseInfo) {
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
        val downloadUrl = apkAsset?.browserDownloadUrl 
            ?: "https://github.com/vamshivamshi9630/Forgot_App_Latest_Versions/releases/download/${release.tagName}/app-release.apk"
        val fileName = apkAsset?.name ?: "Forgot_${release.tagName}.apk"

        startDownload(downloadUrl, fileName)
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
        val release = _latestReleaseInfo.value
        if (release != null) {
            _updateState.value = UpdateState.UpdateAvailable(release)
        } else {
            _updateState.value = UpdateState.Idle
        }
    }

    fun retryDownload() {
        downloadManager.retryDownload()
    }

    fun installApk(file: File) {
        downloadManager.installApk(file)
    }

    fun skipVersion(version: String) {
        updateRepository.setSkippedVersion(version)
        _skippedVersion.value = version
        _isUpdateAvailable.value = false
        _updateState.value = UpdateState.UpToDate
    }

    fun resetSkippedVersion() {
        updateRepository.setSkippedVersion(null)
        _skippedVersion.value = null
    }

    fun setAutoCheckOnStartup(enabled: Boolean) {
        updateRepository.setAutoCheckOnStartup(enabled)
        _autoCheckOnStartup.value = enabled
    }

    fun shareApp() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = apkShareManager.shareInstalledAPK(currentVersion)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.onFailure { e ->
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Unable to share APK: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun dismissUpdateDialog() {
        _isUpdateAvailable.value = false
    }
}
