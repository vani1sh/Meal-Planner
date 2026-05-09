package com.example.mealplanner.data.local

import androidx.room.*
import com.example.mealplanner.data.local.entity.CustomProductEntity
import com.example.mealplanner.data.local.entity.DiaryEntryEntity
import com.example.mealplanner.data.local.entity.RecipeEntity
import com.example.mealplanner.data.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface FoodDao {

    // CustomProduct
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomProduct(product: CustomProductEntity)

    @Query("DELETE FROM custom_products WHERE LOWER(name) = LOWER(:name) AND (LOWER(brand) = LOWER(:brand) OR (brand IS NULL AND :brand IS NULL)) AND userId = :userId")
    suspend fun deleteCustomDuplicates(name: String, brand: String?, userId: String)

    @Query("SELECT * FROM custom_products WHERE userId = :userId ORDER BY name ASC")
    fun getAllCustomProductsFlow(userId: String): Flow<List<CustomProductEntity>>

       @Query("SELECT * FROM custom_products WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%')")
    suspend fun searchCustomProducts(query: String, userId: String): List<CustomProductEntity>

    @Query("DELETE FROM custom_products WHERE id = :productId AND userId = :userId")
    suspend fun deleteCustomProductById(productId: String, userId: String)

    @Update
    suspend fun updateCustomProduct(product: CustomProductEntity)

    @Query("SELECT * FROM custom_products WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllCustomProducts(userId: String): List<CustomProductEntity>

    // Diary
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntryEntity)

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp DESC")
    fun getDiaryEntriesForUser(userId: String, startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntryEntity>>

    @Query("DELETE FROM diary_entries WHERE id = :entryId AND userId = :userId")
    suspend fun deleteDiaryEntry(entryId: Int, userId: String)

    @Query("UPDATE diary_entries SET amountGrams = :newAmount WHERE id = :entryId AND userId = :userId")
    suspend fun updateDiaryEntryGrams(entryId: Int, newAmount: Int, userId: String)

    // Recipe
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE LOWER(name) = LOWER(:name) AND userId = :userId")
    suspend fun deleteRecipeDuplicates(name: String, userId: String)

    @Query("SELECT * FROM recipes WHERE userId = :userId ORDER BY name ASC")
    fun getAllRecipesFlow(userId: String): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE userId = :userId AND name LIKE '%' || :query || '%'")
    suspend fun searchRecipes(query: String, userId: String): List<RecipeEntity>

    @Query("DELETE FROM recipes WHERE id = :recipeId AND userId = :userId")
    suspend fun deleteRecipeById(recipeId: String, userId: String)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getRecipeById(id: String, userId: String): RecipeEntity?

    // ShoppingList
    @Query("SELECT * FROM shopping_list WHERE userId = :userId")
    fun getShoppingList(userId: String): Flow<List<ShoppingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItem(item: ShoppingListItemEntity)

    @Update
    suspend fun updateShoppingListItem(item: ShoppingListItemEntity)

    @Delete
    suspend fun deleteShoppingListItem(item: ShoppingListItemEntity)
}


@Database(entities = [
    CustomProductEntity::class,
    DiaryEntryEntity::class,
    RecipeEntity::class,
    ShoppingListItemEntity::class
    ],
    version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}