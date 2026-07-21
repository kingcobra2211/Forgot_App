package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Memory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE status = 'Active' ORDER BY isPinned DESC, createdDate DESC")
    fun getActiveMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE status = 'Archived' ORDER BY createdDate DESC")
    fun getArchivedMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE status = 'Trash' ORDER BY trashDate DESC")
    fun getTrashMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE status = 'Active' AND reminderDate IS NOT NULL ORDER BY reminderDate ASC")
    fun getActiveReminders(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: Int): Memory?

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

    @Query("""
        SELECT * FROM memories 
        WHERE status = :status 
        AND (
            title LIKE '%' || :query || '%' 
            OR description LIKE '%' || :query || '%' 
            OR category LIKE '%' || :query || '%' 
            OR person LIKE '%' || :query || '%' 
            OR location LIKE '%' || :query || '%'
            OR parkingFloor LIKE '%' || :query || '%'
            OR parkingSlot LIKE '%' || :query || '%'
        )
        ORDER BY isPinned DESC, createdDate DESC
    """)
    fun searchMemories(query: String, status: String = "Active"): Flow<List<Memory>>
}
