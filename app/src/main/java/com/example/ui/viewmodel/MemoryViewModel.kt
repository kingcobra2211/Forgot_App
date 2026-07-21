package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Memory
import com.example.data.repository.MemoryRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MemoryRepository
    private val sharedPrefs = application.getSharedPreferences("forgot_prefs", Context.MODE_PRIVATE)

    val activeMemories: StateFlow<List<Memory>>
    val archivedMemories: StateFlow<List<Memory>>
    val trashMemories: StateFlow<List<Memory>>
    val activeReminders: StateFlow<List<Memory>>

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)

    val themeKey = MutableStateFlow(sharedPrefs.getString("theme_key", "dark") ?: "dark")
    val language = MutableStateFlow(sharedPrefs.getString("language_key", "english") ?: "english")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MemoryRepository(database.memoryDao())

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
    }

    // Reactive Search Results combining query, category filter and active memories
    val searchResults: StateFlow<List<Memory>> = combine(
        activeMemories,
        searchQuery,
        selectedCategory
    ) { memories, query, category ->
        memories.filter { memory ->
            val matchesQuery = query.isEmpty() ||
                memory.title.contains(query, ignoreCase = true) ||
                memory.description.contains(query, ignoreCase = true) ||
                memory.category.contains(query, ignoreCase = true) ||
                (memory.person?.contains(query, ignoreCase = true) ?: false) ||
                (memory.location?.contains(query, ignoreCase = true) ?: false) ||
                (memory.parkingFloor?.contains(query, ignoreCase = true) ?: false) ||
                (memory.parkingSlot?.contains(query, ignoreCase = true) ?: false)

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

    fun saveMemory(memory: Memory) {
        viewModelScope.launch {
            repository.insertMemory(memory)
        }
    }

    fun updateMemory(memory: Memory) {
        viewModelScope.launch {
            repository.updateMemory(memory)
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
            repository.updateMemory(
                memory.copy(
                    status = "Archived",
                    isPinned = false,
                    updatedDate = System.currentTimeMillis()
                )
            )
        }
    }

    fun unarchiveMemory(memory: Memory) {
        viewModelScope.launch {
            repository.updateMemory(
                memory.copy(
                    status = "Active",
                    updatedDate = System.currentTimeMillis()
                )
            )
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
        }
    }

    fun restoreMemoryFromTrash(memory: Memory) {
        viewModelScope.launch {
            repository.updateMemory(
                memory.copy(
                    status = "Active",
                    trashDate = null,
                    updatedDate = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteMemoryPermanently(memory: Memory) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    // Auto-deletes trash memories older than 30 days
    private fun performTrashCleanup() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
            trashMemories.first().forEach { memory ->
                if (memory.trashDate != null && memory.trashDate < thirtyDaysAgo) {
                    repository.deleteMemory(memory)
                }
            }
        }
    }

    // Export memories as a local JSON string
    fun exportBackup(context: Context): String? {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, Memory::class.java)
            val adapter = moshi.adapter<List<Memory>>(listType)

            // Let's export active, archived and trash memories
            val allList = mutableListOf<Memory>()
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
    fun importBackup(json: String): Boolean {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, Memory::class.java)
            val adapter = moshi.adapter<List<Memory>>(listType)

            val importedMemories = adapter.fromJson(json) ?: return false

            viewModelScope.launch {
                repository.clearAll()
                for (memory in importedMemories) {
                    // Reset IDs to ensure fresh insertion without conflicts, or keep them if unique
                    repository.insertMemory(memory.copy(id = 0))
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
