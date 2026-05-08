package com.example.mealplanner.data.local

import androidx.room.*
import com.example.mealplanner.data.local.entity.CustomProductEntity
import com.example.mealplanner.data.local.entity.DiaryEntryEntity
import com.example.mealplanner.data.local.entity.RecipeEntity
import com.example.mealplanner.data.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomProduct(product: CustomProductEntity)

    @Query("DELETE FROM custom_products WHERE LOWER(name) = LOWER(:name) AND (LOWER(brand) = LOWER(:brand) OR (brand IS NULL AND :brand IS NULL))")
    suspend fun deleteDuplicates(name: String, brand: String?)

    @Query("SELECT * FROM custom_products ORDER BY name ASC")
    fun getAllCustomProductsFlow(): Flow<List<CustomProductEntity>>

    @Query("SELECT * FROM custom_products WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'")
    suspend fun searchCustomProducts(query: String): List<CustomProductEntity>

    @Query("DELETE FROM custom_products WHERE id = :productId")
    suspend fun deleteCustomProductById(productId: String)

    @Update
    suspend fun updateCustomProduct(product: CustomProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntryEntity)

    @Query("SELECT * FROM diary_entries WHERE timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp DESC")
    fun getDiaryEntries(startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntryEntity>>

    @Query("DELETE FROM diary_entries WHERE id = :entryId")
    suspend fun deleteDiaryEntryById(entryId: Int)

    @Query("UPDATE diary_entries SET amountGrams = :newAmount WHERE id = :entryId")
    suspend fun updateDiaryEntryGrams(entryId: Int, newAmount: Int)

    @Query("SELECT * FROM custom_products ORDER BY name ASC")
    suspend fun getAllCustomProducts(): List<CustomProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE LOWER(name) = LOWER(:name)")
    suspend fun deleteRecipeDuplicates(name: String)

    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipesFlow(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :query || '%'")
    suspend fun searchRecipes(query: String): List<RecipeEntity>

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: String)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: String): RecipeEntity?

    @Query("SELECT * FROM shopping_list")
    fun getShoppingList(): Flow<List<ShoppingListItemEntity>>

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
    version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}