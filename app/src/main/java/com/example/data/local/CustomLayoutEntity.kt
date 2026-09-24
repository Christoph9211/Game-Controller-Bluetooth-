package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a custom saved controller layout configuration.
 * Allows users to persist, manage, and switch between multiple button/stick configurations.
 */
@Entity(tableName = "custom_controller_layouts")
data class CustomLayoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val isPreset: Boolean = false,
    val elementCount: Int = 0,
    val elementsJson: String,
    val lastModified: Long = System.currentTimeMillis()
)
