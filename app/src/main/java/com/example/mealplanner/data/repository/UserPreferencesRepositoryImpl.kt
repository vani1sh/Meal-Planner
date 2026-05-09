package com.example.mealplanner.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.mealplanner.domain.repository.UserPreferencesRepository
import com.example.mealplanner.presentation.onboarding.UserGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private fun getTdeeKey(userId: String) = intPreferencesKey("tdee_$userId")
    private fun getProteinKey(userId: String) = intPreferencesKey("protein_$userId")
    private fun getFatKey(userId: String) = intPreferencesKey("fat_$userId")
    private fun getCarbsKey(userId: String) = intPreferencesKey("carbs_$userId")

    override fun getUserGoal(userId: String): Flow<UserGoal?> {
        return dataStore.data.map { preferences ->
            val tdee = preferences[getTdeeKey(userId)] ?: return@map null
            val protein = preferences[getProteinKey(userId)] ?: return@map null
            val fat = preferences[getFatKey(userId)] ?: return@map null
            val carbs = preferences[getCarbsKey(userId)] ?: return@map null

            UserGoal(tdee, protein, fat, carbs)
        }
    }

    override suspend fun saveUserGoal(userId: String, goal: UserGoal) {
        dataStore.edit { preferences ->
            preferences[getTdeeKey(userId)] = goal.tdee
            preferences[getProteinKey(userId)] = goal.protein
            preferences[getFatKey(userId)] = goal.fat
            preferences[getCarbsKey(userId)] = goal.carbs
        }
    }
}