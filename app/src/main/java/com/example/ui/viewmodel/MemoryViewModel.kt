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

    // Reactive Search Results combining query, category filter and active memories
    val searchResults: StateFlow<List<MemoryWithDetails>> = combine(
        activeMemories,
        searchQuery,
        selectedCategory
    ) { memories, query, category ->
        memories.filter { memoryWithDetails ->
            val memory = memoryWithDetails.memory
            val matchesQuery = query.isEmpty() ||
                memory.title.contains(query, ignoreCase = true) ||
                memory.description.contains(query, ignoreCase = true) ||
                memory.category.contains(query, ignoreCase = true) ||
                (memoryWithDetails.person?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.location?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.parkingFloor?.contains(query, ignoreCase = true) ?: false) ||
                (memoryWithDetails.parkingSlot?.contains(query, ignoreCase = true) ?: false)

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

    fun saveMemory(memory: Memory, detail: Any? = null) {
        viewModelScope.launch {
            val savedMemory = repository.saveMemoryWithDetails(memory, detail)
            reminderScheduler.schedule(savedMemory)
        }
    }

    fun updateMemory(memory: Memory) {
        viewModelScope.launch {
            repository.updateMemory(memory)
            reminderScheduler.schedule(memory)
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

    // Export memories as a local JSON string
    fun exportBackup(): String? {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, MemoryWithDetails::class.java)
            val adapter = moshi.adapter<List<MemoryWithDetails>>(listType)

            // Let's export active, archived and trash memories
            val allList = mutableListOf<MemoryWithDetails>()
            allList.addAll(activeMemories.value)
            allList.addAll(archivedMemories.value)
            allList.addAll(trashMemories.value)

            adapter.toJson(allList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Import memories from a JSON string, restoring them to the database
    private suspend fun importBackup(json: String): Boolean {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, MemoryWithDetails::class.java)
            val adapter = moshi.adapter<List<MemoryWithDetails>>(listType)

            val importedMemories = adapter.fromJson(json) ?: return false

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
