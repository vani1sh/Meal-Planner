package com.example.mealplanner.presentation.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.MealType
import com.example.mealplanner.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject


data class DiaryUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalFat: Float = 0f,
    val totalCarbs: Float = 0f,
    val isLoading: Boolean = true,
    val currentDate: Calendar = Calendar.getInstance()
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    private var diaryJob: kotlinx.coroutines.Job? = null

    init {
        loadDiaryForDate(_uiState.value.currentDate)
    }

    fun changeDate(daysOffset: Int) {
        val newDate = _uiState.value.currentDate.clone() as Calendar
        newDate.add(Calendar.DAY_OF_YEAR, daysOffset)

        _uiState.update { it.copy(currentDate = newDate, isLoading = true) }
        loadDiaryForDate(newDate)
    }

    private fun loadDiaryForDate(calendar: Calendar) {
        val startOfDay = calendar.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val endOfDay = calendar.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        endOfDay.set(Calendar.SECOND, 59)
        endOfDay.set(Calendar.MILLISECOND, 999)

        diaryJob?.cancel()

        diaryJob = viewModelScope.launch {
            repository.getDiaryForDate(startOfDay.timeInMillis, endOfDay.timeInMillis).collect { entries ->
                _uiState.update { currentState ->
                    currentState.copy(
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
}

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onNavigateToAddProduct: (Long, MealType) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            DateSelector(
                currentDate = state.currentDate,
                onPreviousDay = { viewModel.changeDate(-1) },
                onNextDay = { viewModel.changeDate(1) }
            )

            MacrosSummaryCard(state)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                MealType.entries.forEach { type ->
                    item {
                        MealHeader(
                            type = type,
                            onAddClick = {
                                onNavigateToAddProduct(state.currentDate.timeInMillis, type)
                            }
                        )
                    }

                    val mealEntries = state.entries.filter { it.mealType == type }
                    if (mealEntries.isEmpty()) {
                        item {
                            Text(
                                "Нет записей",
                                modifier = Modifier.padding(16.dp, 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        items(mealEntries) { entry ->
                            DiaryEntryItem(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealHeader(type: MealType, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp, 8.dp, 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(type.displayName, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAddClick) {
            Text("+ Добавить")
        }
    }
}

@Composable
fun DateSelector(
    currentDate: Calendar,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(currentDate.time)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущий день")
        }

        Text(
            text = dateString,
            style = MaterialTheme.typography.titleMedium
        )

        IconButton(onClick = onNextDay) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующий день")
        }
    }
}

@Composable
fun MacrosSummaryCard(state: DiaryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Итого за день", style = MaterialTheme.typography.titleLarge)
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
    HorizontalDivider()
}