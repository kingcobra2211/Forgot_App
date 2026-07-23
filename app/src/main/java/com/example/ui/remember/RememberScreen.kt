package com.example.ui.remember

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.CategoryItem
import com.example.ui.utils.LanguageUtils
import com.example.ui.utils.LocalResponsiveMetrics
import com.example.ui.viewmodel.MemoryViewModel
import java.io.File
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
    val metrics = LocalResponsiveMetrics.current
    
    // Determine if we are editing or adding
    val isEditing = memoryId != null && memoryId != 0
    val existingMemoryWithDetails = remember(memoryId) {
        if (isEditing) {
            viewModel.activeMemories.value.firstOrNull { it.memory.id == memoryId }
                ?: viewModel.archivedMemories.value.firstOrNull { it.memory.id == memoryId }
        } else null
    }
    val existingMemory = existingMemoryWithDetails?.memory

    // Base form states
    var category by remember { mutableStateOf(existingMemory?.category ?: initialCategory ?: "Note") }
    var title by remember { mutableStateOf(existingMemory?.title ?: "") }
    var description by remember { mutableStateOf(existingMemory?.description ?: "") }
    var priority by remember { mutableStateOf(existingMemory?.priority ?: "Medium") }
    var reminderTimestamp by remember { mutableStateOf(existingMemory?.reminderDate) }
    var isPinned by remember { mutableStateOf(existingMemory?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(existingMemory?.isFavorite ?: false) }
    
    // Category specific detail states (loaded from relational delegates)
    var amount by remember { mutableStateOf(existingMemoryWithDetails?.amount?.toString() ?: "") }
    var person by remember { mutableStateOf(existingMemoryWithDetails?.person ?: "") }
    var isPaid by remember { mutableStateOf(existingMemoryWithDetails?.isPaid ?: false) }
    
    // Parking states
    var parkingFloor by remember { mutableStateOf(existingMemoryWithDetails?.parkingFloor ?: "") }
    var parkingSlot by remember { mutableStateOf(existingMemoryWithDetails?.parkingSlot ?: "") }
    var locationText by remember { mutableStateOf(existingMemoryWithDetails?.location ?: "") }
    var vehicleName by remember { mutableStateOf(existingMemoryWithDetails?.parkingDetail?.vehicleName ?: "") }

    // Document states
    var docType by remember { mutableStateOf(existingMemoryWithDetails?.documentDetail?.documentType ?: "Passport") }
    var docNumber by remember { mutableStateOf(existingMemoryWithDetails?.documentDetail?.documentNumber ?: "") }
    var docIssuedBy by remember { mutableStateOf(existingMemoryWithDetails?.documentDetail?.issuedBy ?: "") }
    var docExpiryTimestamp by remember { mutableStateOf(existingMemoryWithDetails?.documentExpiry) }

    // Shopping states
    var shoppingStore by remember { mutableStateOf(existingMemoryWithDetails?.shoppingDetail?.store ?: "") }
    var shoppingBudget by remember { mutableStateOf(existingMemoryWithDetails?.shoppingDetail?.budget?.toString() ?: "") }
    val checklistItems = remember { mutableStateListOf<Pair<String, Boolean>>() }

    // Medicine states
    var medicineDoseMorning by remember { mutableStateOf(existingMemoryWithDetails?.medicineDoseMorning ?: false) }
    var medicineDoseAfternoon by remember { mutableStateOf(existingMemoryWithDetails?.medicineDoseAfternoon ?: false) }
    var medicineDoseNight by remember { mutableStateOf(existingMemoryWithDetails?.medicineDoseNight ?: false) }
    var doctorName by remember { mutableStateOf(existingMemoryWithDetails?.medicineDetail?.doctorName ?: "") }
    var dosage by remember { mutableStateOf(existingMemoryWithDetails?.medicineDetail?.dosage ?: "") }

    // Place states
    var contactPerson by remember { mutableStateOf(existingMemoryWithDetails?.placeDetail?.contactPerson ?: "") }
    var urlLinkText by remember { mutableStateOf(existingMemoryWithDetails?.urlLink ?: "") }

    // Gift states
    var giftOccasion by remember { mutableStateOf(existingMemoryWithDetails?.giftDetail?.occasion ?: "") }

    // Wishlist states
    var wishProduct by remember { mutableStateOf(existingMemoryWithDetails?.wishlistDetail?.productName ?: "") }
    var priceText by remember { mutableStateOf(existingMemoryWithDetails?.price?.toString() ?: "") }

    // Attachments states (Camera, Gallery, Audio, File Paths)
    var photoPathState by remember {
        mutableStateOf<String?>(
            existingMemoryWithDetails?.parkingDetail?.photoPath
                ?: existingMemoryWithDetails?.moneyDetail?.receiptPhotoPath
                ?: existingMemoryWithDetails?.documentDetail?.photoPath
                ?: existingMemoryWithDetails?.medicineDetail?.prescriptionPhotoPath
                ?: existingMemoryWithDetails?.placeDetail?.photoPath
                ?: existingMemoryWithDetails?.wishlistDetail?.photoPath
                ?: existingMemory?.photoPath
        )
    }
    val attachmentPaths = remember {
        mutableStateListOf<String>().apply {
            existingMemory?.attachmentPaths
                ?.split("\n")
                ?.filter { it.isNotBlank() }
                ?.forEach(::add)
            if (isEmpty() && photoPathState != null) add(photoPathState!!)
        }
    }
    
    var voiceFilePath by remember { mutableStateOf<String?>(existingMemory?.voicePath) }
    
    // Expandable Additional Details
    var showAdditionalDetails by remember { mutableStateOf(false) }

    // Load Shopping checklist if editing
    LaunchedEffect(existingMemoryWithDetails) {
        if (existingMemoryWithDetails != null) {
            checklistItems.clear()
            val shoppingDetail = existingMemoryWithDetails.shoppingDetail
            if (shoppingDetail != null && !shoppingDetail.shoppingItems.isNullOrEmpty()) {
                shoppingDetail.shoppingItems.split("\n").forEach { line ->
                    val parts = line.split("|")
                    if (parts.isNotEmpty()) {
                        val text = parts[0]
                        val checked = parts.getOrNull(1)?.toBoolean() ?: false
                        if (text.isNotEmpty()) {
                            checklistItems.add(Pair(text, checked))
                        }
                    }
                }
            }
        } else {
            // Quick titles templates for clean start
            updateDefaultTitle(category) { title = it }
        }
    }

    // Update dynamic defaults on category changes
    LaunchedEffect(category) {
        if (!isEditing) {
            updateDefaultTitle(category) { title = it }
        }
    }

    var newChecklistItemText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val activeCategoryColor = CategoryRegistry.getCategoryItem(category).color

    // Recording and Media Player State
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var pendingRecordingPath by remember { mutableStateOf<String?>(null) }

    // Auto cleanup media resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.release()
                mediaPlayer?.release()
                pendingRecordingPath?.let { File(it).delete() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Media Recorders Helpers
    val startRecording = {
        try {
            val storageDir = File(context.filesDir, "attachments")
            if (!storageDir.exists()) storageDir.mkdirs()
            val voiceFile = File(storageDir, "audio_${System.currentTimeMillis()}.m4a")
            
            @Suppress("DEPRECATION")
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(voiceFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            pendingRecordingPath = voiceFile.absolutePath
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start recording: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val stopRecording = {
        val recordingPath = pendingRecordingPath
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            voiceFilePath = recordingPath
            Toast.makeText(context, "Voice note saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            recordingPath?.let { File(it).delete() }
            Toast.makeText(context, "Recording was not saved", Toast.LENGTH_SHORT).show()
        } finally {
            mediaRecorder = null
            pendingRecordingPath = null
            isRecording = false
        }
    }

    val startPlayback = {
        voiceFilePath?.let { path ->
            try {
                mediaPlayer?.runCatching { release() }
                mediaPlayer = null
                isPlaying = false
                val player = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlaying = false
                        release()
                        mediaPlayer = null
                    }
                }
                mediaPlayer = player
                isPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Playback failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val stopPlayback = {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    val deleteRecording = {
        try {
            stopPlayback()
            voiceFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            voiceFilePath = null
            Toast.makeText(context, "Voice recording deleted", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val addAttachment = { path: String ->
        if (path !in attachmentPaths) attachmentPaths.add(path)
        if (photoPathState == null) photoPathState = path
    }
    val removeAttachment = { path: String ->
        attachmentPaths.remove(path)
        if (photoPathState == path) photoPathState = attachmentPaths.firstOrNull()
        File(path).takeIf { it.exists() }?.delete()
    }

    // Launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(context, "Microphone permission required for recordings", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val storageDir = File(context.filesDir, "attachments")
            if (!storageDir.exists()) storageDir.mkdirs()
            val destFile = File(storageDir, fileName)
            try {
                destFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                addAttachment(destFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val path = copyUriToInternalStorage(context, uri, fileName)
            if (path != null) {
                addAttachment(path)
            }
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val originalName = getFileName(context, uri) ?: "file"
            val fileName = "doc_${System.currentTimeMillis()}_$originalName"
            val path = copyUriToInternalStorage(context, uri, fileName)
            if (path != null) {
                addAttachment(path)
            }
        }
    }

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
                actions = {
                    IconToggleButton(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it }
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin memory",
                            tint = if (isPinned) activeCategoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconToggleButton(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite memory",
                            tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                .padding(
                    start = metrics.horizontalPadding,
                    end = metrics.horizontalPadding,
                    top = metrics.verticalPadding,
                    bottom = metrics.verticalPadding * 2
                ),
            verticalArrangement = Arrangement.spacedBy(metrics.gridSpacing)
        ) {
            // ==========================================
            // FLOW SECTION 1: CATEGORY SELECTOR
            // ==========================================
            Text(
                text = "What do you want to remember?",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = metrics.titleFontSize),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing)
            ) {
                items(CategoryRegistry.categories) { catItem ->
                    val selected = category.lowercase() == catItem.name.lowercase()
                    val scale by animateFloatAsState(targetValue = if (selected) 1.05f else 1.0f)
                    val elevation by animateDpAsState(targetValue = if (selected) 6.dp else 1.dp)
                    val borderStroke = if (selected) {
                        BorderStroke(2.dp, catItem.color)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    }
                    
                    Card(
                        onClick = {
                            category = catItem.name
                        },
                        modifier = Modifier
                            .width(110.dp)
                            .height(95.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .testTag("category_card_${catItem.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                catItem.color.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = borderStroke,
                        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = catItem.icon,
                                contentDescription = catItem.name,
                                tint = if (selected) catItem.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = LanguageUtils.getString(catItem.name, language),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) catItem.color else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ==========================================
            // FLOW SECTION 2: DYNAMIC CATEGORY FIELDS
            // ==========================================
            val isCustomCategory = category.lowercase() != "note"
            AnimatedVisibility(
                visible = isCustomCategory,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = activeCategoryColor.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, activeCategoryColor.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${LanguageUtils.getString(category, language)} Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = activeCategoryColor
                        )

                        // ------------------ REQUIRED FIELDS ------------------
                        Text(
                            text = "Required Information",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        // Category specific inputs
                        when (category.lowercase()) {
                            "money" -> {
                                OutlinedTextField(
                                    value = person,
                                    onValueChange = { person = it },
                                    label = { Text("Person Name *") },
                                    placeholder = { Text("Friend, Vendor...") },
                                    modifier = Modifier.fillMaxWidth().testTag("money_person_input"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = { amount = it },
                                    label = { Text("Amount *") },
                                    placeholder = { Text("500") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("money_amount_input"),
                                    singleLine = true
                                )
                            }
                            "parking" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = parkingFloor,
                                        onValueChange = { parkingFloor = it },
                                        label = { Text("Floor *") },
                                        placeholder = { Text("B2, Floor 4...") },
                                        modifier = Modifier.weight(1f).testTag("parking_floor_input"),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = parkingSlot,
                                        onValueChange = { parkingSlot = it },
                                        label = { Text("Slot *") },
                                        placeholder = { Text("A34, 11...") },
                                        modifier = Modifier.weight(1f).testTag("parking_slot_input"),
                                        singleLine = true
                                    )
                                }
                            }
                            "document" -> {
                                OutlinedTextField(
                                    value = docType,
                                    onValueChange = { docType = it },
                                    label = { Text("Document Type *") },
                                    placeholder = { Text("Passport, License, Aadhaar...") },
                                    modifier = Modifier.fillMaxWidth().testTag("doc_type_input"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = docNumber,
                                    onValueChange = { docNumber = it },
                                    label = { Text("Document Number *") },
                                    placeholder = { Text("AXX991823...") },
                                    modifier = Modifier.fillMaxWidth().testTag("doc_number_input"),
                                    singleLine = true
                                )
                            }
                            "shopping" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newChecklistItemText,
                                        onValueChange = { newChecklistItemText = it },
                                        placeholder = { Text("Add checklist/shopping item...") },
                                        modifier = Modifier.weight(1f).testTag("shopping_item_input"),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            if (newChecklistItemText.trim().isNotEmpty()) {
                                                checklistItems.add(Pair(newChecklistItemText.trim(), false))
                                                newChecklistItemText = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = activeCategoryColor)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (checklistItems.isEmpty()) {
                                        Text(
                                            text = "No items in list yet * (Add at least one)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(8.dp)
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
                                                        contentDescription = "Remove item",
                                                        tint = Color(0xFFEF5350)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "medicine" -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
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
                            "place" -> {
                                OutlinedTextField(
                                    value = locationText,
                                    onValueChange = { locationText = it },
                                    label = { Text("Address / Location Spot *") },
                                    placeholder = { Text("123 Main St, Seattle...") },
                                    modifier = Modifier.fillMaxWidth().testTag("place_location_input"),
                                    singleLine = true
                                )
                            }
                            "gift idea" -> {
                                OutlinedTextField(
                                    value = person,
                                    onValueChange = { person = it },
                                    label = { Text("Recipient Name *") },
                                    placeholder = { Text("Mom, Dad, Friend...") },
                                    modifier = Modifier.fillMaxWidth().testTag("gift_recipient_input"),
                                    singleLine = true
                                )
                            }
                            "wishlist" -> {
                                OutlinedTextField(
                                    value = wishProduct,
                                    onValueChange = { wishProduct = it },
                                    label = { Text("Product / Item Name *") },
                                    placeholder = { Text("iPhone 16, New Shoes...") },
                                    modifier = Modifier.fillMaxWidth().testTag("wish_product_input"),
                                    singleLine = true
                                )
                            }
                        }

                        // ------------------ OPTIONAL FIELDS ("Additional Details") ------------------
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdditionalDetails = !showAdditionalDetails }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Additional Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (showAdditionalDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = "Toggle Additional Details"
                            )
                        }

                        AnimatedVisibility(
                            visible = showAdditionalDetails,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (category.lowercase()) {
                                    "money" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Transaction Status",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
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
                                                        "Pending",
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
                                                        "Settled",
                                                        color = if (isPaid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Category specific attachments option
                                        AttachmentPickerRow(
                                            category = category,
                                            photoPath = photoPathState,
                                            onPhotoSelected = { photoPathState = it },
                                            onCameraClicked = {
                                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                            },
                                            onGalleryClicked = { galleryLauncher.launch("image/*") },
                                            onDocClicked = { documentLauncher.launch("*/*") }
                                        )
                                    }
                                    "parking" -> {
                                        OutlinedTextField(
                                            value = vehicleName,
                                            onValueChange = { vehicleName = it },
                                            label = { Text("Vehicle Name") },
                                            placeholder = { Text("Red Tesla, Activa...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = locationText,
                                            onValueChange = { locationText = it },
                                            label = { Text("GPS / Landmark Notes") },
                                            placeholder = { Text("Near pillar C4, Level -1...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        AttachmentPickerRow(
                                            category = category,
                                            photoPath = photoPathState,
                                            onPhotoSelected = { photoPathState = it },
                                            onCameraClicked = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                            onGalleryClicked = { galleryLauncher.launch("image/*") },
                                            onDocClicked = { documentLauncher.launch("*/*") }
                                        )
                                    }
                                    "document" -> {
                                        OutlinedTextField(
                                            value = docIssuedBy,
                                            onValueChange = { docIssuedBy = it },
                                            label = { Text("Issued Authority") },
                                            placeholder = { Text("Govt of India, DMV...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
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
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Event, contentDescription = "Expiry", tint = activeCategoryColor)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(text = expiryStr, fontWeight = FontWeight.Bold)
                                                }
                                                Text(text = "Change Date", color = activeCategoryColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }

                                        AttachmentPickerRow(
                                            category = category,
                                            photoPath = photoPathState,
                                            onPhotoSelected = { photoPathState = it },
                                            onCameraClicked = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                            onGalleryClicked = { galleryLauncher.launch("image/*") },
                                            onDocClicked = { documentLauncher.launch("application/pdf") }
                                        )
                                    }
                                    "shopping" -> {
                                        OutlinedTextField(
                                            value = shoppingStore,
                                            onValueChange = { shoppingStore = it },
                                            label = { Text("Store Name") },
                                            placeholder = { Text("D-Mart, Whole Foods...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = shoppingBudget,
                                            onValueChange = { shoppingBudget = it },
                                            label = { Text("Budget") },
                                            placeholder = { Text("2000") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        AttachmentPickerRow(
                                            category = category,
                                            photoPath = photoPathState,
                                            onPhotoSelected = { photoPathState = it },
                                            onCameraClicked = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                            onGalleryClicked = { galleryLauncher.launch("image/*") },
                                            onDocClicked = { documentLauncher.launch("*/*") }
                                        )
                                    }
                                    "medicine" -> {
                                        OutlinedTextField(
                                            value = doctorName,
                                            onValueChange = { doctorName = it },
                                            label = { Text("Doctor Name") },
                                            placeholder = { Text("Dr. Sharma, Dr. Jones...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = dosage,
                                            onValueChange = { dosage = it },
                                            label = { Text("Dosage") },
                                            placeholder = { Text("1 Tablet, 5ml...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        AttachmentPickerRow(
                                            category = category,
                                            photoPath = photoPathState,
                                            onPhotoSelected = { photoPathState = it },
                                            onCameraClicked = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                            onGalleryClicked = { galleryLauncher.launch("image/*") },
                                            onDocClicked = { documentLauncher.launch("*/*") }
                                        )
                                    }
                                    "place" -> {
                                        OutlinedTextField(
                                            value = urlLinkText,
                                            onValueChange = { urlLinkText = it },
                                            label = { Text("Google Maps URL") },
                                            placeholder = { Text("https://maps.google.com...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = contactPerson,
                                            onValueChange = { contactPerson = it },
                                            label = { Text("Point of Contact Person") },
                                            placeholder = { Text("Manager, Friend, Host...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        AttachmentPickerRow(
                                            category = category,
                                            photoPath = photoPathState,
                                            onPhotoSelected = { photoPathState = it },
                                            onCameraClicked = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                            onGalleryClicked = { galleryLauncher.launch("image/*") },
                                            onDocClicked = { documentLauncher.launch("*/*") }
                                        )
                                    }
                                    "gift idea" -> {
                                        OutlinedTextField(
                                            value = giftOccasion,
                                            onValueChange = { giftOccasion = it },
                                            label = { Text("Occasion") },
                                            placeholder = { Text("Birthday, Anniversary, Christmas...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = priceText,
                                            onValueChange = { priceText = it },
                                            label = { Text("Budget") },
                                            placeholder = { Text("200") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = urlLinkText,
                                            onValueChange = { urlLinkText = it },
                                            label = { Text("Gift Web Link") },
                                            placeholder = { Text("https://giftshop.com...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        AttachmentPickerRow(
                                            category = category,
                                            photoPath = photoPathState,
                                            onPhotoSelected = { photoPathState = it },
                                            onCameraClicked = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                            onGalleryClicked = { galleryLauncher.launch("image/*") },
                                            onDocClicked = { documentLauncher.launch("*/*") }
                                        )
                                    }
                                    "wishlist" -> {
                                        OutlinedTextField(
                                            value = priceText,
                                            onValueChange = { priceText = it },
                                            label = { Text("Expected Price") },
                                            placeholder = { Text("15000") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = urlLinkText,
                                            onValueChange = { urlLinkText = it },
                                            label = { Text("Website Store Link") },
                                            placeholder = { Text("https://amazon.in...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        AttachmentPickerRow(
                                            category = category,
                                            photoPath = photoPathState,
                                            onPhotoSelected = { photoPathState = it },
                                            onCameraClicked = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                            onGalleryClicked = { galleryLauncher.launch("image/*") },
                                            onDocClicked = { documentLauncher.launch("*/*") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // FLOW SECTION 3: COMMON INFORMATION (No Header Title)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(LanguageUtils.getString("title_label", language) + " *") },
                        placeholder = { Text("What would you like to remember?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remember_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeCategoryColor,
                            focusedLabelColor = activeCategoryColor
                        )
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(LanguageUtils.getString("description_label", language)) },
                        placeholder = { Text("Add notes, descriptions, or visual cues...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeCategoryColor,
                            focusedLabelColor = activeCategoryColor
                        )
                    )
                }
            }

            // ==========================================
            // FLOW SECTION 4: REMINDER
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Set Reminder Alert",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = activeCategoryColor
                    )

                    val reminderStr = if (reminderTimestamp != null) {
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(reminderTimestamp!!))
                    } else "No Reminder Scheduled"

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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Notification reminder",
                                    tint = activeCategoryColor
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = reminderStr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (reminderTimestamp != null) {
                            IconButton(
                                onClick = { reminderTimestamp = null },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFFEF5350).copy(alpha = 0.15f)
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear reminder", tint = Color(0xFFEF5350))
                            }
                        }
                    }
                }
            }

            // ==========================================
            // FLOW SECTION 5: PRIORITY
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = LanguageUtils.getString("priority_label", language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = activeCategoryColor
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
                            val bg = if (selected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            val tc = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { priority = level }
                                    .padding(vertical = 10.dp),
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
                }
            }

            // ==========================================
            // FLOW SECTION 6: ATTACHMENTS (Real Audio/Photo Display)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Memory Attachments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = activeCategoryColor
                    )

                    // Display attached photo / document if it exists
                    photoPathState?.let { path ->
                        val isPdf = path.endsWith(".pdf", ignoreCase = true)
                        val isImage = path.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif")
                        if (!isImage) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Attached file",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = path.substringAfterLast("/"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isPdf) "PDF Document" else "Attached file",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                IconButton(onClick = { removeAttachment(path) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete document", tint = Color(0xFFEF5350))
                                }
                            }
                        } else {
                            // Image layout
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Attached Photo",
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { removeAttachment(path) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Photo", tint = Color.White)
                                }
                            }
                        }
                    }

                    if (attachmentPaths.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(attachmentPaths.filter { it != photoPathState }) { path ->
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            path.substringAfterLast('/'),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (path.endsWith(".pdf", true)) Icons.Default.Description else Icons.Default.AttachFile,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { removeAttachment(path) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove attachment")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    AttachmentPickerRow(
                        category = category,
                        photoPath = photoPathState,
                        onPhotoSelected = { path -> path?.let(addAttachment) },
                        onCameraClicked = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        onGalleryClicked = { galleryLauncher.launch("image/*") },
                        onDocClicked = { documentLauncher.launch("*/*") }
                    )

                    // Divider if we show both photo and voice
                    if (photoPathState != null || voiceFilePath != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }

                    // Voice Notes Player / Recorder UI
                    voiceFilePath?.let { path ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) stopPlayback() else startPlayback()
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = activeCategoryColor.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause recording",
                                            tint = activeCategoryColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPlaying) "Playing Audio..." else "Voice Note Attachment",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                IconButton(onClick = deleteRecording) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete voice note", tint = Color(0xFFEF5350))
                                }
                            }
                        }
                    } ?: run {
                        // Start recording UI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isRecording) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Recording Audio Note...", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = stopRecording,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop recording")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop", color = Color.White)
                                }
                            } else {
                                Text("No audio recorded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Button(
                                    onClick = {
                                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeCategoryColor)
                                ) {
                                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Record audio")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Record", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // FLOW SECTION 7: ACTION BUTTONS (Save Button)
            // ==========================================
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
                            title = "Remember $category"
                        }

                        // Build memory object
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
                            location = if (category.lowercase() == "parking" || category.lowercase() == "place") locationText.trim().ifEmpty { null } else null,
                            latitude = existingMemory?.latitude,
                            longitude = existingMemory?.longitude,
                            photoPath = if (category.lowercase() in listOf("note", "shopping", "gift idea")) photoPathState else null,
                            voicePath = voiceFilePath,
                            attachmentPaths = attachmentPaths.joinToString("\n"),
                            isPinned = isPinned,
                            isFavorite = isFavorite,
                            trashDate = existingMemory?.trashDate
                        )

                        // Assemble category detail object dynamically with correct parameters
                        val detailObj = when (category.lowercase()) {
                            "money" -> MoneyDetail(
                                memoryId = memory.id,
                                personName = person.trim(),
                                amount = amount.trim().toDoubleOrNull() ?: 0.0,
                                currency = "INR",
                                type = "General",
                                dueDate = existingMemoryWithDetails?.moneyDetail?.dueDate,
                                status = if (isPaid) "Returned" else "Pending",
                                receiptPhotoPath = photoPathState
                            )
                            "parking" -> ParkingDetail(
                                memoryId = memory.id,
                                floorLevel = parkingFloor.trim(),
                                slotNumber = parkingSlot.trim(),
                                parkingName = locationText.trim(),
                                gpsLocationDescription = "",
                                vehicleName = vehicleName.trim(),
                                photoPath = photoPathState
                            )
                            "document" -> DocumentDetail(
                                memoryId = memory.id,
                                documentType = docType.trim(),
                                documentNumber = docNumber.trim(),
                                issuedBy = docIssuedBy.trim(),
                                issueDate = existingMemoryWithDetails?.documentDetail?.issueDate,
                                expiryDate = docExpiryTimestamp,
                                photoPath = photoPathState
                            )
                            "shopping" -> ShoppingDetail(
                                memoryId = memory.id,
                                store = shoppingStore.trim(),
                                shoppingItems = checklistItems.joinToString("\n") { "${it.first}|${it.second}" },
                                budget = shoppingBudget.trim().toDoubleOrNull(),
                                completed = existingMemoryWithDetails?.shoppingDetail?.completed ?: false,
                                purchaseDate = existingMemoryWithDetails?.shoppingDetail?.purchaseDate
                            )
                            "medicine" -> MedicineDetail(
                                memoryId = memory.id,
                                medicineName = title.trim(),
                                dosage = dosage.trim(),
                                morning = medicineDoseMorning,
                                afternoon = medicineDoseAfternoon,
                                night = medicineDoseNight,
                                doctorName = doctorName.trim(),
                                prescriptionPhotoPath = photoPathState,
                                startDate = existingMemoryWithDetails?.medicineDetail?.startDate,
                                endDate = existingMemoryWithDetails?.medicineDetail?.endDate
                            )
                            "wishlist" -> WishlistDetail(
                                memoryId = memory.id,
                                productName = wishProduct.trim().ifEmpty { title.trim() },
                                expectedPrice = priceText.trim().toDoubleOrNull(),
                                websiteStore = urlLinkText.trim(),
                                priority = priority,
                                photoPath = photoPathState
                            )
                            "gift idea" -> GiftDetail(
                                memoryId = memory.id,
                                forPerson = person.trim(),
                                occasion = giftOccasion.trim(),
                                budget = priceText.trim().toDoubleOrNull(),
                                purchaseStatus = existingMemoryWithDetails?.giftDetail?.purchaseStatus ?: "Pending"
                            )
                            "place" -> PlaceDetail(
                                memoryId = memory.id,
                                placeName = title.trim(),
                                address = locationText.trim(),
                                gpsLocation = "",
                                contactPerson = contactPerson.trim(),
                                website = urlLinkText.trim(),
                                photoPath = photoPathState
                            )
                            else -> null
                        }

                        // Save transactionally
                        viewModel.saveMemory(memory, detailObj)
                        onSaveComplete()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("save_memory_button"),
                    enabled = !isRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeCategoryColor
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

@Composable
fun AttachmentPickerRow(
    category: String,
    photoPath: String?,
    onPhotoSelected: (String?) -> Unit,
    onCameraClicked: () -> Unit,
    onGalleryClicked: () -> Unit,
    onDocClicked: () -> Unit
) {
    val titleLabel = when (category.lowercase()) {
        "parking" -> "Attach Parking Photo"
        "money" -> "Attach Receipt Photo"
        "medicine" -> "Attach Prescription Photo"
        "document" -> "Attach PDF / Document Image"
        "place" -> "Attach Place Photo"
        "gift idea" -> "Attach Gift Photo"
        "wishlist" -> "Attach Wishlist Photo"
        "shopping" -> "Attach Purchase Receipt"
        else -> "Attach Optional Photo"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = titleLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (category.lowercase() == "document") {
                Button(
                    onClick = onDocClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "PDF File", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF / Files", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Button(
                    onClick = onGalleryClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Image File", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onCameraClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Button(
                    onClick = onGalleryClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun updateDefaultTitle(category: String, onTitleUpdate: (String) -> Unit) {
    onTitleUpdate(
        when (category.lowercase()) {
            "parking" -> "Car Parking Location"
            "medicine" -> "Daily Medicine Reminders"
            "shopping" -> "Shopping Items"
            else -> ""
        }
    )
}

fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val storageDir = File(context.filesDir, "attachments")
        if (!storageDir.exists()) storageDir.mkdirs()
        val destFile = File(storageDir, File(fileName).name)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            destFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            destFile.absolutePath
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
    }
    if (name == null) {
        name = uri.path
        val cut = name?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            name = name?.substring(cut + 1)
        }
    }
    return name
}
