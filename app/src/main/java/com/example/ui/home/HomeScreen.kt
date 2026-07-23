package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemoryWithDetails
import com.example.ui.components.MemoryCard
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.utils.LocalResponsiveMetrics
import com.example.ui.viewmodel.MemoryViewModel
import java.text.SimpleDateFormat
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
    val metrics = LocalResponsiveMetrics.current
    val isCompact = metrics.widthSizeClass == WindowWidthSizeClass.Compact

    // Calculate dynamically sorted categories based on frequency of usage
    val categoryUsageCounts = remember(activeMemories) {
        activeMemories.groupBy { it.memory.category }.mapValues { it.value.size }
    }
    val sortedCategories = remember(categoryUsageCounts) {
        CategoryRegistry.categories.sortedByDescending { categoryUsageCounts[it.name] ?: 0 }
    }

    // Filter upcoming active reminders
    val upcomingReminders = remember(activeMemories) {
        val now = System.currentTimeMillis()
        activeMemories
            .filter { it.memory.reminderDate != null && it.memory.reminderDate!! > now }
            .sortedBy { it.memory.reminderDate }
    }

    // Separate pinned and recent memories
    val pinnedMemories = remember(activeMemories) {
        activeMemories.filter { it.memory.isPinned }
    }
    val unpinnedMemories = remember(activeMemories) {
        activeMemories.filter { !it.memory.isPinned }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = metrics.horizontalPadding, vertical = metrics.verticalPadding)
            ) {
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
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    // Version Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = metrics.horizontalPadding / 2, vertical = 6.dp)
                    ) {
                        Text(
                            text = "v1.0 Pro",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val columns = if (isCompact) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 340.dp)
        
        LazyVerticalGrid(
            columns = columns,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("home_screen_grid"),
            contentPadding = PaddingValues(
                start = metrics.horizontalPadding,
                end = metrics.horizontalPadding,
                top = metrics.verticalPadding,
                bottom = metrics.verticalPadding * 2
            ),
            horizontalArrangement = Arrangement.spacedBy(metrics.gridSpacing),
            verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing)
        ) {
            // Header: Dynamic memory focus prompt
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Save before you forget.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "What would you like to remember today?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    onClick = onNavigateToSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = metrics.searchBarHeight)
                        .testTag("home_search_bar_trigger"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(metrics.cardCornerRadius),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = LanguageUtils.getString("search_hint", language),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 2. PRIORITIZED SECTION: Quick Capture (sorted by frequency of use)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Quick Capture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sortedCategories) { catItem ->
                            val count = categoryUsageCounts[catItem.name] ?: 0
                            Card(
                                onClick = {
                                    onNavigateToRemember(null, catItem.name)
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = catItem.color.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .testTag("quick_capture_${catItem.name.lowercase()}")
                                    .widthIn(min = 130.dp, max = 180.dp)
                                    .height(85.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(catItem.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catItem.icon,
                                            contentDescription = catItem.name,
                                            tint = catItem.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = LanguageUtils.getString(catItem.name, language),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$count saved",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = catItem.color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. PRIORITIZED SECTION: Upcoming Reminders
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminders",
                                tint = Color(0xFFF57C00),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upcoming Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (upcomingReminders.isNotEmpty()) {
                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onNavigateToReminders() }
                            )
                        }
                    }

                    if (upcomingReminders.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "No upcoming reminders",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "All Caught Up!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "No reminders scheduled for future.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(upcomingReminders) { memoryWithDetails ->
                                val memory = memoryWithDetails.memory
                                val catItem = CategoryRegistry.getCategoryItem(memory.category)
                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(memory.reminderDate!!))
                                Card(
                                    onClick = { onNavigateToRemember(memory.id, null) },
                                    modifier = Modifier
                                        .widthIn(min = 190.dp, max = 260.dp)
                                        .testTag("upcoming_reminder_card_${memory.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(catItem.color.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = catItem.icon,
                                                    contentDescription = memory.category,
                                                    tint = catItem.color,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFFFF3E0))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "ALARM",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFE65100),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Text(
                                            text = memory.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE65100),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. PRIORITIZED SECTION: Pinned Memories
            if (pinnedMemories.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pinned Memories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                items(pinnedMemories) { memoryWithDetails ->
                    val memory = memoryWithDetails.memory
                    MemoryCard(
                        memoryWithDetails = memoryWithDetails,
                        language = language,
                        onEdit = { onNavigateToRemember(memory.id, null) },
                        onPinToggle = { pinned -> viewModel.pinMemory(memory, pinned) },
                        onFavoriteToggle = { fav -> viewModel.favoriteMemory(memory, fav) },
                        onArchiveToggle = { viewModel.archiveMemory(memory) },
                        onDelete = { viewModel.moveMemoryToTrash(memory) },
                        onUpdateChecklist = { newItems ->
                            val updatedDetail = memoryWithDetails.shoppingDetail?.copy(shoppingItems = newItems)
                            viewModel.saveMemory(memory, updatedDetail)
                        },
                        onUpdatePaidStatus = { paid ->
                            val updatedDetail = memoryWithDetails.moneyDetail?.copy(status = if (paid) "Returned" else "Pending")
                            viewModel.saveMemory(memory, updatedDetail)
                        }
                    )
                }
            }

            // 5. PRIORITIZED SECTION: Recent Memories
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (selectedCategory != null) "Filtered Memories" else "Recent Memories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (selectedCategory != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.selectedCategory.value = null }
                        ) {
                            Text(
                                text = "Filtering by ${LanguageUtils.getString(selectedCategory!!, language)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear filter",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Apply filter logic
            val filteredUnpinned = if (selectedCategory != null) {
                unpinnedMemories.filter { it.memory.category.lowercase() == selectedCategory!!.lowercase() }
            } else {
                unpinnedMemories
            }

            if (filteredUnpinned.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAlt,
                                contentDescription = "No memories yet",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Save your first memory!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Write down where you put your keys, your parking spot, medicine dosage, or checklist. It takes just 5 seconds.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigateToRemember(null, "Note") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Create", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Memory Now", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredUnpinned) { memoryWithDetails ->
                    val memory = memoryWithDetails.memory
                    MemoryCard(
                        memoryWithDetails = memoryWithDetails,
                        language = language,
                        onEdit = { onNavigateToRemember(memory.id, null) },
                        onPinToggle = { pinned -> viewModel.pinMemory(memory, pinned) },
                        onFavoriteToggle = { fav -> viewModel.favoriteMemory(memory, fav) },
                        onArchiveToggle = { viewModel.archiveMemory(memory) },
                        onDelete = { viewModel.moveMemoryToTrash(memory) },
                        onUpdateChecklist = { newItems ->
                            val updatedDetail = memoryWithDetails.shoppingDetail?.copy(shoppingItems = newItems)
                            viewModel.saveMemory(memory, updatedDetail)
                        },
                        onUpdatePaidStatus = { paid ->
                            val updatedDetail = memoryWithDetails.moneyDetail?.copy(status = if (paid) "Returned" else "Pending")
                            viewModel.saveMemory(memory, updatedDetail)
                        }
                    )
                }
            }
        }
    }
}
