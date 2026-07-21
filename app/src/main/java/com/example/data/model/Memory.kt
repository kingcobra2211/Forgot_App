package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // "Note", "Money", "Parking", "Document", "Shopping", "Medicine", "Place", "Gift Idea", "Wishlist", "Custom"
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val reminderDate: Long? = null,
    val priority: String = "Medium", // "Low", "Medium", "High"
    val status: String = "Active", // "Active", "Completed", "Archived", "Trash"
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String? = null,
    val voicePath: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val trashDate: Long? = null
)
