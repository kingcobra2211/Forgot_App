package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemoryWithDetails
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryCard(
    memoryWithDetails: MemoryWithDetails,
    language: String,
    onEdit: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onFavoriteToggle: (Boolean) -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    onUpdateChecklist: (String) -> Unit = {},
    onUpdatePaidStatus: (Boolean) -> Unit = {}
) {
    val memory = memoryWithDetails.memory
    val categoryItem = CategoryRegistry.getCategoryItem(memory.category)
    val accentColor = categoryItem.color
    
    // Parse checklist
    val checklistItems = remember(memoryWithDetails.shoppingDetail?.shoppingItems) {
        val list = mutableListOf<Pair<String, Boolean>>()
        try {
            val shoppingItems = memoryWithDetails.shoppingDetail?.shoppingItems
            if (!shoppingItems.isNullOrEmpty()) {
                shoppingItems.split("\n").forEach { line ->
                    val parts = line.split("|")
                    if (parts.isNotEmpty()) {
                        val text = parts[0]
                        val checked = parts.getOrNull(1)?.toBoolean() ?: false
                        if (text.isNotEmpty()) {
                            list.add(Pair(text, checked))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    // Modern Material 3 Outlined Card with subtle tonal tint
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("memory_card_${memory.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge + Title and Labels + Quick Status Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large stylish Category Icon badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryItem.icon,
                        contentDescription = memory.category,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Category Label
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = memory.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (memory.status == "Completed") TextDecoration.LineThrough else TextDecoration.None,
                            color = if (memory.status == "Completed") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = LanguageUtils.getString(memory.category, language).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            letterSpacing = 0.5.sp
                        )
                        
                        // Priority Badge
                        if (memory.priority != "Medium" || true) {
                            val priorityColor = when (memory.priority) {
                                "High" -> Color(0xFFEF5350)
                                "Low" -> Color(0xFF66BB6A)
                                else -> Color(0xFFFFA726)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(priorityColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = LanguageUtils.getString("priority_${memory.priority.lowercase()}", language).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = priorityColor,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Interactive Pin & Favorite actions directly accessible with 48dp target standard spacing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onPinToggle(!memory.isPinned) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin memory",
                            tint = if (memory.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onFavoriteToggle(!memory.isFavorite) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite memory",
                            tint = if (memory.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Description Subtitle with clean typography
            if (memory.description.isNotEmpty()) {
                Text(
                    text = memory.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
                )
            }

            // Category Specific Beautiful UI Subsections
            when (memory.category.lowercase()) {
                "money" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            if (!memoryWithDetails.person.isNullOrEmpty()) {
                                Text(
                                    text = "${LanguageUtils.getString("money_person", language)}: ${memoryWithDetails.person}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "₹${memoryWithDetails.amount ?: 0.0}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (memoryWithDetails.isPaid) MaterialTheme.colorScheme.primary else Color(0xFFE53935)
                            )
                        }

                        // Payment Status Toggle Pill
                        val statusText = if (memoryWithDetails.isPaid) "money_paid" else "money_pending"
                        val chipBg = if (memoryWithDetails.isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFFEF5350).copy(alpha = 0.15f)
                        val chipTextCol = if (memoryWithDetails.isPaid) MaterialTheme.colorScheme.primary else Color(0xFFE53935)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable { onUpdatePaidStatus(!memoryWithDetails.isPaid) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = LanguageUtils.getString(statusText, language),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = chipTextCol
                            )
                        }
                    }
                }

                "parking" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!memoryWithDetails.parkingFloor.isNullOrEmpty()) {
                            Column {
                                Text(
                                    text = LanguageUtils.getString("parking_floor", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = memoryWithDetails.parkingFloor!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!memoryWithDetails.parkingSlot.isNullOrEmpty()) {
                            Column {
                                Text(
                                    text = LanguageUtils.getString("parking_slot", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = memoryWithDetails.parkingSlot!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!memoryWithDetails.location.isNullOrEmpty()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Location Detail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "GPS Location",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = memoryWithDetails.location!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                "document" -> {
                    if (memoryWithDetails.documentExpiry != null) {
                        val isExpired = memoryWithDetails.documentExpiry!! < System.currentTimeMillis()
                        val expiryDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(memoryWithDetails.documentExpiry!!))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isExpired) Color(0xFFEF5350).copy(alpha = 0.1f) 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isExpired) Icons.Default.Warning else Icons.Default.EventNote,
                                contentDescription = "Expiry date",
                                tint = if (isExpired) Color(0xFFEF5350) else accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${LanguageUtils.getString("doc_expiry", language)}: $expiryDateStr",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpired) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                "shopping" -> {
                    if (checklistItems.isNotEmpty()) {
                        val completedCount = checklistItems.count { it.second }
                        val totalCount = checklistItems.size
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Shopping List ($completedCount/$totalCount)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            checklistItems.take(4).forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val updatedList = checklistItems.toMutableList()
                                            updatedList[index] = item.copy(second = !item.second)
                                            val builder = StringBuilder()
                                            updatedList.forEach { i ->
                                                builder.append("${i.first}|${i.second}\n")
                                            }
                                            onUpdateChecklist(builder.toString().trim())
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.second) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "Check item",
                                        tint = if (item.second) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.first,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (item.second) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (item.second) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            if (totalCount > 4) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+ ${totalCount - 4} more items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 26.dp)
                                )
                            }
                        }
                    }
                }

                "medicine" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Daily Doses:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (memoryWithDetails.medicineDoseMorning) Icons.Default.LightMode else Icons.Outlined.LightMode,
                                contentDescription = "Morning Dose",
                                tint = if (memoryWithDetails.medicineDoseMorning) Color(0xFFFFA726) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Morning",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (memoryWithDetails.medicineDoseMorning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontWeight = if (memoryWithDetails.medicineDoseMorning) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (memoryWithDetails.medicineDoseAfternoon) Icons.Default.WbSunny else Icons.Outlined.WbSunny,
                                contentDescription = "Afternoon Dose",
                                tint = if (memoryWithDetails.medicineDoseAfternoon) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Noon",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (memoryWithDetails.medicineDoseAfternoon) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontWeight = if (memoryWithDetails.medicineDoseAfternoon) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (memoryWithDetails.medicineDoseNight) Icons.Default.NightlightRound else Icons.Outlined.NightlightRound,
                                contentDescription = "Night Dose",
                                tint = if (memoryWithDetails.medicineDoseNight) Color(0xFF5C6BC0) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Night",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (memoryWithDetails.medicineDoseNight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontWeight = if (memoryWithDetails.medicineDoseNight) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                "wishlist" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (memoryWithDetails.price != null) {
                            Text(
                                text = "Estimated Price: ₹${memoryWithDetails.price}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        if (!memoryWithDetails.urlLink.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Web Link",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Buy Link",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                "gift idea" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            if (!memoryWithDetails.person.isNullOrEmpty()) {
                                Text(
                                    text = "Recipient: ${memoryWithDetails.person}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (memoryWithDetails.amount != null) {
                                Text(
                                    text = "Budget: ₹${memoryWithDetails.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                        if (!memoryWithDetails.urlLink.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = "Gift Link",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ideas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Section: Date / Reminders and Toolbar Actions with 48dp Touch Targets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left aligned: Last updated / created date OR Reminder Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    if (memory.reminderDate != null) {
                        val reminderStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(memory.reminderDate))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Active Reminder Scheduled",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reminderStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        val updatedStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(memory.updatedDate))
                        Text(
                            text = "Saved $updatedStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                // Right aligned action buttons for Archive, Edit, Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Archive Toggle Button
                    IconButton(
                        onClick = onArchiveToggle,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.status == "Archived") Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (memory.status == "Archived") "Unarchive memory" else "Archive memory",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Edit Button (Only if not in Trash)
                    if (memory.status != "Trash") {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit memory details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.status == "Trash") Icons.Default.DeleteForever else Icons.Default.Delete,
                            contentDescription = if (memory.status == "Trash") "Delete memory permanently" else "Move memory to trash",
                            tint = if (memory.status == "Trash") Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
