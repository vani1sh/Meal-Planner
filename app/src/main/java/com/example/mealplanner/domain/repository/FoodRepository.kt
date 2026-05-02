package com.example.mealplanner.domain.repository

import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.Product
import kotlinx.coroutines.flow.Flow


interface FoodRepository {
    suspend fun searchProducts(query: String): Result<List<Product>>

    suspend fun saveCustomProduct(product: Product)

    fun getDiaryForDate(startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntry>>

    suspend fun addDiaryEntry(entry: DiaryEntry)
}