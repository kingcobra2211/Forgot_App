package com.example.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MemoryCard
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.utils.LocalResponsiveMetrics
import com.example.ui.utils.buildFormattedReleaseNotes
import com.example.ui.viewmodel.MemoryViewModel
import com.example.ui.viewmodel.UpdateViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MemoryViewModel,
    updateViewModel: UpdateViewModel,
    onNavigateToRemember: (memoryId: Int?, category: String?) -> Unit,
    onNavigateToAppVersion: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val themeKey by viewModel.themeKey.collectAsState()
    
    val activeMemories by viewModel.activeMemories.collectAsState()
    val archivedMemories by viewModel.archivedMemories.collectAsState()
    val trashMemories by viewModel.trashMemories.collectAsState()

    val isCheckingUpdates by updateViewModel.isCheckingUpdates.collectAsState()
    val isUpdateAvailable by updateViewModel.isUpdateAvailable.collectAsState()
    val autoCheckOnStartup by updateViewModel.autoCheckOnStartup.collectAsState()
    val lastCheckedTime by updateViewModel.lastCheckedTime.collectAsState()
    val latestReleaseInfo by updateViewModel.latestReleaseInfo.collectAsState()
    val updateError by updateViewModel.error.collectAsState()

    var showArchive by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val metrics = LocalResponsiveMetrics.current

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates(isAutoCheck = true)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = metrics.horizontalPadding, vertical = metrics.verticalPadding)
            ) {
                Text(
                    text = LanguageUtils.getString("settings_tab", language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(
                    start = metrics.horizontalPadding,
                    end = metrics.horizontalPadding,
                    top = metrics.verticalPadding,
                    bottom = metrics.verticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing)
        ) {
            
            // 1. STATS OVERVIEW CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Stats",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Memory Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${activeMemories.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Archived", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${archivedMemories.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Trash", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${trashMemories.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text("Category Usage", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CategoryRegistry.categories.forEach { catItem ->
                        val count = (activeMemories + archivedMemories).count { it.memory.category.lowercase() == catItem.name.lowercase() }
                        if (count > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(catItem.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catItem.icon,
                                            contentDescription = catItem.name,
                                            tint = catItem.color,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(LanguageUtils.getString(catItem.name, language), style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("$count items", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // 2. PREMIUM THEME PREVIEW SELECTOR
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = "Themes", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(
                        text = LanguageUtils.getString("theme", language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                
                val themesList = listOf(
                    Triple("dark", "Dark Vibe", Color(0xFF131316)),
                    Triple("light", "Light Vibe", Color(0xFFF4F5F8)),
                    Triple("amoled", "AMOLED Black", Color(0xFF000000)),
                    Triple("blue", "Cyber Blue", Color(0xFF0A192F)),
                    Triple("green", "Mint Green", Color(0xFF0F1E15)),
                    Triple("purple", "Neon Velvet", Color(0xFF12001F))
                )

                val columns = if (metrics.widthSizeClass == com.example.ui.utils.AppWindowWidthClass.Compact) 2 else 3
                
                Column(verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing)) {
                    for (i in themesList.indices step columns) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing)
                        ) {
                            for (j in 0 until columns) {
                                if (i + j < themesList.size) {
                                    val (key, label, previewBg) = themesList[i + j]
                                    val selected = themeKey.lowercase() == key.lowercase()
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(metrics.cardCornerRadius))
                                            .background(previewBg)
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(metrics.cardCornerRadius)
                                            )
                                            .clickable { viewModel.updateTheme(key) }
                                            .padding(horizontal = 14.dp, vertical = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when (key) {
                                                                "dark" -> Color(0xFFADC6FF)
                                                                "light" -> Color(0xFF3F51B5)
                                                                "amoled" -> Color(0xFFFFFFFF)
                                                                "blue" -> Color(0xFF00E5FF)
                                                                "green" -> Color(0xFF2ECC71)
                                                                else -> Color(0xFFE040FB)
                                                            }
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = label, 
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.labelFontSize), 
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (key == "light") Color.Black else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = if (key == "light") Color(0xFF3F51B5) else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 3. BACKUP & RESTORE SECTION
            Column(verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Backup & Local Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing)
                ) {
                    OutlinedButton(
                        onClick = onExportBackup,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(metrics.cardCornerRadius / 1.25f)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Export", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageUtils.getString("export_backup", language),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.bodyFontSize),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onImportBackup,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("import_backup_button"),
                        shape = RoundedCornerShape(metrics.cardCornerRadius / 1.25f)
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = "Import", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageUtils.getString("import_backup", language),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.bodyFontSize),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 4. SHARE APP CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_app_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { updateViewModel.shareApp() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Share Forgot App",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Send the app to friends",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // 5. ARCHIVE & TRASH (Simplified list items in a Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    AboutListItem(
                        icon = Icons.Default.Archive, 
                        title = LanguageUtils.getString("archive_title", language),
                        subtitle = "View and manage archived memories",
                        onClick = { showArchive = !showArchive }
                    )

                    AnimatedVisibility(visible = showArchive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (archivedMemories.isEmpty()) {
                                Text(
                                    text = "No items in Archive.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            } else {
                                archivedMemories.forEach { memoryWithDetails ->
                                    val memory = memoryWithDetails.memory
                                    MemoryCard(
                                        memoryWithDetails = memoryWithDetails,
                                        language = language,
                                        onEdit = { onNavigateToRemember(memory.id, null) },
                                        onPinToggle = { pinned -> viewModel.pinMemory(memory, pinned) },
                                        onFavoriteToggle = { fav -> viewModel.favoriteMemory(memory, fav) },
                                        onArchiveToggle = { viewModel.unarchiveMemory(memory) },
                                        onDelete = { viewModel.moveMemoryToTrash(memory) }
                                    )
                                }
                            }
                        }
                    }

                    AboutListItem(
                        icon = Icons.Default.Delete, 
                        title = "Trash Bin",
                        subtitle = "Manage recently deleted items",
                        onClick = { showTrash = !showTrash }
                    )

                    AnimatedVisibility(visible = showTrash) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (trashMemories.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.emptyTrash() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Purge")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(LanguageUtils.getString("empty_trash", language), fontWeight = FontWeight.Bold)
                                }
                            }

                            if (trashMemories.isEmpty()) {
                                Text(
                                    text = "Trash is empty.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            } else {
                                trashMemories.forEach { memoryWithDetails ->
                                    val memory = memoryWithDetails.memory
                                    MemoryCard(
                                        memoryWithDetails = memoryWithDetails,
                                        language = language,
                                        onEdit = {},
                                        onPinToggle = {},
                                        onFavoriteToggle = {},
                                        onArchiveToggle = { viewModel.restoreMemoryFromTrash(memory) },
                                        onDelete = { viewModel.deleteMemoryPermanently(memory) }
                                    )
                                }
                            }
                        }
                    }

                    AboutListItem(
                        icon = Icons.Default.SystemUpdate, 
                        title = LanguageUtils.getString("App Updates", language),
                        subtitle = if (isUpdateAvailable) "Newer version available" else "App is up to date (v${updateViewModel.currentVersion})",
                        showBadge = isUpdateAvailable,
                        onClick = { 
                            updateViewModel.checkForUpdates(isAutoCheck = false)
                            onNavigateToAppVersion()
                        }
                    )
                }
            }

            // 7. BRAND INFO CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = "Forgot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(text = "Your Personal Memory Engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Version ${updateViewModel.currentVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    val isUpdateAvailableState by updateViewModel.isUpdateAvailable.collectAsState()
                    AboutListItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "Build details and history",
                        onClick = onNavigateToAppVersion,
                        showBadge = isUpdateAvailableState
                    )
                    AboutListItem(
                        icon = Icons.AutoMirrored.Filled.Feed,
                        title = "Release Notes",
                        subtitle = "See what's new",
                        onClick = {
                            if (latestReleaseInfo == null) updateViewModel.checkForUpdates(isAutoCheck = true)
                            showReleaseNotesDialog = true
                        }
                    )
                    AboutListItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "Our commitment to your data",
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Black) },
            text = {
                Text("Forgot is an offline-first app. Your data stays on your device. No analytics, no trackers, no cloud uploads.")
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("OK") }
            }
        )
    }

    if (showReleaseNotesDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseNotesDialog = false },
            title = { Text("What's New", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    val release = latestReleaseInfo
                    if (release != null) {
                        Text("Version ${release.tagName}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(text = buildFormattedReleaseNotes(release.body))
                    } else {
                        Text("Check for updates to see notes.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReleaseNotesDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun AboutListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showBadge: Boolean = false
) {
    val metrics = LocalResponsiveMetrics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(metrics.cardCornerRadius / 2))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF5350))
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}
