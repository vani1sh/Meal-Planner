package com.example.mealplanner.domain.model


data class Product(
    val id: String,
    val name: String,
    val brand: String?,
    val calories: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val isCustom: Boolean,
    val isRecipe: Boolean = false,
    val recipeIngredientsJson: String? = null
)