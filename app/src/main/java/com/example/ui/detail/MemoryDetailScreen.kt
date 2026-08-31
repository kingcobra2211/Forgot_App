package com.example.ui.detail

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.viewmodel.MemoryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memoryId: Int,
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit,
    onEditMemory: (Int) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val language by viewModel.language.collectAsState()

    val activeMemories by viewModel.activeMemories.collectAsState()
    val archivedMemories by viewModel.archivedMemories.collectAsState()
    val trashMemories by viewModel.trashMemories.collectAsState()

    val memoryWithDetails = remember(memoryId, activeMemories, archivedMemories, trashMemories) {
        activeMemories.firstOrNull { it.memory.id == memoryId }
            ?: archivedMemories.firstOrNull { it.memory.id == memoryId }
            ?: trashMemories.firstOrNull { it.memory.id == memoryId }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedFullImageUri by remember { mutableStateOf<String?>(null) }

    // Audio playback state
    var isPlayingAudio by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    if (memoryWithDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val memory = memoryWithDetails.memory
    val categoryItem = CategoryRegistry.getCategoryItem(memory.category)
    val activeCategoryColor = categoryItem.color

    val allAttachments = remember(memory) {
        val list = mutableListOf<String>()
        if (!memory.photoPath.isNullOrBlank()) list.add(memory.photoPath)
        memory.attachmentPaths.split("\n")
            .filter { it.isNotBlank() }
            .forEach { if (!list.contains(it)) list.add(it) }
        list
    }

    // Safety Delete Dialog
    if (showDeleteConfirmDialog) {
        val isTrash = memory.status == "Trash"
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (isTrash) "Delete Permanently?" else "Move Memory to Trash?", fontWeight = FontWeight.Bold) },
            text = { Text(if (isTrash) "Are you sure you want to permanently delete \"${memory.title}\"?" else "Are you sure you want to move \"${memory.title}\" to trash?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        if (isTrash) viewModel.deleteMemoryPermanently(memory)
                        else viewModel.moveMemoryToTrash(memory)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text(if (isTrash) "Delete" else "Move to Trash", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Screen Image Preview Overlay
    if (selectedFullImageUri != null) {
        Dialog(onDismissRequest = { selectedFullImageUri = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Photo Attachment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { selectedFullImageUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close preview")
                        }
                    }

                    AsyncImage(
                        model = selectedFullImageUri,
                        contentDescription = "Full Image Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Button(
                        onClick = {
                            saveImageToGallery(context, selectedFullImageUri!!)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = activeCategoryColor)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download to Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = categoryItem.icon, contentDescription = null, tint = activeCategoryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = memory.title, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (memory.status != "Trash") {
                        IconButton(onClick = { onEditMemory(memory.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = if (memory.status == "Trash") Icons.Default.DeleteForever else Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF5350)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Badge & Priority Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = activeCategoryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, activeCategoryColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = LanguageUtils.getString(memory.category, language),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        color = activeCategoryColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Text(
                    text = "Priority: ${memory.priority}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description with Copy Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = activeCategoryColor)
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(memory.description))
                                Toast.makeText(context, "Description copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy description", modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = memory.description.ifBlank { "No description provided." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Reminder Alert Badge if present
            if (memory.reminderDate != null) {
                val formattedReminder = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(memory.reminderDate))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = activeCategoryColor.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, activeCategoryColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = activeCategoryColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Scheduled Reminder", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = activeCategoryColor)
                            Text(formattedReminder, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Category Details Card
            RenderCategoryDetails(memoryWithDetails = memoryWithDetails, activeColor = activeCategoryColor)

            // Voice Recording Player
            if (!memory.voicePath.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isPlayingAudio) {
                                        mediaPlayer?.pause()
                                        isPlayingAudio = false
                                    } else {
                                        try {
                                            if (mediaPlayer == null) {
                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(memory.voicePath)
                                                    prepare()
                                                    setOnCompletionListener { isPlayingAudio = false }
                                                }
                                            }
                                            mediaPlayer?.start()
                                            isPlayingAudio = true
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "Play/Pause Voice Note",
                                    tint = activeCategoryColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Voice Recording Note", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(if (isPlayingAudio) "Playing audio..." else "Tap play button to listen", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Photo Attachments Gallery Preview
            if (allAttachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Attachments (${allAttachments.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = activeCategoryColor)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(allAttachments) { path ->
                            Surface(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedFullImageUri = path },
                                border = BorderStroke(1.dp, activeCategoryColor.copy(alpha = 0.3f))
                            ) {
                                AsyncImage(
                                    model = path,
                                    contentDescription = "Attachment Thumbnail",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RenderCategoryDetails(memoryWithDetails: MemoryWithDetails, activeColor: Color) {
    when (memoryWithDetails.memory.category.lowercase()) {
        "money" -> {
            val m = memoryWithDetails.moneyDetail
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Money Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = activeColor)
                    Text("Person: ${memoryWithDetails.person ?: "-"}")
                    Text("Amount: ₹${memoryWithDetails.amount ?: 0.0}")
                    Text("Status: ${if (memoryWithDetails.isPaid) "Settled / Returned" else "Pending Return"}", fontWeight = FontWeight.Bold, color = if (memoryWithDetails.isPaid) Color(0xFF2E7D32) else Color(0xFFEF5350))
                }
            }
        }
        "parking" -> {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Parking Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = activeColor)
                    Text("Floor: ${memoryWithDetails.parkingFloor ?: "-"}")
                    Text("Slot: ${memoryWithDetails.parkingSlot ?: "-"}")
                    Text("Location: ${memoryWithDetails.location ?: "-"}")
                }
            }
        }
        "document" -> {
            val d = memoryWithDetails.documentDetail
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Document Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = activeColor)
                    Text("Document Type: ${d?.documentType ?: "-"}")
                    Text("Document Number: ${d?.documentNumber ?: "-"}")
                    Text("Issued By: ${d?.issuedBy ?: "-"}")
                }
            }
        }
        "medicine" -> {
            val m = memoryWithDetails.medicineDetail
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Medicine Dosage Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = activeColor)
                    Text("Morning Dose ☀️: ${if (memoryWithDetails.medicineDoseMorning) "Yes" else "No"}")
                    Text("Afternoon Dose 🌤️: ${if (memoryWithDetails.medicineDoseAfternoon) "Yes" else "No"}")
                    Text("Night Dose 🌙: ${if (memoryWithDetails.medicineDoseNight) "Yes" else "No"}")
                    if (!m?.doctorName.isNullOrBlank()) Text("Doctor: ${m?.doctorName}")
                }
            }
        }
    }
}

private fun saveImageToGallery(context: Context, path: String) {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } else {
            BitmapFactory.decodeFile(path, options)
        }

        // Calculate sample size for max 2048px dimension
        var sampleSize = 1
        val maxDim = maxOf(options.outWidth, options.outHeight)
        while (maxDim / sampleSize > 2048) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val bitmap = if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } else {
            BitmapFactory.decodeFile(path, decodeOptions)
        }

        if (bitmap == null) {
            Toast.makeText(context, "Failed to load image for download", Toast.LENGTH_SHORT).show()
            return
        }

        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ForgotApp")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                Toast.makeText(context, "Image saved to Gallery!", Toast.LENGTH_SHORT).show()
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "ForgotApp").apply { if (!exists()) mkdirs() }
            val file = File(appDir, filename)
            file.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            Toast.makeText(context, "Image saved to Gallery!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
