package com.example.data.model

import java.io.File

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
    data class Downloading(
        val progress: Int,
        val speedKb: Double,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val etaSeconds: Long,
        val release: ReleaseInfo
    ) : UpdateState()
    data class DownloadCompleted(val file: File, val release: ReleaseInfo) : UpdateState()
}
