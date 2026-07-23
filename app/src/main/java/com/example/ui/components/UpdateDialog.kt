package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ReleaseInfo
import com.example.data.repository.DownloadState
import com.example.ui.utils.LocalResponsiveMetrics
import com.example.ui.viewmodel.UpdateViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel,
    onDismiss: () -> Unit
) {
    val latestRelease by viewModel.latestReleaseInfo.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val metrics = LocalResponsiveMetrics.current
    
    val release = latestRelease ?: return
    val apkAsset = remember(release) {
        release.assets.firstOrNull { it.name.endsWith(".apk") }
    }

    Dialog(
        onDismissRequest = {
            // Only allow dismissal if we are not actively downloading or if we click cancel explicitly
            if (downloadState !is DownloadState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = downloadState !is DownloadState.Downloading,
            dismissOnClickOutside = downloadState !is DownloadState.Downloading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(metrics.horizontalPadding / 2),
            shape = RoundedCornerShape(metrics.cardCornerRadius + 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(metrics.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing)
            ) {
                // Header with App Name and Update Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Forgot App Update",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "A new update is available!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Toggle views based on DownloadState
                when (val state = downloadState) {
                    is DownloadState.Idle, is DownloadState.Paused, is DownloadState.Failed -> {
                        // 1. UPDATE DISCOVERY VIEW
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Versions Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoTag(
                                    label = "Current Version",
                                    value = viewModel.currentVersion,
                                    modifier = Modifier.weight(1f)
                                )
                                InfoTag(
                                    label = "Latest Version",
                                    value = release.tagName,
                                    modifier = Modifier.weight(1f),
                                    highlight = true
                                )
                            }

                            // Meta details: Published Date & Size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoTag(
                                    label = "Release Date",
                                    value = formatPublishedDate(release.publishedAt),
                                    modifier = Modifier.weight(1f)
                                )
                                InfoTag(
                                    label = "APK Size",
                                    value = if (apkAsset != null) formatBytes(apkAsset.size) else "Unknown",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Release Notes / Changelog Box
                            Text(
                                text = "What's New:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(12.dp)
                            ) {
                                val changelogScroll = rememberScrollState()
                                val rawNotes = release.body ?: "No release notes provided."
                                Text(
                                    text = cleanMarkdown(rawNotes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(changelogScroll)
                                )
                            }

                            // Show download error if previous attempt failed
                            if (state is DownloadState.Failed) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Error",
                                            tint = Color(0xFFC62828)
                                        )
                                        Text(
                                            text = state.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.skipVersion(release.tagName) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Skip This")
                                }
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Later")
                                }
                                Button(
                                    onClick = {
                                        if (apkAsset != null) {
                                            viewModel.startDownload(
                                                url = apkAsset.browserDownloadUrl,
                                                fileName = apkAsset.name
                                            )
                                        } else {
                                            // Fallback
                                            viewModel.startDownload(
                                                url = "https://github.com/vamshivamshi9630/Forgot_App_Latest_Versions/releases/download/${release.tagName}/app-release.apk",
                                                fileName = "Forgot_${release.tagName}.apk"
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = if (state is DownloadState.Failed) Icons.Default.Refresh else Icons.Default.Download,
                                        contentDescription = "Update Now",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state is DownloadState.Failed) "Retry" else "Update Now",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    is DownloadState.Downloading -> {
                        // 2. ACTIVE DOWNLOAD PROGRESS VIEW
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Downloading updates...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Linear progress indicator
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Progress stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.progress}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatBytes(state.downloadedBytes) + " / " + formatBytes(state.totalBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Detail stats: Speed, remaining size, ETA
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatLine(
                                        label = "Speed",
                                        value = formatSpeed(state.speedKb)
                                    )
                                    StatLine(
                                        label = "Remaining Size",
                                        value = formatBytes(state.totalBytes - state.downloadedBytes)
                                    )
                                    StatLine(
                                        label = "Time Remaining",
                                        value = formatEta(state.etaSeconds)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Action Buttons: Pause/Resume, Cancel
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.cancelDownload() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(imageVector = Icons.Default.Cancel, contentDescription = "Cancel")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { viewModel.pauseDownload() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause", color = Color.White)
                                }
                            }
                        }
                    }
                    is DownloadState.Completed -> {
                        // 3. DOWNLOAD COMPLETE / READY TO INSTALL VIEW
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(56.dp)
                            )

                            Text(
                                text = "Download Complete!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )

                            Text(
                                text = "The latest APK is verified and ready to install.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Dismiss")
                                }
                                Button(
                                    onClick = { viewModel.installApk(state.file) },
                                    modifier = Modifier.weight(1.5f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(imageVector = Icons.Default.PlayForWork, contentDescription = "Install", tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Install Now", fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoTag(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun StatLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatPublishedDate(publishedAt: String?): String {
    if (publishedAt == null) return "Unknown"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(publishedAt) ?: return publishedAt
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        publishedAt
    }
}

private fun formatBytes(bytes: Long): String {
    return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun formatSpeed(speedKb: Double): String {
    return if (speedKb > 1024.0) {
        String.format(Locale.getDefault(), "%.2f MB/s", speedKb / 1024.0)
    } else {
        String.format(Locale.getDefault(), "%.1f kB/s", speedKb)
    }
}

private fun formatEta(seconds: Long): String {
    if (seconds <= 0) return "Calculating..."
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) {
        "${mins}m ${secs}s remaining"
    } else {
        "${secs}s remaining"
    }
}

private fun cleanMarkdown(notes: String): String {
    // Strip redundant headers, bullet lines or formatting to display cleanly in simple text
    return notes
        .replace(Regex("###\\s*"), "")
        .replace(Regex("##\\s*"), "")
        .replace(Regex("#\\s*"), "")
        .replace(Regex("\\*\\*"), "")
        .replace(Regex("\\*"), " • ")
        .replace(Regex("-\\s+"), " • ")
}
