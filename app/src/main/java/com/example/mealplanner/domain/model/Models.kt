package com.example.mealplanner.domain.model


data class Product(
    val id: String,
    val name: String,
    val brand: String?,
    val calories: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val isCustom: Boolean
)

data class DiaryEntry(
    val id: Int = 0,
    val product: Product,
    val amountGrams: Int,
    val timestamp: Long
) {
    val consumedCalories: Float get() = (product.calories * amountGrams) / 100f
    val consumedProtein: Float get() = (product.protein * amountGrams) / 100f
    val consumedFat: Float get() = (product.fat * amountGrams) / 100f
    val consumedCarbs: Float get() = (product.carbs * amountGrams) / 100f
}