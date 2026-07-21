package com.example.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.ui.unit.dp
import com.example.data.model.Memory
import com.example.ui.components.MemoryCard
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MemoryViewModel,
    onNavigateToRemember: (memoryId: Int?, category: String?) -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val themeKey by viewModel.themeKey.collectAsState()
    
    val activeMemories by viewModel.activeMemories.collectAsState()
    val archivedMemories by viewModel.archivedMemories.collectAsState()
    val trashMemories by viewModel.trashMemories.collectAsState()

    var showArchive by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Clipboard Managers
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageUtils.getString("settings_tab", language),
                        fontWeight = FontWeight.Black
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 1. STATS OVERVIEW CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Memory Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple progress bar showing category distribution counts
                    Text("Category Usage", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    CategoryRegistry.categories.forEach { catItem ->
                        val count = (activeMemories + archivedMemories).count { it.category.lowercase() == catItem.name.lowercase() }
                        if (count > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = catItem.icon,
                                        contentDescription = catItem.name,
                                        tint = catItem.color,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(LanguageUtils.getString(catItem.name, language), style = MaterialTheme.typography.bodySmall)
                                }
                                Text("$count items", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. THEME SELECTOR SECTION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = LanguageUtils.getString("theme", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val themesList = listOf(
                    Triple("dark", "Dark Vibe", Color(0xFFD0BCFF)),
                    Triple("light", "Light Vibe", Color(0xFF6650A4)),
                    Triple("amoled", "AMOLED Black", Color(0xFFFFFFFF)),
                    Triple("blue", "Cyber Blue", Color(0xFF00E5FF)),
                    Triple("green", "Mint Green", Color(0xFF00E676)),
                    Triple("purple", "Neon Velvet", Color(0xFFE040FB))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    themesList.take(3).forEach { (key, label, color) ->
                        val selected = themeKey.lowercase() == key.lowercase()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable { viewModel.updateTheme(key) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    themesList.drop(3).forEach { (key, label, color) ->
                        val selected = themeKey.lowercase() == key.lowercase()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable { viewModel.updateTheme(key) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. LANGUAGE SELECTOR SECTION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = LanguageUtils.getString("language", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isEnglish = language.lowercase() == "english"
                    val isTelugu = language.lowercase() == "telugu"

                    Button(
                        onClick = { viewModel.updateLanguage("english") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnglish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "English 🇺🇸",
                            fontWeight = FontWeight.Bold,
                            color = if (isEnglish) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.updateLanguage("telugu") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTelugu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "తెలుగు (Telugu) 🇮🇳",
                            fontWeight = FontWeight.Bold,
                            color = if (isTelugu) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // 4. BACKUP & RESTORE SECTION
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Backup & Local Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Export JSON Backup
                    OutlinedButton(
                        onClick = {
                            val backupJson = viewModel.exportBackup(context)
                            if (backupJson != null) {
                                val clip = ClipData.newPlainText("forgot_backup", backupJson)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, LanguageUtils.getString("copied_clipboard", language), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, LanguageUtils.getString("backup_failed", language), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageUtils.getString("export_backup", language),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Import JSON Backup
                    Button(
                        onClick = {
                            showImportDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_backup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Import")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageUtils.getString("import_backup", language),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()

            // 5. ARCHIVE EXPANSION COLLAPSIBLE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showArchive = !showArchive }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = LanguageUtils.getString("archive_title", language) + " (${archivedMemories.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Icon(
                            imageVector = if (showArchive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }

                    AnimatedVisibility(visible = showArchive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (archivedMemories.isEmpty()) {
                                Text(
                                    text = "No items in Archive.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            } else {
                                archivedMemories.forEach { memory ->
                                    MemoryCard(
                                        memory = memory,
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

            // 6. TRASH EXPANSION COLLAPSIBLE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTrash = !showTrash }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Trash", tint = Color(0xFFEF5350))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = LanguageUtils.getString("trash_title", language) + " (${trashMemories.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFEF5350)
                            )
                        }
                        Icon(
                            imageVector = if (showTrash) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }

                    AnimatedVisibility(visible = showTrash) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (trashMemories.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.emptyTrash() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
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
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            } else {
                                trashMemories.forEach { memory ->
                                    MemoryCard(
                                        memory = memory,
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

            // 7. ABOUT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LanguageUtils.getString("about", language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = LanguageUtils.getString("about_desc", language),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Interactive JSON Restorer Modal
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restore Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paste the copied Forgot backup JSON below to restore all your saved memories.")
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("backup_import_textarea"),
                        placeholder = { Text("[ {\"title\": \"Passport\", ...} ]") },
                        maxLines = 10
                    )
                    
                    // Simple click paste option from clipboard
                    Button(
                        onClick = {
                            val clipData = clipboardManager.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                importText = clipData.getItemAt(0).text.toString()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste from Clipboard", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.trim().isNotEmpty()) {
                            val success = viewModel.importBackup(importText.trim())
                            if (success) {
                                Toast.makeText(context, LanguageUtils.getString("backup_restored", language), Toast.LENGTH_LONG).show()
                                showImportDialog = false
                                importText = ""
                            } else {
                                Toast.makeText(context, "Invalid Backup Format.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("confirm_import_backup_button")
                ) {
                    Text("Restore Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
