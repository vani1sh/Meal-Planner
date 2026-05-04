package com.example.mealplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "custom_products")
data class CustomProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val calories: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float
)