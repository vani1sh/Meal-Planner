package com.example.mealplanner.domain.repository

import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.Product
import kotlinx.coroutines.flow.Flow


interface FoodRepository {
    suspend fun searchProducts(query: String): Result<List<Product>>

    suspend fun saveCustomProduct(product: Product)

    suspend fun deleteCustomProduct(productId: String)
    suspend fun updateCustomProduct(product: Product)

    fun getDiaryForDate(startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntry>>

    suspend fun addDiaryEntry(entry: DiaryEntry)

    suspend fun deleteDiaryEntry(entryId: Int)
    suspend fun updateDiaryEntryWeight(entryId: Int, newAmountGrams: Int)

    fun getCustomProductsFlow(): Flow<List<Product>>

    fun getRecipesFlow(): Flow<List<Product>>
    suspend fun saveRecipe(product: Product)
    suspend fun deleteRecipe(recipeId: String)

    suspend fun getRecipeById(id: String): Result<Product?>
}