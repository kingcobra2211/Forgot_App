package com.example.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.MemoryWithDetails
import com.example.ui.components.MemoryCard
import com.example.ui.utils.LanguageUtils
import com.example.ui.utils.LocalResponsiveMetrics
import com.example.ui.viewmodel.MemoryViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: MemoryViewModel,
    onNavigateToRemember: (memoryId: Int?, category: String?) -> Unit
) {
    val language by viewModel.language.collectAsState()
    val activeReminders by viewModel.activeReminders.collectAsState()
    val metrics = LocalResponsiveMetrics.current

    // Grouping reminders
    val groupedReminders = remember(activeReminders) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Start and end of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfToday = calendar.timeInMillis

        val missed = mutableListOf<MemoryWithDetails>()
        val today = mutableListOf<MemoryWithDetails>()
        val upcoming = mutableListOf<MemoryWithDetails>()

        for (memory in activeReminders) {
            val date = memory.memory.reminderDate ?: continue
            when {
                date < now && date < startOfToday -> missed.add(memory)
                date in startOfToday..endOfToday -> today.add(memory)
                else -> upcoming.add(memory)
            }
        }
        
        Triple(missed, today, upcoming)
    }

    val (missedReminders, todayReminders, upcomingReminders) = groupedReminders

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageUtils.getString("reminders_tab", language),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (activeReminders.isEmpty()) {
            // Master Empty State: No reminders anywhere in the app
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(metrics.horizontalPadding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(metrics.cardCornerRadius)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.horizontalPadding, vertical = metrics.sectionSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "No reminders active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No active reminders",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = metrics.titleFontSize),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add reminders to your memories (like doctor meetings, money returns, or parking locations) to get notified here.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.bodyFontSize),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onNavigateToRemember(null, "Note") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.AddAlarm, contentDescription = "Add", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set A Reminder", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("reminders_lazy_column"),
                contentPadding = PaddingValues(
                    horizontal = metrics.horizontalPadding,
                    vertical = metrics.verticalPadding
                ),
                verticalArrangement = Arrangement.spacedBy(metrics.gridSpacing)
            ) {
                // Overdue/Missed Section
                if (missedReminders.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning, 
                                contentDescription = "Missed", 
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Missed Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                    items(missedReminders) { memoryWithDetails ->
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

                // Today's Reminders Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today, 
                            contentDescription = "Today", 
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageUtils.getString("todays_reminders", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                if (todayReminders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "No reminders scheduled for today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(todayReminders) { memoryWithDetails ->
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

                // Upcoming Reminders Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event, 
                            contentDescription = "Upcoming", 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageUtils.getString("upcoming_reminders", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (upcomingReminders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "No upcoming reminders scheduled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(upcomingReminders) { memoryWithDetails ->
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
}
