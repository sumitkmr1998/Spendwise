package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#0D9488",
    val iconName: String = "category",
    val keywords: String = "", // Comma-separated matching keywords
    val isDefault: Boolean = false
) {
    val composeColor: Color
        get() {
            return try {
                val clean = colorHex.removePrefix("#")
                val colorInt = clean.toLong(16)
                if (clean.length == 6) {
                    Color(colorInt or 0x00000000FF000000)
                } else {
                    Color(colorInt)
                }
            } catch (_: Exception) {
                Color(0xFF0D9488)
            }
        }

    val keywordList: List<String>
        get() = keywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
}
