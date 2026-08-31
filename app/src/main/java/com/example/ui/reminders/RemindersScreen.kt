package com.example.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import java.text.SimpleDateFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
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

    val haptic = LocalHapticFeedback.current
    var isCalendarView by remember { mutableStateOf(false) }
    var selectedCalDay by remember { mutableStateOf(Calendar.getInstance()) }

    val selectedDayMemories = remember(activeReminders, selectedCalDay) {
        val cal = Calendar.getInstance()
        activeReminders.filter { item ->
            val remDate = item.memory.reminderDate
            if (remDate != null) {
                cal.timeInMillis = remDate
                cal.get(Calendar.YEAR) == selectedCalDay.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == selectedCalDay.get(Calendar.DAY_OF_YEAR)
            } else false
        }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageUtils.getString("reminders_tab", language),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )

                    // View Mode Toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(4.dp)
                    ) {
                        FilterChip(
                            selected = !isCalendarView,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isCalendarView = false
                            },
                            label = { Text("List") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "List View", modifier = Modifier.size(16.dp)) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = isCalendarView,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isCalendarView = true
                            },
                            label = { Text("Calendar") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar View", modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
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
                        Image(
                            painter = painterResource(id = R.drawable.img_onboarding_hero),
                            contentDescription = "No reminders active artwork",
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
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
        } else if (isCalendarView) {
            // CALENDAR VIEW MODE
            val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
            val currentMonthStr = remember(selectedCalDay) { monthFormat.format(selectedCalDay.time) }
            val todayCal = remember { Calendar.getInstance() }

            val (daysInMonth, firstDayOfWeek) = remember(selectedCalDay) {
                val cal = selectedCalDay.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val startDay = cal.get(Calendar.DAY_OF_WEEK)
                Pair(maxDays, startDay)
            }

            val reminderDaysInMonth = remember(activeReminders, selectedCalDay) {
                val monthCal = selectedCalDay.clone() as Calendar
                val targetMonth = monthCal.get(Calendar.MONTH)
                val targetYear = monthCal.get(Calendar.YEAR)
                val set = mutableSetOf<Int>()
                val remCal = Calendar.getInstance()
                activeReminders.forEach { item ->
                    item.memory.reminderDate?.let { dateMs ->
                        remCal.timeInMillis = dateMs
                        if (remCal.get(Calendar.MONTH) == targetMonth && remCal.get(Calendar.YEAR) == targetYear) {
                            set.add(remCal.get(Calendar.DAY_OF_MONTH))
                        }
                    }
                }
                set
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = metrics.horizontalPadding, end = metrics.horizontalPadding, top = 8.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)),
                        shape = RoundedCornerShape(metrics.cardCornerRadius)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentMonthStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(onClick = {
                                        val cal = selectedCalDay.clone() as Calendar
                                        cal.add(Calendar.MONTH, -1)
                                        selectedCalDay = cal
                                    }) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev month")
                                    }
                                    IconButton(onClick = {
                                        val cal = selectedCalDay.clone() as Calendar
                                        cal.add(Calendar.MONTH, 1)
                                        selectedCalDay = cal
                                    }) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                                    }
                                }
                            }

                            // Day of week headers
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(38.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Calendar Dates Grid (7 columns)
                            val totalSlots = (firstDayOfWeek - 1) + daysInMonth
                            val totalRows = (totalSlots + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (row in 0 until totalRows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        for (col in 0 until 7) {
                                            val dayNum = row * 7 + col - (firstDayOfWeek - 2)
                                            if (dayNum in 1..daysInMonth) {
                                                val isSelectedDay = selectedCalDay.get(Calendar.DAY_OF_MONTH) == dayNum
                                                val hasReminder = reminderDaysInMonth.contains(dayNum)
                                                val isToday = (todayCal.get(Calendar.DAY_OF_MONTH) == dayNum &&
                                                               todayCal.get(Calendar.MONTH) == selectedCalDay.get(Calendar.MONTH) &&
                                                               todayCal.get(Calendar.YEAR) == selectedCalDay.get(Calendar.YEAR))

                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelectedDay -> MaterialTheme.colorScheme.primary
                                                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable {
                                                            val newCal = selectedCalDay.clone() as Calendar
                                                            newCal.set(Calendar.DAY_OF_MONTH, dayNum)
                                                            selectedCalDay = newCal
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = "$dayNum",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelectedDay || isToday) FontWeight.Bold else FontWeight.Normal,
                                                            color = when {
                                                                isSelectedDay -> Color.White
                                                                isToday -> MaterialTheme.colorScheme.primary
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )
                                                        if (hasReminder) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelectedDay) Color.White else MaterialTheme.colorScheme.primary)
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.size(38.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Reminders on ${SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(selectedCalDay.time)} (${selectedDayMemories.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedDayMemories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No reminders for selected day.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(selectedDayMemories) { memoryWithDetails ->
                        val memory = memoryWithDetails.memory
                        MemoryCard(
                            memoryWithDetails = memoryWithDetails,
                            language = language,
                            onEdit = { onNavigateToRemember(memory.id, null) },
                            onPinToggle = { pinned -> viewModel.pinMemory(memory, pinned) },
                            onFavoriteToggle = { fav -> viewModel.favoriteMemory(memory, fav) },
                            onArchiveToggle = { viewModel.archiveMemory(memory) },
                            onDelete = { viewModel.moveMemoryToTrash(memory) }
                        )
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
                    start = metrics.horizontalPadding,
                    end = metrics.horizontalPadding,
                    top = metrics.verticalPadding,
                    bottom = 0.dp
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
                            },
                            onSnoozeReminder = {
                                viewModel.updateMemory(memory.copy(reminderDate = (memory.reminderDate ?: System.currentTimeMillis()) + 24 * 60 * 60 * 1000L))
                            },
                            onClearReminder = {
                                viewModel.updateMemory(memory.copy(reminderDate = null))
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
                            },
                            onSnoozeReminder = {
                                viewModel.updateMemory(memory.copy(reminderDate = (memory.reminderDate ?: System.currentTimeMillis()) + 24 * 60 * 60 * 1000L))
                            },
                            onClearReminder = {
                                viewModel.updateMemory(memory.copy(reminderDate = null))
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
                            },
                            onSnoozeReminder = {
                                viewModel.updateMemory(memory.copy(reminderDate = (memory.reminderDate ?: System.currentTimeMillis()) + 24 * 60 * 60 * 1000L))
                            },
                            onClearReminder = {
                                viewModel.updateMemory(memory.copy(reminderDate = null))
                            }
                        )
                    }
                }
            }
        }
    }
}
