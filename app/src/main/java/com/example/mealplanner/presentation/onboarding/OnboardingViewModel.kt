package com.example.mealplanner.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.domain.repository.AuthRepository
import com.example.mealplanner.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val gender: Gender = Gender.MALE,
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val resultGoal: UserGoal? = null
)

enum class Gender { MALE, FEMALE }
enum class ActivityLevel(val multiplier: Float, val title: String) {
    SEDENTARY(1.2f, "Низкая (сидячий образ жизни)"),
    LIGHT(1.375f, "Слабая (тренировки 1-3 раза в неделю)"),
    MODERATE(1.55f, "Средняя (тренировки 3-5 раз в неделю)"),
    ACTIVE(1.725f, "Высокая (тренировки 6-7 раз в неделю)")
}

data class UserGoal(val tdee: Int, val protein: Int, val fat: Int, val carbs: Int)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun updateField(update: (OnboardingState) -> OnboardingState) {
        _state.value = update(_state.value)
    }

    fun calculateMacros() {
        val s = _state.value
        val w = s.weight.toFloatOrNull() ?: return
        val h = s.height.toFloatOrNull() ?: return
        val a = s.age.toIntOrNull() ?: return

        var bmr = (10f * w) + (6.25f * h) - (5f * a)
        bmr += if (s.gender == Gender.MALE) 5f else -161f

        val tdee = bmr * s.activityLevel.multiplier

        val fat = w * 1.0f
        val proteinMultiplier = if (s.activityLevel.multiplier >= 1.55f) 1.8f else 1.2f
        val protein = w * proteinMultiplier

        val fatCals = fat * 9f
        val proteinCals = protein * 4f
        val carbsCals = tdee - fatCals - proteinCals
        val carbs = (carbsCals / 4f).coerceAtLeast(0f)

        val goal = UserGoal(
            tdee = tdee.toInt(),
            protein = protein.toInt(),
            fat = fat.toInt(),
            carbs = carbs.toInt()
        )

        _state.value = s.copy(resultGoal = goal)

        saveGoalToDatabase(goal)
    }

    private fun saveGoalToDatabase(goal: UserGoal) {
        viewModelScope.launch {
            val userId = authRepository.currentUser.firstOrNull()
            if (userId != null) {
                userPrefsRepository.saveUserGoal(userId, goal)
            }
        }
    }
}