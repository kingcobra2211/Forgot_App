package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.MemoryRepository
import com.example.data.repository.ReminderScheduler
import com.example.ui.utils.LanguageUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MemoryRepository
    private val reminderScheduler = ReminderScheduler(application)
    private val sharedPrefs = application.getSharedPreferences("forgot_prefs", Context.MODE_PRIVATE)

    val activeMemories: StateFlow<List<MemoryWithDetails>>
    val archivedMemories: StateFlow<List<MemoryWithDetails>>
    val trashMemories: StateFlow<List<MemoryWithDetails>>
    val activeReminders: StateFlow<List<MemoryWithDetails>>

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)

    val themeKey = MutableStateFlow(sharedPrefs.getString("theme_key", "dark") ?: "dark")
    val language = MutableStateFlow(sharedPrefs.getString("language_key", "english") ?: "english")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MemoryRepository(database, database.memoryDao())

        activeMemories = repository.activeMemories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        archivedMemories = repository.archivedMemories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        trashMemories = repository.trashMemories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeReminders = repository.activeReminders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Auto delete trash older than 30 days on launch
        performTrashCleanup()
        rescheduleReminders()
    }

    val searchScope = MutableStateFlow("active") // "active", "archived", "all"

    // Reactive Search Results combining query, category filter and active/archived memories
    val searchResults: StateFlow<List<MemoryWithDetails>> = combine(
        activeMemories,
        archivedMemories,
        searchQuery,
        selectedCategory,
        searchScope
    ) { active, archived, query, category, scope ->
        val pool = when (scope) {
            "archived" -> archived
            "all" -> active + archived
            else -> active
        }
        pool.filter { memoryWithDetails ->
            val memory = memoryWithDetails.memory
            val matchesQuery = query.isEmpty() ||
                memory.title.contains(query, ignoreCase = true) ||
                memory.description.contains(query, ignoreCase = true) ||
                memory.category.contains(query, ignoreCase = true) ||
                (memoryWithDetails.person?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.location?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.parkingFloor?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.parkingSlot?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.documentDetail?.documentNumber?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.documentDetail?.documentType?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.parkingDetail?.vehicleName?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.medicineDetail?.doctorName?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.medicineDetail?.medicineName?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.shoppingDetail?.store?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.shoppingDetail?.shoppingItems?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.placeDetail?.contactPerson?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.urlLink?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.wishlistDetail?.productName?.contains(query, ignoreCase = true) ?: false)

            val matchesCategory = category == null || memory.category.lowercase() == category.lowercase()
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTheme(newTheme: String) {
        themeKey.value = newTheme
        sharedPrefs.edit().putString("theme_key", newTheme).apply()
    }

    fun updateLanguage(newLanguage: String) {
        language.value = newLanguage
        sharedPrefs.edit().putString("language_key", newLanguage).apply()
    }

    fun saveMemory(memory: Memory, detail: Any? = null, isDaily: Boolean = false) {
        viewModelScope.launch {
            val savedMemory = repository.saveMemoryWithDetails(memory, detail)
            reminderScheduler.schedule(savedMemory, isDaily = isDaily)
        }
    }

    fun updateMemory(memory: Memory, isDaily: Boolean = false) {
        viewModelScope.launch {
            repository.updateMemory(memory)
            reminderScheduler.schedule(memory, isDaily = isDaily)
        }
    }

    fun pinMemory(memory: Memory, isPinned: Boolean) {
        viewModelScope.launch {
            repository.updateMemory(memory.copy(isPinned = isPinned, updatedDate = System.currentTimeMillis()))
        }
    }

    fun favoriteMemory(memory: Memory, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateMemory(memory.copy(isFavorite = isFavorite, updatedDate = System.currentTimeMillis()))
        }
    }

    fun archiveMemory(memory: Memory) {
        viewModelScope.launch {
            val archivedMemory = memory.copy(
                status = "Archived",
                isPinned = false,
                updatedDate = System.currentTimeMillis()
            )
            repository.updateMemory(archivedMemory)
            reminderScheduler.cancel(memory.id)
        }
    }

    fun unarchiveMemory(memory: Memory) {
        viewModelScope.launch {
            val restoredMemory = memory.copy(status = "Active", updatedDate = System.currentTimeMillis())
            repository.updateMemory(restoredMemory)
            reminderScheduler.schedule(restoredMemory)
        }
    }

    fun moveMemoryToTrash(memory: Memory) {
        viewModelScope.launch {
            repository.updateMemory(
                memory.copy(
                    status = "Trash",
                    isPinned = false,
                    isFavorite = false,
                    trashDate = System.currentTimeMillis(),
                    updatedDate = System.currentTimeMillis()
                )
            )
            reminderScheduler.cancel(memory.id)
        }
    }

    fun restoreMemoryFromTrash(memory: Memory) {
        viewModelScope.launch {
            val restoredMemory = memory.copy(
                status = "Active",
                trashDate = null,
                updatedDate = System.currentTimeMillis()
            )
            repository.updateMemory(restoredMemory)
            reminderScheduler.schedule(restoredMemory)
        }
    }

    fun deleteMemoryPermanently(memory: Memory) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
            reminderScheduler.cancel(memory.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashMemories.value.forEach { reminderScheduler.cancel(it.memory.id) }
            repository.emptyTrash()
        }
    }

    // Auto-deletes trash memories older than 30 days
    private fun performTrashCleanup() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
            trashMemories.first().forEach { memoryWithDetails ->
                val memory = memoryWithDetails.memory
                if (memory.trashDate != null && memory.trashDate < thirtyDaysAgo) {
                    repository.deleteMemory(memory)
                    reminderScheduler.cancel(memory.id)
                }
            }
        }
    }

data class BackupPayload(
    val memoryWithDetails: MemoryWithDetails,
    val photoBase64: String? = null,
    val voiceBase64: String? = null
)

    // Export memories and all attachments (photos & voice notes) as a local JSON string
    fun exportBackup(): String? {
        return try {
            val context = getApplication<Application>()
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, BackupPayload::class.java)
            val adapter = moshi.adapter<List<BackupPayload>>(listType)

            // Let's export active, archived and trash memories
            val allList = mutableListOf<MemoryWithDetails>()
            allList.addAll(activeMemories.value)
            allList.addAll(archivedMemories.value)
            allList.addAll(trashMemories.value)

            val maxFileSizeBytes = 8 * 1024 * 1024L // 8 MB safety limit per file for JSON base64
            val payloadList = allList.map { item ->
                var photoB64: String? = null
                var voiceB64: String? = null
                val photoPathToExport = item.memory.photoPath
                    ?: item.parkingDetail?.photoPath
                    ?: item.moneyDetail?.receiptPhotoPath
                    ?: item.documentDetail?.photoPath
                    ?: item.medicineDetail?.prescriptionPhotoPath
                    ?: item.placeDetail?.photoPath
                    ?: item.wishlistDetail?.photoPath

                val photoFile = photoPathToExport?.let { File(it) }
                if (photoFile != null && photoFile.exists() && photoFile.length() <= maxFileSizeBytes) {
                    photoB64 = android.util.Base64.encodeToString(photoFile.readBytes(), android.util.Base64.NO_WRAP)
                }
                val voiceFile = item.memory.voicePath?.let { File(it) }
                if (voiceFile != null && voiceFile.exists() && voiceFile.length() <= maxFileSizeBytes) {
                    voiceB64 = android.util.Base64.encodeToString(voiceFile.readBytes(), android.util.Base64.NO_WRAP)
                }
                BackupPayload(item, photoB64, voiceB64)
            }

            adapter.toJson(payloadList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Import memories and restore all attached photos and voice recordings
    private suspend fun importBackup(json: String): Boolean {
        return try {
            val context = getApplication<Application>()
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            
            // Backward compatibility: try reading BackupPayload list first, if fail try MemoryWithDetails list
            val payloadListType = Types.newParameterizedType(List::class.java, BackupPayload::class.java)
            val payloadAdapter = moshi.adapter<List<BackupPayload>>(payloadListType)

            val payloads = try {
                payloadAdapter.fromJson(json)
            } catch (e: Exception) {
                null
            }

            val attachmentsDir = File(context.filesDir, "attachments").apply { if (!exists()) mkdirs() }

            val importedMemories = if (!payloads.isNullOrEmpty()) {
                payloads.map { payload ->
                    var newPhotoPath = payload.memoryWithDetails.memory.photoPath
                    var newVoicePath = payload.memoryWithDetails.memory.voicePath

                    if (!payload.photoBase64.isNullOrEmpty()) {
                        val bytes = android.util.Base64.decode(payload.photoBase64, android.util.Base64.DEFAULT)
                        val file = File(attachmentsDir, "photo_${System.currentTimeMillis()}_${payload.memoryWithDetails.memory.id}.jpg")
                        file.writeBytes(bytes)
                        newPhotoPath = file.absolutePath
                    }

                    if (!payload.voiceBase64.isNullOrEmpty()) {
                        val bytes = android.util.Base64.decode(payload.voiceBase64, android.util.Base64.DEFAULT)
                        val file = File(attachmentsDir, "voice_${System.currentTimeMillis()}_${payload.memoryWithDetails.memory.id}.3gp")
                        file.writeBytes(bytes)
                        newVoicePath = file.absolutePath
                    }

                    val updatedMemory = payload.memoryWithDetails.memory.copy(
                        photoPath = newPhotoPath,
                        voicePath = newVoicePath
                    )
                    val updatedParking = payload.memoryWithDetails.parkingDetail?.copy(photoPath = newPhotoPath ?: payload.memoryWithDetails.parkingDetail?.photoPath)
                    val updatedMoney = payload.memoryWithDetails.moneyDetail?.copy(receiptPhotoPath = newPhotoPath ?: payload.memoryWithDetails.moneyDetail?.receiptPhotoPath)
                    val updatedDoc = payload.memoryWithDetails.documentDetail?.copy(photoPath = newPhotoPath ?: payload.memoryWithDetails.documentDetail?.photoPath)
                    val updatedMed = payload.memoryWithDetails.medicineDetail?.copy(prescriptionPhotoPath = newPhotoPath ?: payload.memoryWithDetails.medicineDetail?.prescriptionPhotoPath)
                    val updatedPlace = payload.memoryWithDetails.placeDetail?.copy(photoPath = newPhotoPath ?: payload.memoryWithDetails.placeDetail?.photoPath)
                    val updatedWish = payload.memoryWithDetails.wishlistDetail?.copy(photoPath = newPhotoPath ?: payload.memoryWithDetails.wishlistDetail?.photoPath)

                    payload.memoryWithDetails.copy(
                        memory = updatedMemory,
                        parkingDetail = updatedParking,
                        moneyDetail = updatedMoney,
                        documentDetail = updatedDoc,
                        medicineDetail = updatedMed,
                        placeDetail = updatedPlace,
                        wishlistDetail = updatedWish
                    )
                }
            } else {
                val listType = Types.newParameterizedType(List::class.java, MemoryWithDetails::class.java)
                val adapter = moshi.adapter<List<MemoryWithDetails>>(listType)
                adapter.fromJson(json) ?: return false
            }

            repository.restoreBackup(importedMemories)
            rescheduleReminders()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export backup to a file using Android's Storage Access Framework (SAF)
    fun performExportBackup(uri: Uri) {
        val context = getApplication<Application>()
        val json = exportBackup()
        if (json == null) {
            Toast.makeText(context, LanguageUtils.getString("export_failed", language.value), Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                val fileName = uri.path?.split("/")?.lastOrNull() ?: "backup.json"
                Toast.makeText(context, LanguageUtils.getString("export_success", language.value) + fileName, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, LanguageUtils.getString("export_failed", language.value) + ": ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Import backup from a file using Android's Storage Access Framework (SAF)
    fun performImportBackup(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }

                if (json != null && importBackup(json)) {
                    Toast.makeText(context, LanguageUtils.getString("import_success", language.value), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, LanguageUtils.getString("import_failed", language.value), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, LanguageUtils.getString("import_failed", language.value) + ": ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rescheduleReminders() {
        viewModelScope.launch {
            repository.activeReminders.first().forEach { reminderScheduler.schedule(it.memory) }
        }
    }
}
