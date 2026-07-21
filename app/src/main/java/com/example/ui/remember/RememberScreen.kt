package com.example.ui.remember

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Memory
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.viewmodel.MemoryViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RememberScreen(
    viewModel: MemoryViewModel,
    memoryId: Int?,
    initialCategory: String?,
    onSaveComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    
    // Determine if we are editing or adding
    val isEditing = memoryId != null && memoryId != 0
    val existingMemory = remember(memoryId) {
        if (isEditing) {
            viewModel.activeMemories.value.firstOrNull { it.id == memoryId }
                ?: viewModel.archivedMemories.value.firstOrNull { it.id == memoryId }
        } else null
    }

    // Form states
    var title by remember { mutableStateOf(existingMemory?.title ?: "") }
    var description by remember { mutableStateOf(existingMemory?.description ?: "") }
    var category by remember { mutableStateOf(existingMemory?.category ?: initialCategory ?: "Note") }
    var priority by remember { mutableStateOf(existingMemory?.priority ?: "Medium") }
    var reminderTimestamp by remember { mutableStateOf(existingMemory?.reminderDate) }
    var isPinned by remember { mutableStateOf(existingMemory?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(existingMemory?.isFavorite ?: false) }
    
    // Category specific states
    var amount by remember { mutableStateOf(existingMemory?.amount?.toString() ?: "") }
    var person by remember { mutableStateOf(existingMemory?.person ?: "") }
    var isPaid by remember { mutableStateOf(existingMemory?.isPaid ?: false) }
    var parkingFloor by remember { mutableStateOf(existingMemory?.parkingFloor ?: "") }
    var parkingSlot by remember { mutableStateOf(existingMemory?.parkingSlot ?: "") }
    var locationText by remember { mutableStateOf(existingMemory?.location ?: "") }
    var docExpiryTimestamp by remember { mutableStateOf(existingMemory?.documentExpiry) }
    var medicineDoseMorning by remember { mutableStateOf(existingMemory?.medicineDoseMorning ?: false) }
    var medicineDoseAfternoon by remember { mutableStateOf(existingMemory?.medicineDoseAfternoon ?: false) }
    var medicineDoseNight by remember { mutableStateOf(existingMemory?.medicineDoseNight ?: false) }
    var priceText by remember { mutableStateOf(existingMemory?.price?.toString() ?: "") }
    var urlLinkText by remember { mutableStateOf(existingMemory?.urlLink ?: "") }

    // Shopping checklist list of items
    val checklistItems = remember { mutableStateListOf<Pair<String, Boolean>>() }
    
    LaunchedEffect(existingMemory) {
        if (existingMemory != null) {
            checklistItems.clear()
            try {
                if (!existingMemory.checklistJson.isNullOrEmpty()) {
                    val arr = JSONArray(existingMemory.checklistJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        checklistItems.add(Pair(obj.getString("text"), obj.getBoolean("checked")))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Apply quick-add helpers / templates based on category selected
            if (title.isEmpty()) {
                title = when (category.lowercase()) {
                    "parking" -> "Car Parking Location"
                    "medicine" -> "Daily Medicine Reminders"
                    "shopping" -> "Shopping Items"
                    else -> ""
                }
            }
        }
    }

    var newChecklistItemText by remember { mutableStateOf("") }

    // Dropdowns / dialog helpers
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Memory" else LanguageUtils.getString("add_memory_title", language),
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Quick Selector Row
            Text(
                text = LanguageUtils.getString("category_label", language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryRegistry.categories.take(5).forEach { catItem ->
                    val selected = category.lowercase() == catItem.name.lowercase()
                    val bg = if (selected) catItem.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val tc = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable {
                                category = catItem.name
                                // Auto templates
                                if (title.isEmpty() || title == "Car Parking Location" || title == "Daily Medicine Reminders" || title == "Shopping Items") {
                                    title = when (catItem.name.lowercase()) {
                                        "parking" -> "Car Parking Location"
                                        "medicine" -> "Daily Medicine Reminders"
                                        "shopping" -> "Shopping Items"
                                        else -> ""
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = catItem.icon,
                                contentDescription = catItem.name,
                                tint = tc,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = LanguageUtils.getString(catItem.name, language),
                                style = MaterialTheme.typography.bodySmall,
                                color = tc,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryRegistry.categories.drop(5).forEach { catItem ->
                    val selected = category.lowercase() == catItem.name.lowercase()
                    val bg = if (selected) catItem.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val tc = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable {
                                category = catItem.name
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = catItem.icon,
                                contentDescription = catItem.name,
                                tint = tc,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = LanguageUtils.getString(catItem.name, language),
                                style = MaterialTheme.typography.bodySmall,
                                color = tc,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(LanguageUtils.getString("title_label", language)) },
                placeholder = { Text("What do you want to remember?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("remember_title_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CategoryRegistry.getCategoryItem(category).color,
                    focusedLabelColor = CategoryRegistry.getCategoryItem(category).color
                )
            )

            // Description input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(LanguageUtils.getString("description_label", language)) },
                placeholder = { Text("Add descriptive notes or instructions here...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CategoryRegistry.getCategoryItem(category).color,
                    focusedLabelColor = CategoryRegistry.getCategoryItem(category).color
                )
            )

            // DYNAMIC CATEGORY FIELDS
            AnimatedVisibility(visible = category.lowercase() == "money") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Money Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CategoryRegistry.getCategoryItem("Money").color
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = person,
                            onValueChange = { person = it },
                            label = { Text(LanguageUtils.getString("money_person", language)) },
                            placeholder = { Text("Rahul, Friend...") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text(LanguageUtils.getString("money_amount", language)) },
                            placeholder = { Text("500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    // Paid/Pending toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = LanguageUtils.getString("money_status", language),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row {
                            Button(
                                onClick = { isPaid = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isPaid) Color(0xFFEF5350) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                            ) {
                                Text(
                                    LanguageUtils.getString("money_pending", language),
                                    color = if (!isPaid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { isPaid = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPaid) Color(0xFF66BB6A) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            ) {
                                Text(
                                    LanguageUtils.getString("money_paid", language),
                                    color = if (isPaid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = category.lowercase() == "parking") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Parking Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CategoryRegistry.getCategoryItem("Parking").color
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = parkingFloor,
                            onValueChange = { parkingFloor = it },
                            label = { Text(LanguageUtils.getString("parking_floor", language)) },
                            placeholder = { Text("B2, 3rd Floor...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = parkingSlot,
                            onValueChange = { parkingSlot = it },
                            label = { Text(LanguageUtils.getString("parking_slot", language)) },
                            placeholder = { Text("A43, 102...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = { Text(LanguageUtils.getString("parking_gps", language)) },
                        placeholder = { Text("Pillar 45, Near lift lobby...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            AnimatedVisibility(visible = category.lowercase() == "document") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Document Expiry Date",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CategoryRegistry.getCategoryItem("Document").color
                    )
                    
                    val expiryStr = if (docExpiryTimestamp != null) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(docExpiryTimestamp!!))
                    } else "No Expiry Selected"

                    Card(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.YEAR, year)
                                    cal.set(Calendar.MONTH, month)
                                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    docExpiryTimestamp = cal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Event, contentDescription = "Expiry")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = expiryStr, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "Choose Date", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = category.lowercase() == "shopping") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Checklist Items",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CategoryRegistry.getCategoryItem("Shopping").color
                    )

                    // Add item bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newChecklistItemText,
                            onValueChange = { newChecklistItemText = it },
                            placeholder = { Text("Add grocery/list item...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newChecklistItemText.trim().isNotEmpty()) {
                                    checklistItems.add(Pair(newChecklistItemText.trim(), false))
                                    newChecklistItemText = ""
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                    // Render checklist items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (checklistItems.isEmpty()) {
                            Text(
                                text = "No items added yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            checklistItems.forEachIndexed { index, pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = pair.second,
                                            onCheckedChange = { checked ->
                                                checklistItems[index] = pair.copy(second = checked)
                                            }
                                        )
                                        Text(text = pair.first, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(
                                        onClick = { checklistItems.removeAt(index) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFEF5350)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = category.lowercase() == "medicine") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Medicine Dosage Schedule",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CategoryRegistry.getCategoryItem("Medicine").color
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Morning ☀️", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Checkbox(checked = medicineDoseMorning, onCheckedChange = { medicineDoseMorning = it })
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Afternoon 🌤️", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Checkbox(checked = medicineDoseAfternoon, onCheckedChange = { medicineDoseAfternoon = it })
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Night 🌙", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Checkbox(checked = medicineDoseNight, onCheckedChange = { medicineDoseNight = it })
                        }
                    }
                }
            }

            AnimatedVisibility(visible = category.lowercase() == "wishlist" || category.lowercase() == "gift idea") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Price & Links",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CategoryRegistry.getCategoryItem(category).color
                    )
                    
                    if (category.lowercase() == "gift idea") {
                        OutlinedTextField(
                            value = person,
                            onValueChange = { person = it },
                            label = { Text("For Whom (Occasion)") },
                            placeholder = { Text("Mom, Friend, Birthday...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = if (category.lowercase() == "wishlist") priceText else amount,
                            onValueChange = {
                                if (category.lowercase() == "wishlist") priceText = it else amount = it
                            },
                            label = { Text(if (category.lowercase() == "wishlist") "Estimated Price" else "Budget") },
                            placeholder = { Text("1500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = urlLinkText,
                            onValueChange = { urlLinkText = it },
                            label = { Text("Product Web Link") },
                            placeholder = { Text("https://amazon.com...") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Reminders Setup
            Text(
                text = LanguageUtils.getString("reminder_label", language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            val reminderStr = if (reminderTimestamp != null) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(reminderTimestamp!!))
            } else "No Reminder Configured"

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val timeCalendar = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val finalCalendar = Calendar.getInstance()
                                        finalCalendar.set(Calendar.YEAR, year)
                                        finalCalendar.set(Calendar.MONTH, month)
                                        finalCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        finalCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        finalCalendar.set(Calendar.MINUTE, minute)
                                        finalCalendar.set(Calendar.SECOND, 0)
                                        reminderTimestamp = finalCalendar.timeInMillis
                                    },
                                    timeCalendar.get(Calendar.HOUR_OF_DAY),
                                    timeCalendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Reminder")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = reminderStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (reminderTimestamp != null) {
                    IconButton(
                        onClick = { reminderTimestamp = null },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.15f))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear reminder", tint = Color(0xFFEF5350))
                    }
                }
            }

            // Priority Selector
            Text(
                text = LanguageUtils.getString("priority_label", language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Low", "Medium", "High").forEach { level ->
                    val selected = priority == level
                    val color = when (level) {
                        "High" -> Color(0xFFEF5350)
                        "Medium" -> Color(0xFFFFA726)
                        else -> Color(0xFF66BB6A)
                    }
                    val bg = if (selected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    val tc = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { priority = level }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = LanguageUtils.getString("priority_${level.lowercase()}", language),
                            fontWeight = FontWeight.Bold,
                            color = tc,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Save/Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(LanguageUtils.getString("cancel_button", language))
                }

                Button(
                    onClick = {
                        if (title.trim().isEmpty()) {
                            // Suggest generic title if empty
                            title = "Remember $category"
                        }

                        // Serialize Shopping checklist
                        val checklistJson = if (category.lowercase() == "shopping") {
                            val arr = JSONArray()
                            for (pair in checklistItems) {
                                val obj = JSONObject()
                                obj.put("text", pair.first)
                                obj.put("checked", pair.second)
                                arr.put(obj)
                            }
                            arr.toString()
                        } else null

                        val memory = Memory(
                            id = existingMemory?.id ?: 0,
                            title = title.trim(),
                            description = description.trim(),
                            category = category,
                            createdDate = existingMemory?.createdDate ?: System.currentTimeMillis(),
                            updatedDate = System.currentTimeMillis(),
                            reminderDate = reminderTimestamp,
                            priority = priority,
                            status = existingMemory?.status ?: "Active",
                            location = locationText.trim().ifEmpty { null },
                            latitude = existingMemory?.latitude,
                            longitude = existingMemory?.longitude,
                            photoPath = existingMemory?.photoPath,
                            voicePath = existingMemory?.voicePath,
                            isPinned = isPinned,
                            isFavorite = isFavorite,
                            trashDate = existingMemory?.trashDate,
                            
                            // Category-specific properties
                            amount = amount.trim().toDoubleOrNull(),
                            isPaid = isPaid,
                            person = person.trim().ifEmpty { null },
                            parkingFloor = parkingFloor.trim().ifEmpty { null },
                            parkingSlot = parkingSlot.trim().ifEmpty { null },
                            documentExpiry = docExpiryTimestamp,
                            medicineDoseMorning = medicineDoseMorning,
                            medicineDoseAfternoon = medicineDoseAfternoon,
                            medicineDoseNight = medicineDoseNight,
                            checklistJson = checklistJson,
                            urlLink = urlLinkText.trim().ifEmpty { null },
                            price = priceText.trim().toDoubleOrNull()
                        )

                        if (isEditing) {
                            viewModel.updateMemory(memory)
                        } else {
                            viewModel.saveMemory(memory)
                        }
                        onSaveComplete()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("save_memory_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CategoryRegistry.getCategoryItem(category).color
                    )
                ) {
                    Text(
                        LanguageUtils.getString("save_button", language),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
