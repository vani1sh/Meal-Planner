package com.example.mealplanner.presentation.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject


data class DiaryUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalFat: Float = 0f,
    val totalCarbs: Float = 0f,
    val isLoading: Boolean = true
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    init {
        loadTodayDiary()
    }

    private fun loadTodayDiary() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        viewModelScope.launch {
            repository.getDiaryForDate(startOfDay, endOfDay).collect { entries ->
                _uiState.value = DiaryUiState(
                    entries = entries,
                    totalCalories = entries.map { it.consumedCalories }.sum(),
                    totalProtein = entries.map { it.consumedProtein }.sum(),
                    totalFat = entries.map { it.consumedFat }.sum(),
                    totalCarbs = entries.map { it.consumedCarbs }.sum(),
                    isLoading = false
                )
            }
        }
    }
}

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onNavigateToAddProduct: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddProduct) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            MacrosSummaryCard(state)

            Spacer(modifier = Modifier.height(16.dp))

            if (state.entries.isEmpty() && !state.isLoading) {
                Text(
                    text = "Дневник пуст. Добавьте первый прием пищи!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(state.entries) { entry ->
                        DiaryEntryItem(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun MacrosSummaryCard(state: DiaryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Итого за сегодня", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Калории: ${state.totalCalories.toInt()} ккал", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Белки: ${state.totalProtein.toInt()} г")
                Text("Жиры: ${state.totalFat.toInt()} г")
                Text("Углеводы: ${state.totalCarbs.toInt()} г")
            }
        }
    }
}

@Composable
fun DiaryEntryItem(entry: DiaryEntry) {
    ListItem(
        headlineContent = { Text(entry.product.name) },
        supportingContent = { Text("${entry.amountGrams} г") },
        trailingContent = {
            Text(
                "${entry.consumedCalories.toInt()} ккал",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
    Divider()
}