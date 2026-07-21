package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

sealed interface DownloadState {
    object Idle : DownloadState
    
    data class Downloading(
        val progress: Int,
        val speedKb: Double,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val etaSeconds: Long
    ) : DownloadState
    
    object Paused : DownloadState
    
    data class Completed(val file: File) : DownloadState
    
    data class Failed(val error: String) : DownloadState
}

class DownloadManager(private val context: Context) {

    private val notificationHelper = NotificationHelper(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state

    private var downloadJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Current download parameters
    private var downloadUrl: String? = null
    private var destFile: File? = null
    private var totalBytes: Long = 0L
    private var downloadedBytes: Long = 0L

    fun startDownload(url: String, fileName: String) {
        cancelDownload()
        downloadUrl = url
        val updatesDir = File(context.filesDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }
        destFile = File(updatesDir, fileName)
        downloadedBytes = 0L
        totalBytes = 0L

        executeDownload()
    }

    fun pauseDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = DownloadState.Paused
    }

    fun resumeDownload() {
        if (downloadUrl != null && destFile != null) {
            _state.value = DownloadState.Downloading(
                progress = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0,
                speedKb = 0.0,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                etaSeconds = 0
            )
            executeDownload(resume = true)
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        try {
            destFile?.let {
                if (it.exists()) it.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        destFile = null
        downloadUrl = null
        downloadedBytes = 0L
        totalBytes = 0L
        notificationHelper.cancelNotification()
        _state.value = DownloadState.Idle
    }

    fun retryDownload() {
        val url = downloadUrl
        val file = destFile
        if (url != null && file != null) {
            startDownload(url, file.name)
        }
    }

    private fun executeDownload(resume: Boolean = false) {
        downloadJob = scope.launch {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                val url = downloadUrl ?: return@launch
                val file = destFile ?: return@launch

                val startByte = if (resume && file.exists()) file.length() else 0L
                downloadedBytes = startByte

                val requestBuilder = Request.Builder().url(url)
                if (startByte > 0) {
                    requestBuilder.addHeader("Range", "bytes=$startByte-")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val code = response.code

                if (startByte > 0 && code != 206) {
                    // Range not supported, restart download from beginning
                    executeDownload(resume = false)
                    return@launch
                }

                if (!response.isSuccessful) {
                    throw Exception("Server returned HTTP error code: $code")
                }

                val body = response.body ?: throw Exception("Empty response body from server.")
                val responseLength = body.contentLength()
                
                if (startByte == 0L) {
                    totalBytes = responseLength
                } else if (totalBytes == 0L) {
                    totalBytes = startByte + responseLength
                }

                inputStream = body.byteStream()
                // Append mode if we are resuming
                outputStream = FileOutputStream(file, resume && startByte > 0)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                var bytesWrittenInSample = 0L
                var lastSampleTime = System.currentTimeMillis()

                // Initial update
                updateProgress(bytesWrittenInSample, lastSampleTime)

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesWrittenInSample += bytesRead

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSampleTime
                    if (elapsed >= 800) { // Update status every 800ms to reduce UI overhead but maintain freshness
                        updateProgress(bytesWrittenInSample, lastSampleTime)
                        bytesWrittenInSample = 0L
                        lastSampleTime = now
                    }
                }

                outputStream.flush()
                
                // Done!
                _state.value = DownloadState.Completed(file)
                notificationHelper.showDownloadCompleted(file)

            } catch (e: Exception) {
                if (downloadJob?.isCancelled == true) {
                    _state.value = DownloadState.Paused
                } else {
                    val errorMsg = e.localizedMessage ?: "Unknown download error"
                    _state.value = DownloadState.Failed(errorMsg)
                    notificationHelper.showDownloadFailed(errorMsg)
                }
            } finally {
                try {
                    inputStream?.close()
                    outputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateProgress(bytesInSample: Long, sampleStartTime: Long) {
        val total = totalBytes
        val downloaded = downloadedBytes
        if (total <= 0) return

        val now = System.currentTimeMillis()
        val elapsed = now - sampleStartTime
        
        val speedKb = if (elapsed > 0) {
            (bytesInSample / 1024.0) / (elapsed / 1000.0)
        } else 0.0

        val remainingBytes = total - downloaded
        val etaSeconds = if (speedKb > 0) {
            (remainingBytes / (speedKb * 1024.0)).toLong()
        } else 0L

        val progress = ((downloaded * 100) / total).toInt()

        _state.value = DownloadState.Downloading(
            progress = progress,
            speedKb = speedKb,
            downloadedBytes = downloaded,
            totalBytes = total,
            etaSeconds = etaSeconds
        )

        notificationHelper.showDownloadProgress(
            progress = progress,
            speedKb = speedKb,
            downloadedMb = downloaded / (1024.0 * 1024.0),
            totalMb = total / (1024.0 * 1024.0)
        )
    }

    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "Installation file not found.", Toast.LENGTH_SHORT).show()
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Installation initiation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
