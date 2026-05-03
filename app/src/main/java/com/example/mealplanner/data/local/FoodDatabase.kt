package com.example.mealplanner.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow


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

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomProduct(product: CustomProductEntity)

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
}

@Database(entities = [CustomProductEntity::class, DiaryEntryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}