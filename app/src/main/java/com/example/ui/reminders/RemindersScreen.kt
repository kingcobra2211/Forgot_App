package com.example.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Memory
import com.example.ui.components.MemoryCard
import com.example.ui.utils.LanguageUtils
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

        val missed = mutableListOf<Memory>()
        val today = mutableListOf<Memory>()
        val upcoming = mutableListOf<Memory>()

        for (memory in activeReminders) {
            val date = memory.reminderDate ?: continue
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
                        fontWeight = FontWeight.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("reminders_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overdue/Missed Section
            if (missedReminders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Missed", tint = Color(0xFFEF5350))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Missed Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF5350)
                        )
                    }
                }
                items(missedReminders) { memory ->
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

            // Today's Reminders Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Today, contentDescription = "Today", tint = Color(0xFFFFA726))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LanguageUtils.getString("todays_reminders", language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFA726)
                    )
                }
            }

            if (todayReminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "No reminders scheduled for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            } else {
                items(todayReminders) { memory ->
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

            // Upcoming Reminders Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = "Upcoming", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "No upcoming reminders scheduled.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            } else {
                items(upcomingReminders) { memory ->
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
