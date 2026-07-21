package com.example.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryItem(
    val name: String, // Matches Memory.category (e.g. "Note", "Money", "Parking", etc.)
    val icon: ImageVector,
    val color: Color,
    val descriptionKey: String
)

object CategoryRegistry {
    val categories = listOf(
        CategoryItem(
            name = "Note",
            icon = Icons.Default.StickyNote2,
            color = Color(0xFF42A5F5), // Bright Blue
            descriptionKey = "Simple remember"
        ),
        CategoryItem(
            name = "Money",
            icon = Icons.Default.MonetizationOn,
            color = Color(0xFFFFA726), // Orange
            descriptionKey = "Rahul ki ₹500 ivvali"
        ),
        CategoryItem(
            name = "Parking",
            icon = Icons.Default.LocalParking,
            color = Color(0xFF66BB6A), // Emerald Green
            descriptionKey = "Car ekkada park chesanu?"
        ),
        CategoryItem(
            name = "Document",
            icon = Icons.Default.Description,
            color = Color(0xFFAB47BC), // Royal Purple
            descriptionKey = "Passport ekkada pettano?"
        ),
        CategoryItem(
            name = "Shopping",
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFF26C6DA), // Cyan
            descriptionKey = "Grocery items"
        ),
        CategoryItem(
            name = "Medicine",
            icon = Icons.Default.MedicalServices,
            color = Color(0xFFEF5350), // Red
            descriptionKey = "Doctor medicine teesukovali"
        ),
        CategoryItem(
            name = "Place",
            icon = Icons.Default.Place,
            color = Color(0xFFD4E157), // Lime
            descriptionKey = "Friend recommend chesina spot"
        ),
        CategoryItem(
            name = "Gift Idea",
            icon = Icons.Default.CardGiftcard,
            color = Color(0xFFEC407A), // Hot Pink
            descriptionKey = "Birthday gift idea"
        ),
        CategoryItem(
            name = "Wishlist",
            icon = Icons.Default.Star,
            color = Color(0xFF8D6E63), // Brown
            descriptionKey = "Wish items to buy later"
        )
    )

    fun getCategoryItem(name: String): CategoryItem {
        return categories.firstOrNull { it.name.lowercase() == name.lowercase() }
            ?: CategoryItem(name, Icons.Default.StickyNote2, Color(0xFF78909C), "Simple note")
    }
}
