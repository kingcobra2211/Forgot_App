package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Memory
import com.example.ui.components.MemoryCard
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.viewmodel.MemoryViewModel
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MemoryViewModel,
    onNavigateToRemember: (memoryId: Int?, category: String?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToReminders: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val activeMemories by viewModel.activeMemories.collectAsState()
    val reminders by viewModel.activeReminders.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    // Determine current time-of-day greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingKey = when {
        hour in 5..11 -> "greeting_morning"
        hour in 12..16 -> "greeting_afternoon"
        else -> "greeting_evening"
    }
    
    // Stats Calculations
    val totalCount = activeMemories.size
    val completedCount = activeMemories.count { it.status == "Completed" }
    val pendingCount = reminders.count { it.reminderDate != null && it.reminderDate > System.currentTimeMillis() }

    val pinnedMemories = remember(activeMemories) {
        activeMemories.filter { it.isPinned }
    }
    val unpinnedMemories = remember(activeMemories) {
        activeMemories.filter { !it.isPinned }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // App Logo / Title & Simple Billing/Premium Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = LanguageUtils.getString("app_title", language),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = LanguageUtils.getString("tagline", language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Billing/Premium Free indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "v1.0 Free",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("home_screen_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Greeting & Interactive search trigger
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = LanguageUtils.getString(greetingKey, language),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = LanguageUtils.getString("greeting_question", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Fake search box
                    Card(
                        onClick = onNavigateToSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_bar_trigger"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = LanguageUtils.getString("search_hint", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Dashboard Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Memories Card
                    Card(
                        modifier = Modifier.weight(1.2f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = LanguageUtils.getString("total_memories", language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Reminders Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToReminders() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = LanguageUtils.getString("pending_items", language),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF57C00),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$pendingCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF57C00)
                                )
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Active",
                                    tint = Color(0xFFF57C00),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Horizontal Categories Quick Filters Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageUtils.getString("categories", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCategory != null) {
                            Text(
                                text = "Clear filter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { viewModel.selectedCategory.value = null }
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CategoryRegistry.categories) { catItem ->
                            val isSelected = selectedCategory?.lowercase() == catItem.name.lowercase()
                            val bg = if (isSelected) catItem.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            val tc = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Card(
                                onClick = {
                                    viewModel.selectedCategory.value = if (isSelected) null else catItem.name
                                },
                                colors = CardDefaults.cardColors(containerColor = bg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = catItem.icon,
                                        contentDescription = catItem.name,
                                        tint = tc,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = LanguageUtils.getString(catItem.name, language),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = tc
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pinned Section
            if (pinnedMemories.isNotEmpty()) {
                item {
                    Text(
                        text = LanguageUtils.getString("pinned_items", language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(pinnedMemories) { memory ->
                    MemoryCard(
                        memory = memory,
                        language = language,
                        onEdit = { onNavigateToRemember(memory.id, null) },
                        onPinToggle = { pinned -> viewModel.pinMemory(memory, pinned) },
                        onFavoriteToggle = { fav -> viewModel.favoriteMemory(memory, fav) },
                        onArchiveToggle = { viewModel.archiveMemory(memory) },
                        onDelete = { viewModel.moveMemoryToTrash(memory) },
                        onUpdateChecklist = { newJson -> viewModel.updateMemory(memory.copy(checklistJson = newJson)) },
                        onUpdatePaidStatus = { paid -> viewModel.updateMemory(memory.copy(isPaid = paid)) }
                    )
                }
            }

            // Recent Section Header
            item {
                Text(
                    text = if (selectedCategory != null) "Filtered Memories" else LanguageUtils.getString("recent_memories", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Filter memories by category if filter is active
            val filteredUnpinned = if (selectedCategory != null) {
                unpinnedMemories.filter { it.category.lowercase() == selectedCategory!!.lowercase() }
            } else {
                unpinnedMemories
            }

            if (filteredUnpinned.isEmpty()) {
                item {
                    // Modern Empty State Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.StickyNote2,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = LanguageUtils.getString("no_memories_found", language),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = LanguageUtils.getString("empty_state_tip", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredUnpinned) { memory ->
                    MemoryCard(
                        memory = memory,
                        language = language,
                        onEdit = { onNavigateToRemember(memory.id, null) },
                        onPinToggle = { pinned -> viewModel.pinMemory(memory, pinned) },
                        onFavoriteToggle = { fav -> viewModel.favoriteMemory(memory, fav) },
                        onArchiveToggle = { viewModel.archiveMemory(memory) },
                        onDelete = { viewModel.moveMemoryToTrash(memory) },
                        onUpdateChecklist = { newJson -> viewModel.updateMemory(memory.copy(checklistJson = newJson)) },
                        onUpdatePaidStatus = { paid -> viewModel.updateMemory(memory.copy(isPaid = paid)) }
                    )
                }
            }
        }
    }
}
