package com.example.mealplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: String,
    val isCustomProduct: Boolean,
    val amountGrams: Int,
    val timestamp: Long,
    val mealType: String,
    val snapshotName: String,
    val snapshotCalories: Float,
    val snapshotProtein: Float,
    val snapshotFat: Float,
    val snapshotCarbs: Float
)