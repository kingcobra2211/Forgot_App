package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.dao.MemoryDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val database: AppDatabase,
    private val memoryDao: MemoryDao
) {
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

    suspend fun saveMemoryWithDetails(memory: Memory, detail: Any?): Memory = database.withTransaction {
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

        memory.copy(id = memoryId)
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

    suspend fun restoreBackup(memories: List<MemoryWithDetails>) = database.withTransaction {
        memoryDao.clearAll()
        memories.forEach { memoryWithDetails ->
            val baseMemory = memoryWithDetails.memory.copy(id = 0)
            val detail = when (baseMemory.category.lowercase()) {
                "parking" -> memoryWithDetails.parkingDetail
                "money" -> memoryWithDetails.moneyDetail
                "document" -> memoryWithDetails.documentDetail
                "medicine" -> memoryWithDetails.medicineDetail
                "shopping" -> memoryWithDetails.shoppingDetail
                "place" -> memoryWithDetails.placeDetail
                "gift idea" -> memoryWithDetails.giftDetail
                "wishlist" -> memoryWithDetails.wishlistDetail
                else -> null
            }
            saveMemoryWithDetails(baseMemory, detail)
        }
    }

    fun searchMemories(query: String, status: String = "Active"): Flow<List<MemoryWithDetails>> {
        return memoryDao.searchMemories(query, status)
    }
}
