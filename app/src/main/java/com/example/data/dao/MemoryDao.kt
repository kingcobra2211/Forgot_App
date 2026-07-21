package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Transaction
    @Query("SELECT * FROM memories WHERE status = 'Active' ORDER BY isPinned DESC, createdDate DESC")
    fun getActiveMemories(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE status = 'Archived' ORDER BY createdDate DESC")
    fun getArchivedMemories(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE status = 'Trash' ORDER BY trashDate DESC")
    fun getTrashMemories(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE status = 'Active' AND reminderDate IS NOT NULL ORDER BY reminderDate ASC")
    fun getActiveReminders(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: Int): MemoryWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Update
    suspend fun updateMemory(memory: Memory)

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("DELETE FROM memories WHERE status = 'Trash'")
    suspend fun emptyTrash()

    @Query("DELETE FROM memories")
    suspend fun clearAll()

    @Transaction
    @Query("""
        SELECT * FROM memories 
        WHERE status = :status 
        AND (
            title LIKE '%' || :query || '%' 
            OR description LIKE '%' || :query || '%' 
            OR category LIKE '%' || :query || '%'
        )
        ORDER BY isPinned DESC, createdDate DESC
    """)
    fun searchMemories(query: String, status: String = "Active"): Flow<List<MemoryWithDetails>>

    // Detail insertion methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParkingDetail(detail: ParkingDetail)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyDetail(detail: MoneyDetail)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentDetail(detail: DocumentDetail)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicineDetail(detail: MedicineDetail)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingDetail(detail: ShoppingDetail)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaceDetail(detail: PlaceDetail)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiftDetail(detail: GiftDetail)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistDetail(detail: WishlistDetail)

    // Detail deletion methods
    @Query("DELETE FROM parking_details WHERE memoryId = :memoryId")
    suspend fun deleteParkingDetail(memoryId: Int)

    @Query("DELETE FROM money_details WHERE memoryId = :memoryId")
    suspend fun deleteMoneyDetail(memoryId: Int)

    @Query("DELETE FROM document_details WHERE memoryId = :memoryId")
    suspend fun deleteDocumentDetail(memoryId: Int)

    @Query("DELETE FROM medicine_details WHERE memoryId = :memoryId")
    suspend fun deleteMedicineDetail(memoryId: Int)

    @Query("DELETE FROM shopping_details WHERE memoryId = :memoryId")
    suspend fun deleteShoppingDetail(memoryId: Int)

    @Query("DELETE FROM place_details WHERE memoryId = :memoryId")
    suspend fun deletePlaceDetail(memoryId: Int)

    @Query("DELETE FROM gift_details WHERE memoryId = :memoryId")
    suspend fun deleteGiftDetail(memoryId: Int)

    @Query("DELETE FROM wishlist_details WHERE memoryId = :memoryId")
    suspend fun deleteWishlistDetail(memoryId: Int)
}
