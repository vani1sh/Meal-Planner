package com.example.mealplanner.data.repository

import com.example.mealplanner.data.local.CustomProductEntity
import com.example.mealplanner.data.local.DiaryEntryEntity
import com.example.mealplanner.data.local.FoodDao
import com.example.mealplanner.data.remote.OpenFoodFactsApi
import com.example.mealplanner.data.remote.ProductDto
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.MealType
import com.example.mealplanner.domain.model.Product
import com.example.mealplanner.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FoodRepositoryImpl(
    private val api: OpenFoodFactsApi,
    private val dao: FoodDao
) : FoodRepository {

    override suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val localProducts = dao.searchCustomProducts(query).map { it.toDomain() }

            val remoteProducts = api.searchProducts(query).products.mapNotNull { it.toDomain() }

            val combinedAndDeduplicated = (localProducts + remoteProducts).distinctBy {
                val nameRaw = it.name.lowercase().replace("\\s+".toRegex(), "")
                val brandRaw = it.brand?.lowercase()?.replace("\\s+".toRegex(), "") ?: ""
                "$nameRaw-$brandRaw"
            }

            Result.success(combinedAndDeduplicated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveCustomProduct(product: Product) {
        val entity = CustomProductEntity(
            id = UUID.randomUUID().toString(),
            name = product.name,
            brand = product.brand,
            calories = product.calories,
            protein = product.protein,
            fat = product.fat,
            carbs = product.carbs
        )
        dao.insertCustomProduct(entity)
    }

    override fun getDiaryForDate(startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntry>> {
        return dao.getDiaryEntries(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addDiaryEntry(entry: DiaryEntry) {
        val entity = DiaryEntryEntity(
            productId = entry.product.id,
            isCustomProduct = entry.product.isCustom,
            amountGrams = entry.amountGrams,
            timestamp = entry.timestamp,
            mealType = entry.mealType.name,
            snapshotName = entry.product.name,
            snapshotCalories = entry.product.calories,
            snapshotProtein = entry.product.protein,
            snapshotFat = entry.product.fat,
            snapshotCarbs = entry.product.carbs
        )
        dao.insertDiaryEntry(entity)
    }

    override suspend fun deleteCustomProduct(productId: String) {
        dao.deleteCustomProductById(productId)
    }

    override suspend fun updateCustomProduct(product: Product) {
        val entity = CustomProductEntity(
            id = product.id,
            name = product.name,
            brand = product.brand,
            calories = product.calories,
            protein = product.protein,
            fat = product.fat,
            carbs = product.carbs
        )
        dao.updateCustomProduct(entity)
    }
    override suspend fun deleteDiaryEntry(entryId: Int) {
        dao.deleteDiaryEntryById(entryId)
    }

    override suspend fun updateDiaryEntryWeight(entryId: Int, newAmountGrams: Int) {
        dao.updateDiaryEntryGrams(entryId, newAmountGrams)
    }
}

fun ProductDto.toDomain(): Product? {
    if (productName.isNullOrBlank()) return null
    return Product(
        id = id,
        name = productName,
        brand = brands,
        calories = nutriments?.calories ?: 0f,
        protein = nutriments?.proteins ?: 0f,
        fat = nutriments?.fat ?: 0f,
        carbs = nutriments?.carbs ?: 0f,
        isCustom = false
    )
}

fun CustomProductEntity.toDomain() = Product(
    id = id, name = name, brand = brand, calories = calories,
    protein = protein, fat = fat, carbs = carbs, isCustom = true
)

fun DiaryEntryEntity.toDomain() = DiaryEntry(
    id = id,
    amountGrams = amountGrams,
    timestamp = timestamp,
    mealType = MealType.valueOf(mealType),
    product = Product(
        id = productId, name = snapshotName, brand = null,
        calories = snapshotCalories, protein = snapshotProtein,
        fat = snapshotFat, carbs = snapshotCarbs, isCustom = isCustomProduct
    )
)