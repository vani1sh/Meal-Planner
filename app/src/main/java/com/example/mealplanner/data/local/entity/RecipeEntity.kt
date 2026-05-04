package com.example.mealplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mealplanner.domain.model.Product


@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val calories: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val ingredientsJson: String = "[]"
) {
    fun toDomain() = Product(
        id = id,
        name = name,
        brand = "Рецепт",
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        isCustom = true,
        isRecipe = true,
        recipeIngredientsJson = ingredientsJson
    )
}