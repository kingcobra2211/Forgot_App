package com.example.data.repository

import com.example.data.dao.MemoryDao
import com.example.data.model.Memory
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val activeMemories: Flow<List<Memory>> = memoryDao.getActiveMemories()
    val archivedMemories: Flow<List<Memory>> = memoryDao.getArchivedMemories()
    val trashMemories: Flow<List<Memory>> = memoryDao.getTrashMemories()
    val activeReminders: Flow<List<Memory>> = memoryDao.getActiveReminders()

    suspend fun getMemoryById(id: Int): Memory? {
        return memoryDao.getMemoryById(id)
    }

    suspend fun insertMemory(memory: Memory): Long {
        return memoryDao.insertMemory(memory)
    }

    suspend fun updateMemory(memory: Memory) {
        memoryDao.updateMemory(memory)
    }

    suspend fun deleteMemory(memory: Memory) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun emptyTrash() {
        memoryDao.emptyTrash()
    }

    suspend fun clearAll() {
        memoryDao.clearAll()
    }

    fun searchMemories(query: String, status: String = "Active"): Flow<List<Memory>> {
        return memoryDao.searchMemories(query, status)
    }
}
