package com.example.mealplanner.domain.repository

import com.example.mealplanner.data.local.entity.ShoppingListItemEntity
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.Product
import kotlinx.coroutines.flow.Flow


interface FoodRepository {
    suspend fun searchProducts(query: String, userId: String): Result<List<Product>>

    // CustomProduct
    suspend fun saveCustomProduct(product: Product, userId: String)
    suspend fun deleteCustomProduct(productId: String, userId: String)
    suspend fun updateCustomProduct(product: Product, userId: String)
    fun getCustomProductsFlow(userId: String): Flow<List<Product>>

    // Diary
    fun getDiaryForDate(userId: String, startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntry>>
    suspend fun addDiaryEntry(entry: DiaryEntry, userId: String)
    suspend fun deleteDiaryEntry(entryId: Int, userId: String)
    suspend fun updateDiaryEntryWeight(entryId: Int, newAmountGrams: Int, userId: String)

    // Recipe
    fun getRecipesFlow(userId: String): Flow<List<Product>>
    suspend fun saveRecipe(product: Product, userId: String)
    suspend fun deleteRecipe(recipeId: String, userId: String)
    suspend fun getRecipeById(id: String, userId: String): Result<Product?>

    // ShoppingList
    fun getShoppingList(userId: String): Flow<List<ShoppingListItemEntity>>
    suspend fun insertShoppingListItem(item: ShoppingListItemEntity)
    suspend fun updateShoppingListItem(item: ShoppingListItemEntity)
    suspend fun deleteShoppingListItem(item: ShoppingListItemEntity)
    suspend fun addProductToShoppingList(product: Product, amountGrams: Int, userId: String)
}