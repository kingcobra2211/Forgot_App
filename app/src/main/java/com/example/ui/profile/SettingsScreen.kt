package com.example.ui.profile

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.data.model.MemoryWithDetails
import com.example.ui.components.MemoryCard
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.utils.LocalResponsiveMetrics
import com.example.ui.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MemoryViewModel,
    updateViewModel: com.example.ui.viewmodel.UpdateViewModel,
    onNavigateToRemember: (memoryId: Int?, category: String?) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val themeKey by viewModel.themeKey.collectAsState()
    
    val activeMemories by viewModel.activeMemories.collectAsState()
    val archivedMemories by viewModel.archivedMemories.collectAsState()
    val trashMemories by viewModel.trashMemories.collectAsState()

    val isCheckingUpdates by updateViewModel.isCheckingUpdates.collectAsState()
    val autoCheckOnStartup by updateViewModel.autoCheckOnStartup.collectAsState()
    val lastCheckedTime by updateViewModel.lastCheckedTime.collectAsState()
    val skippedVersion by updateViewModel.skippedVersion.collectAsState()
    val latestReleaseInfo by updateViewModel.latestReleaseInfo.collectAsState()
    val updateError by updateViewModel.error.collectAsState()

    var showArchive by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val metrics = LocalResponsiveMetrics.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageUtils.getString("settings_tab", language),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
                    bottom = metrics.verticalPadding * 2
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
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), thickness = 1.dp)
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

                // Layout in an elegant Adaptive Grid
                val columns = if (metrics.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact) 2 else 3
                
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
                                                // Dynamic Dot Indicator
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

            // 3. LANGUAGE SELECTOR SECTION
            Column(verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "Languages", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(
                        text = LanguageUtils.getString("language", language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing)
                ) {
                    val isEnglish = language.lowercase() == "english"
                    val isTelugu = language.lowercase() == "telugu"

                    // English Card
                    Card(
                        onClick = { viewModel.updateLanguage("english") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(metrics.cardCornerRadius / 1.25f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEnglish) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = if (isEnglish) null else CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(metrics.itemSpacing),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "English 🇺🇸",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.bodyFontSize),
                                fontWeight = FontWeight.Bold,
                                color = if (isEnglish) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Telugu Card
                    Card(
                        onClick = { viewModel.updateLanguage("telugu") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(metrics.cardCornerRadius / 1.25f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTelugu) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = if (isTelugu) null else CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(metrics.itemSpacing),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "తెలుగు 🇮🇳",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.bodyFontSize),
                                fontWeight = FontWeight.Bold,
                                color = if (isTelugu) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. BACKUP & RESTORE SECTION
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
                    // Export
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

                    // Import
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

            // 5. SHARE APP CARD (NOW SHARES APK + TEXT)
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
                        .clickable {
                            // Share both APK and text for maximum flexibility
                            updateViewModel.shareApp()
                        }
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

            // 6. ARCHIVE COLLAPSIBLE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showArchive = !showArchive }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Archive, 
                                contentDescription = "Archive", 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = LanguageUtils.getString("archive_title", language) + " (${archivedMemories.size})",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Icon(
                            imageVector = if (showArchive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showArchive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (archivedMemories.isEmpty()) {
                                Text(
                                    text = "No items in Archive.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 4.dp)
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
                }
            }

            // 7. TRASH COLLAPSIBLE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTrash = !showTrash }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete, 
                                contentDescription = "Trash", 
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = LanguageUtils.getString("trash_title", language) + " (${trashMemories.size})",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFEF5350)
                            )
                        }
                        Icon(
                            imageVector = if (showTrash) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = Color(0xFFEF5350)
                        )
                    }

                    AnimatedVisibility(visible = showTrash) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (trashMemories.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.emptyTrash() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 4.dp)
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
                }
            }

            // 8. UPDATED ABOUT & APP UPDATES SECTION
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                // Card A: App Updates Configuration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Updates Settings",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "App Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Last Checked and Settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Auto Check on Startup",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Fetch updates when launching",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoCheckOnStartup,
                                onCheckedChange = { updateViewModel.setAutoCheckOnStartup(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                        // Manual Check Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Last Checked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (lastCheckedTime > 0) {
                                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastCheckedTime))
                                    } else "Never checked",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { updateViewModel.checkForUpdates(isAutoCheck = false) },
                                enabled = !isCheckingUpdates,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Checking...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Check", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check Now", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (updateError != null) {
                            Text(
                                text = "Error: $updateError",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Card B: Brand info, app stats, developer, privacy, licenses
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Premium Centered App Logo & Info
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SdCard,
                                    contentDescription = "Forgot Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "FORGOT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Forgot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Your Instant Personal Memory Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Version ${updateViewModel.currentVersion} (Build ${updateViewModel.buildNumber})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Developed with ❤️ by Forgot Team",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                        // Actions List: Release Notes, Privacy Policy, Licenses
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 1. View Release Notes with Update Badge
                            val hasUpdate = latestReleaseInfo != null && 
                                latestReleaseInfo?.tagName?.let { tag ->
                                    !tag.equals(updateViewModel.currentVersion, ignoreCase = true)
                                } ?: false
                             
                            AboutListItem(
                                icon = Icons.Default.Feed,
                                title = "See What's New",
                                subtitle = "Latest features and improvements",
                                onClick = {
                                    if (latestReleaseInfo == null) {
                                        updateViewModel.checkForUpdates(isAutoCheck = true)
                                    }
                                    showReleaseNotesDialog = true
                                },
                                showBadge = hasUpdate
                            )

                            // 3. Privacy Policy
                            AboutListItem(
                                icon = Icons.Default.Security,
                                title = "Privacy Policy",
                                subtitle = "Our offline-first data safety commitment",
                                onClick = { showPrivacyDialog = true }
                            )

                            // 4. Open Source Licenses
                            AboutListItem(
                                icon = Icons.Default.CollectionsBookmark,
                                title = "Open Source Licenses",
                                subtitle = "Third-party libraries powering the memory engine",
                                onClick = { showLicensesDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = { Icon(imageVector = Icons.Default.Security, contentDescription = "Privacy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your Privacy is Our Absolute Priority.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Forgot is designed as a fully offline-first application. All memories, notes, categorizations, custom attributes, photo paths, voice tracks, and location data are saved and processed directly on your physical Android device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Zero Cloud Uploads: Your data never leaves your device.\n• No Analytics or Trackers: We collect absolutely no usage logs, telemetry, or identifiers.\n• Local Databases: Employs standard local SQLite encrypted by Android system permissions.\n• Backups: Local JSON exports are stored in your chosen location via secure system file picker and managed entirely by you.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("I Understand")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses", fontWeight = FontWeight.Black) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    LicenseItem(
                        library = "Jetpack Compose",
                        author = "Google LLC",
                        license = "Apache License 2.0"
                    )
                    LicenseItem(
                        library = "Room Persistence Database",
                        author = "Google LLC",
                        license = "Apache License 2.0"
                    )
                    LicenseItem(
                        library = "Retrofit REST Client",
                        author = "Square Inc.",
                        license = "Apache License 2.0"
                    )
                    LicenseItem(
                        library = "OkHttp & Logging Interceptor",
                        author = "Square Inc.",
                        license = "Apache License 2.0"
                    )
                    LicenseItem(
                        library = "Moshi JSON Parser",
                        author = "Square Inc.",
                        license = "Apache License 2.0"
                    )
                    LicenseItem(
                        library = "Coil Image Loader",
                        author = "Coil Contributors",
                        license = "Apache License 2.0"
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Release Notes Dialog
    if (showReleaseNotesDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseNotesDialog = false },
            title = { Text("What's New", fontWeight = FontWeight.Bold) },
            text = {
                val release = latestReleaseInfo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (release != null) {
                        Text(
                            text = "Version ${release.tagName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = release.body ?: "No release notes available.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "Click 'Check Now' on the Updates panel to see the latest release information.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showReleaseNotesDialog = false }) {
                    Text("Done")
                }
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
            .padding(vertical = metrics.itemSpacing / 1.5f, horizontal = 6.dp),
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
fun LicenseItem(library: String, author: String, license: String) {
    val metrics = LocalResponsiveMetrics.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(metrics.cardCornerRadius / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(metrics.itemSpacing),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = library, style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.bodyFontSize), fontWeight = FontWeight.Bold)
        Text(text = "Copyright © $author", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "Licensed under $license", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
    }
}
