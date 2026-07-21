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
import com.example.data.model.Memory
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryCard(
    memory: Memory,
    language: String,
    onEdit: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onFavoriteToggle: (Boolean) -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    onUpdateChecklist: (String) -> Unit = {},
    onUpdatePaidStatus: (Boolean) -> Unit = {}
) {
    val categoryItem = CategoryRegistry.getCategoryItem(memory.category)
    val cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val accentColor = categoryItem.color
    
    // Parse checklist
    val checklistItems = remember(memory.checklistJson) {
        val list = mutableListOf<Pair<String, Boolean>>()
        try {
            if (!memory.checklistJson.isNullOrEmpty()) {
                val arr = JSONArray(memory.checklistJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(Pair(obj.getString("text"), obj.getBoolean("checked")))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("memory_card_${memory.id}"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Category Icon + Title + Actions (Pin, Favorite, Menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryItem.icon,
                        contentDescription = memory.category,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Category Name
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = memory.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (memory.status == "Completed") TextDecoration.LineThrough else TextDecoration.None,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (memory.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = LanguageUtils.getString(memory.category, language),
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Header Actions
                IconButton(
                    onClick = { onPinToggle(!memory.isPinned) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (memory.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (memory.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onFavoriteToggle(!memory.isFavorite) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (memory.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (memory.isFavorite) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Description
            if (memory.description.isNotEmpty()) {
                Text(
                    text = memory.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Category Specific UI elements
            when (memory.category.lowercase()) {
                "money" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            if (!memory.person.isNullOrEmpty()) {
                                Text(
                                    text = "${LanguageUtils.getString("money_person", language)}: ${memory.person}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "₹${memory.amount ?: 0.0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (memory.isPaid) MaterialTheme.colorScheme.primary else Color(0xFFEF5350)
                            )
                        }

                        // Payment Status Action Chip
                        val statusText = if (memory.isPaid) "money_paid" else "money_pending"
                        val chipBg = if (memory.isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFFEF5350).copy(alpha = 0.15f)
                        val chipTextCol = if (memory.isPaid) MaterialTheme.colorScheme.primary else Color(0xFFEF5350)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(chipBg)
                                .clickable {
                                    onUpdatePaidStatus(!memory.isPaid)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = LanguageUtils.getString(statusText, language),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!memory.parkingFloor.isNullOrEmpty()) {
                            Column {
                                Text(
                                    text = LanguageUtils.getString("parking_floor", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = memory.parkingFloor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!memory.parkingSlot.isNullOrEmpty()) {
                            Column {
                                Text(
                                    text = LanguageUtils.getString("parking_slot", language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = memory.parkingSlot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!memory.location.isNullOrEmpty()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Location",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "GPS",
                                        tint = accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = memory.location,
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
                    if (memory.documentExpiry != null) {
                        val isExpired = memory.documentExpiry < System.currentTimeMillis()
                        val expiryDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(memory.documentExpiry))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isExpired) Color(0xFFEF5350).copy(alpha = 0.1f) 
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isExpired) Icons.Default.Warning else Icons.Default.Event,
                                contentDescription = "Expiry",
                                tint = if (isExpired) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${LanguageUtils.getString("doc_expiry", language)}: $expiryDateStr",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpired) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Shopping Checklist ($completedCount/$totalCount)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Let's render max 3 items in preview, click check toggle updates checklist directly
                            checklistItems.take(4).forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val updatedList = checklistItems.toMutableList()
                                            updatedList[index] = item.copy(second = !item.second)
                                            // Save checklist back to JSON
                                            val jsonArray = JSONArray()
                                            for (i in updatedList) {
                                                val obj = JSONObject()
                                                obj.put("text", i.first)
                                                obj.put("checked", i.second)
                                                jsonArray.put(obj)
                                            }
                                            onUpdateChecklist(jsonArray.toString())
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.second) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "Check",
                                        tint = if (item.second) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.first,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (item.second) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (item.second) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            if (totalCount > 4) {
                                Text(
                                    text = "+ ${totalCount - 4} more items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 22.dp, top = 2.dp)
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Doses:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (memory.medicineDoseMorning) Icons.Default.LightMode else Icons.Outlined.LightMode,
                                contentDescription = "Morning",
                                tint = if (memory.medicineDoseMorning) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = LanguageUtils.getString("priority_low", language), // simple representation
                                style = MaterialTheme.typography.bodySmall,
                                color = if (memory.medicineDoseMorning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (memory.medicineDoseAfternoon) Icons.Default.WbSunny else Icons.Outlined.WbSunny,
                                contentDescription = "Afternoon",
                                tint = if (memory.medicineDoseAfternoon) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Mid",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (memory.medicineDoseAfternoon) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (memory.medicineDoseNight) Icons.Default.NightlightRound else Icons.Outlined.NightlightRound,
                                contentDescription = "Night",
                                tint = if (memory.medicineDoseNight) Color(0xFF5C6BC0) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Night",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (memory.medicineDoseNight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                "wishlist" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (memory.price != null) {
                            Text(
                                text = "Price: ₹${memory.price}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        if (!memory.urlLink.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Link",
                                    tint = accentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Link",
                                    style = MaterialTheme.typography.bodySmall,
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            if (!memory.person.isNullOrEmpty()) {
                                Text(
                                    text = "For: ${memory.person}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (memory.amount != null) {
                                Text(
                                    text = "Budget: ₹${memory.amount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                        if (!memory.urlLink.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = "Link",
                                    tint = accentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ideas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Date / Time + Main Actions (Edit, Archive, Trash)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Reminder Indicator or Creation Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (memory.reminderDate != null) {
                        val reminderStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(memory.reminderDate))
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminder Active",
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reminderStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFA726),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val createdStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(memory.createdDate))
                        Text(
                            text = createdStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                // Actions: Edit, Archive, Trash
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Archive Toggle Button
                    IconButton(
                        onClick = onArchiveToggle,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.status == "Archived") Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Edit Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.status == "Trash") Icons.Default.DeleteForever else Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (memory.status == "Trash") Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
