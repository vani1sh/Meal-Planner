package com.example.mealplanner.presentation.components

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


@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute = _startRoute.asStateFlow()

    fun checkAuthState() {
        viewModelScope.launch {
            val userId = authRepository.currentUser.firstOrNull()

            if (userId == null) {
                _startRoute.value = "auth"
            } else {
                val goal = userPrefsRepository.getUserGoal(userId).firstOrNull()
                if (goal == null) {
                    _startRoute.value = "onboarding"
                } else {
                    _startRoute.value = "diary"
                }
            }
        }
    }
}