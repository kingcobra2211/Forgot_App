package com.example.data.repository

import com.example.data.dao.MemoryDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val activeMemories: Flow<List<MemoryWithDetails>> = memoryDao.getActiveMemories()
    val archivedMemories: Flow<List<MemoryWithDetails>> = memoryDao.getArchivedMemories()
    val trashMemories: Flow<List<MemoryWithDetails>> = memoryDao.getTrashMemories()
    val activeReminders: Flow<List<MemoryWithDetails>> = memoryDao.getActiveReminders()

    suspend fun getMemoryById(id: Int): MemoryWithDetails? {
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

    suspend fun saveMemoryWithDetails(memory: Memory, detail: Any?) {
        val memoryId = if (memory.id == 0) {
            memoryDao.insertMemory(memory).toInt()
        } else {
            memoryDao.updateMemory(memory)
            memory.id
        }

        // Clean previous 1:1 records to support smooth category changes
        clearAllDetailsForMemory(memoryId)

        // Save new composition detail
        if (detail != null) {
            when (detail) {
                is ParkingDetail -> memoryDao.insertParkingDetail(detail.copy(memoryId = memoryId))
                is MoneyDetail -> memoryDao.insertMoneyDetail(detail.copy(memoryId = memoryId))
                is DocumentDetail -> memoryDao.insertDocumentDetail(detail.copy(memoryId = memoryId))
                is MedicineDetail -> memoryDao.insertMedicineDetail(detail.copy(memoryId = memoryId))
                is ShoppingDetail -> memoryDao.insertShoppingDetail(detail.copy(memoryId = memoryId))
                is PlaceDetail -> memoryDao.insertPlaceDetail(detail.copy(memoryId = memoryId))
                is GiftDetail -> memoryDao.insertGiftDetail(detail.copy(memoryId = memoryId))
                is WishlistDetail -> memoryDao.insertWishlistDetail(detail.copy(memoryId = memoryId))
            }
        }
    }

    suspend fun clearAllDetailsForMemory(memoryId: Int) {
        memoryDao.deleteParkingDetail(memoryId)
        memoryDao.deleteMoneyDetail(memoryId)
        memoryDao.deleteDocumentDetail(memoryId)
        memoryDao.deleteMedicineDetail(memoryId)
        memoryDao.deleteShoppingDetail(memoryId)
        memoryDao.deletePlaceDetail(memoryId)
        memoryDao.deleteGiftDetail(memoryId)
        memoryDao.deleteWishlistDetail(memoryId)
    }

    suspend fun emptyTrash() {
        memoryDao.emptyTrash()
    }

    suspend fun clearAll() {
        memoryDao.clearAll()
    }

    fun searchMemories(query: String, status: String = "Active"): Flow<List<MemoryWithDetails>> {
        return memoryDao.searchMemories(query, status)
    }
}
