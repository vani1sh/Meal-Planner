package com.example.mealplanner.data.repository

import com.example.mealplanner.data.local.entity.CustomProductEntity
import com.example.mealplanner.data.local.entity.DiaryEntryEntity
import com.example.mealplanner.data.local.entity.ShoppingListItemEntity
import com.example.mealplanner.data.local.FoodDao
import com.example.mealplanner.data.local.entity.RecipeEntity
import com.example.mealplanner.data.remote.OpenFoodFactsApi
import com.example.mealplanner.data.remote.ProductDto
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.MealType
import com.example.mealplanner.domain.model.Product
import com.example.mealplanner.domain.model.RecipeIngredient
import com.example.mealplanner.domain.repository.FoodRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FoodRepositoryImpl(
    private val api: OpenFoodFactsApi,
    private val dao: FoodDao
) : FoodRepository {
    override suspend fun searchProducts(query: String, userId: String): Result<List<Product>> {
        return try {
            val localProducts = dao.searchCustomProducts(query, userId).map { it.toDomain() }

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

    // CustomProduct
    override suspend fun saveCustomProduct(product: Product, userId: String) {
        dao.deleteCustomDuplicates(product.name, product.brand, userId)

        val entity = CustomProductEntity(
            id = product.id,
            userId = userId,
            name = product.name,
            brand = product.brand,
            calories = product.calories,
            protein = product.protein,
            fat = product.fat,
            carbs = product.carbs
        )
        dao.insertCustomProduct(entity)
    }

    override suspend fun deleteCustomProduct(productId: String, userId: String) {
        dao.deleteCustomProductById(productId, userId)
    }

    override suspend fun updateCustomProduct(product: Product, userId: String) {
        val entity = CustomProductEntity(
            id = product.id,
            userId = userId,
            name = product.name,
            brand = product.brand,
            calories = product.calories,
            protein = product.protein,
            fat = product.fat,
            carbs = product.carbs
        )
        dao.updateCustomProduct(entity)
    }

    override fun getCustomProductsFlow(userId: String): Flow<List<Product>> {
        return dao.getAllCustomProductsFlow(userId).map { entities ->
            entities.map { it.toDomain() }
                .distinctBy {
                    val nameRaw = it.name.lowercase().replace("\\s+".toRegex(), "")
                    val brandRaw = it.brand?.lowercase()?.replace("\\s+".toRegex(), "") ?: ""
                    "$nameRaw-$brandRaw"
                }
        }
    }

    // Diary
    override fun getDiaryForDate(userId: String, startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntry>> {
        return dao.getDiaryEntriesForUser(userId, startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addDiaryEntry(entry: DiaryEntry, userId: String) {
        dao.insertDiaryEntry(entry.toEntity(userId))
    }

    override suspend fun deleteDiaryEntry(entryId: Int, userId: String) {
        dao.deleteDiaryEntry(entryId, userId)
    }

    override suspend fun updateDiaryEntryWeight(entryId: Int, newAmountGrams: Int, userId: String) {
        dao.updateDiaryEntryGrams(entryId, newAmountGrams, userId)
    }

    // Recipe
    override fun getRecipesFlow(userId: String): Flow<List<Product>> {
        return dao.getAllRecipesFlow(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveRecipe(product: Product, userId: String) {
        dao.deleteRecipeDuplicates(product.name, userId)
        val entity = RecipeEntity(
            id = product.id,
            userId = userId,
            name = product.name,
            calories = product.calories, protein = product.protein, fat = product.fat, carbs = product.carbs,
            ingredientsJson = product.recipeIngredientsJson ?: "[]"
        )
        dao.insertRecipe(entity)
    }

    override suspend fun deleteRecipe(recipeId: String, userId: String) {
        dao.deleteRecipeById(recipeId, userId)
    }

    override suspend fun getRecipeById(id: String, userId: String): Result<Product?> {
        return try { Result.success(dao.getRecipeById(id, userId)?.toDomain()) }
        catch (e: Exception) { Result.failure(e) }
    }

    // ShoppingList
    override fun getShoppingList(userId: String): Flow<List<ShoppingListItemEntity>> = dao.getShoppingList(userId)

    override suspend fun insertShoppingListItem(item: ShoppingListItemEntity) = dao.insertShoppingListItem(item)

    override suspend fun updateShoppingListItem(item: ShoppingListItemEntity) = dao.updateShoppingListItem(item)

    override suspend fun deleteShoppingListItem(item: ShoppingListItemEntity) = dao.deleteShoppingListItem(item)

    override suspend fun addProductToShoppingList(product: Product, amountGrams: Int, userId: String) {
        if (product.isRecipe && product.recipeIngredientsJson != null) {
            val type = object : TypeToken<List<RecipeIngredient>>() {}.type
            val ingredients: List<RecipeIngredient> = Gson().fromJson(product.recipeIngredientsJson, type)

            val ratio = amountGrams / 100f

            ingredients.forEach { ingredient ->
                val calculatedAmount = (ingredient.amountGrams * ratio).toInt()
                dao.insertShoppingListItem(
                    ShoppingListItemEntity(
                        userId = userId,
                        name = ingredient.product.name,
                        amountGrams = calculatedAmount,
                        note = "Рецепт ${product.name}"
                    )
                )
            }
        } else {
            dao.insertShoppingListItem(
                ShoppingListItemEntity(
                    userId = userId,
                    name = product.name,
                    amountGrams = amountGrams
                )
            )
        }
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

fun DiaryEntry.toEntity(userId: String) = DiaryEntryEntity(
    id = id,
    userId = userId,
    productId = product.id,
    isCustomProduct = product.isCustom,
    amountGrams = amountGrams,
    timestamp = timestamp,
    mealType = mealType.name,
    snapshotName = product.name,
    snapshotCalories = product.calories,
    snapshotProtein = product.protein,
    snapshotFat = product.fat,
    snapshotCarbs = product.carbs
)