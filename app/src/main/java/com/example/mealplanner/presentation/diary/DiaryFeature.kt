package com.example.mealplanner.presentation.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.KeyboardType
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
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0); startOfDay.set(Calendar.SECOND, 0)
        val endOfDay = calendar.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23); endOfDay.set(Calendar.MINUTE, 59); endOfDay.set(Calendar.SECOND, 59)

        diaryJob?.cancel()
        diaryJob = viewModelScope.launch {
            repository.getDiaryForDate(startOfDay.timeInMillis, endOfDay.timeInMillis).collect { entries ->
                _uiState.update { currentState ->
                    currentState.copy(
                        entries = entries,
                        totalCalories = entries.sumOf { it.consumedCalories.toDouble() }.toFloat(),
                        totalProtein = entries.sumOf { it.consumedProtein.toDouble() }.toFloat(),
                        totalFat = entries.sumOf { it.consumedFat.toDouble() }.toFloat(),
                        totalCarbs = entries.sumOf { it.consumedCarbs.toDouble() }.toFloat(),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteEntry(entryId: Int) = viewModelScope.launch { repository.deleteDiaryEntry(entryId) }
    fun updateEntryWeight(entryId: Int, newWeight: Int) = viewModelScope.launch { repository.updateDiaryEntryWeight(entryId, newWeight) }

    fun setDate(timestamp: Long) {
        val newDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        _uiState.update { it.copy(currentDate = newDate, isLoading = true) }
        loadDiaryForDate(newDate)
    }
}

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onNavigateToAddProduct: (Long, MealType) -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val expandedStates = remember {
        mutableStateMapOf<MealType, Boolean>().apply {
            MealType.entries.forEach { put(it, false) }
        }
    }

    var expandedMenuFor by remember { mutableStateOf<Int?>(null) }
    var entryToEdit by remember { mutableStateOf<DiaryEntry?>(null) }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            DateSelector(
                currentDate = state.currentDate,
                onPreviousDay = { viewModel.changeDate(-1) },
                onNextDay = { viewModel.changeDate(1) },
                onDateClick = onNavigateToCalendar
            )

            MacrosSummaryCard(state)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                MealType.entries.forEach { type ->
                    val mealEntries = state.entries.filter { it.mealType == type }

                    val mealCals = mealEntries.sumOf { it.consumedCalories.toDouble() }.toInt()
                    val mealProt = mealEntries.sumOf { it.consumedProtein.toDouble() }.toInt()
                    val mealFat = mealEntries.sumOf { it.consumedFat.toDouble() }.toInt()
                    val mealCarb = mealEntries.sumOf { it.consumedCarbs.toDouble() }.toInt()

                    item(key = type.name) {
                        MealHeader(
                            type = type,
                            totalCalories = mealCals,
                            totalProtein = mealProt,
                            totalFat = mealFat,
                            totalCarbs = mealCarb,
                            isExpanded = expandedStates[type] ?: false,
                            onToggle = { expandedStates[type] = !(expandedStates[type] ?: false) },
                            onAddClick = { onNavigateToAddProduct(state.currentDate.timeInMillis, type) }
                        )
                    }

                    item {
                        AnimatedVisibility(visible = expandedStates[type] ?: false) {
                            Column {
                                if (mealEntries.isEmpty()) {
                                    Text(
                                        "Нет записей",
                                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    mealEntries.forEach { entry ->
                                        DiaryEntryItem(
                                            entry = entry,
                                            isMenuExpanded = expandedMenuFor == entry.id,
                                            onMenuClick = { expandedMenuFor = entry.id },
                                            onDismissMenu = { expandedMenuFor = null },
                                            onEdit = {
                                                expandedMenuFor = null
                                                entryToEdit = entry
                                            },
                                            onDelete = {
                                                expandedMenuFor = null
                                                viewModel.deleteEntry(entry.id)
                                            }
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    entryToEdit?.let { entry ->
        EditEntryWeightDialog(
            entry = entry,
            onDismiss = { entryToEdit = null },
            onConfirm = { newWeight ->
                viewModel.updateEntryWeight(entry.id, newWeight)
                entryToEdit = null
            }
        )
    }
}

@Composable
fun DiaryEntryItem(
    entry: DiaryEntry,
    isMenuExpanded: Boolean,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.padding(start = 16.dp),
        headlineContent = { Text(entry.product.name, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            Text(
                text = "${entry.amountGrams} г:  Б ${entry.consumedProtein.toInt()}  Ж ${entry.consumedFat.toInt()}  У ${entry.consumedCarbs.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entry.consumedCalories.toInt()} ккал",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = onDismissMenu) {
                        DropdownMenuItem(text = { Text("Изменить вес") }, onClick = onEdit)
                        DropdownMenuItem(text = { Text("Удалить", color = MaterialTheme.colorScheme.error) }, onClick = onDelete)
                    }
                }
            }
        }
    )
}

@Composable
fun EditEntryWeightDialog(
    entry: DiaryEntry,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var weightInput by remember { mutableStateOf(entry.amountGrams.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить вес") },
        text = {
            Column {
                Text("Продукт: ${entry.product.name}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) weightInput = newValue
                    },
                    label = { Text("Вес (в граммах)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val grams = weightInput.toIntOrNull() ?: 0
                if (grams > 0) onConfirm(grams)
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun MealHeader(
    type: MealType,
    totalCalories: Int,
    totalProtein: Int,
    totalFat: Int,
    totalCarbs: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddClick: () -> Unit
) {
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp, 12.dp, 8.dp, 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotationState),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(type.displayName, style = MaterialTheme.typography.titleMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$totalCalories ккал",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onAddClick) {
                        Text("+", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (!isExpanded && totalCalories > 0) {
                Text(
                    text = "Б $totalProtein  Ж $totalFat  У $totalCarbs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun DateSelector(
    currentDate: Calendar,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateClick: () -> Unit
) {
    val dateString = remember(currentDate) {
        val today = Calendar.getInstance()
        val tomorrow = today.clone() as Calendar; tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        val yesterday = today.clone() as Calendar; yesterday.add(Calendar.DAY_OF_YEAR, -1)

        fun isSameDay(cal1: Calendar, cal2: Calendar) =
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

        when {
            isSameDay(currentDate, today) -> "Сегодня"
            isSameDay(currentDate, tomorrow) -> "Завтра"
            isSameDay(currentDate, yesterday) -> "Вчера"
            else -> {
                val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
                dateFormat.format(currentDate.time)
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Предыдущий день") }

        Text(
            text = dateString,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { onDateClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        IconButton(onClick = onNextDay) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Следующий день") }
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