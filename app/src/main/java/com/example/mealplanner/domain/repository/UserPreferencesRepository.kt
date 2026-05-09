package com.example.mealplanner.domain.repository

import com.example.mealplanner.presentation.onboarding.UserGoal
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getUserGoal(userId: String): Flow<UserGoal?>
    suspend fun saveUserGoal(userId: String, goal: UserGoal)
}