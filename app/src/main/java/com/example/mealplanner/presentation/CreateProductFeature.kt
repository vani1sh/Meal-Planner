package com.example.mealplanner.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.MealType
import com.example.mealplanner.domain.model.Product
import com.example.mealplanner.domain.repository.AuthRepository
import com.example.mealplanner.domain.repository.FoodRepository
import com.example.mealplanner.presentation.components.WeightInputDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class CreateProductViewModel @Inject constructor(
    private val repository: FoodRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val selectedTimestamp: Long = savedStateHandle.get<Long>("timestamp") ?: System.currentTimeMillis()
    private val selectedMealType: String = savedStateHandle.get<String>("mealType") ?: MealType.SNACK.name

    fun saveProductAndAddToDiary(
        name: String, brand: String, calories: String, protein: String, fat: String, carbs: String,
        amountGrams: Int, addToShoppingList: Boolean, onSuccess: () -> Unit
    ) {
        if (name.isBlank()) return

        val newProduct = Product(
            id = UUID.randomUUID().toString(),
            name = name,
            brand = brand.ifBlank { null },
            calories = calories.toFloatOrNull() ?: 0f,
            protein = protein.toFloatOrNull() ?: 0f,
            fat = fat.toFloatOrNull() ?: 0f,
            carbs = carbs.toFloatOrNull() ?: 0f,
            isCustom = true
        )

        viewModelScope.launch {
            val userId = authRepository.currentUser.firstOrNull() ?: return@launch

            repository.saveCustomProduct(newProduct, userId)

            repository.addDiaryEntry(
                DiaryEntry(
                    product = newProduct,
                    amountGrams = amountGrams,
                    timestamp = selectedTimestamp,
                    mealType = MealType.valueOf(selectedMealType)
                ),
                userId = userId
            )

            if (addToShoppingList) {
                val userId = authRepository.currentUser.firstOrNull() ?: return@launch

                repository.addProductToShoppingList(newProduct, amountGrams, userId)
            }

            onSuccess()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductScreen(
    viewModel: CreateProductViewModel,
    onBack: () -> Unit,
    onNavigateBackToDiary: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    var showWeightDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создать продукт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название (обязательно)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Бренд (опционально)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                singleLine = true
            )

            Text(
                "Укажите пищевую ценность на 100 грамм продукта",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = calories,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) calories = it },
                    label = { Text("Ккал") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) protein = it },
                    label = { Text("Белки, г") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) fat = it },
                    label = { Text("Жиры, г") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) carbs = it },
                    label = { Text("Углев., г") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showWeightDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Далее")
            }
        }
    }

    if (showWeightDialog) {
        val tempProduct = Product(
            id = "", name = name, brand = brand.ifBlank { null },
            calories = calories.toFloatOrNull() ?: 0f, protein = protein.toFloatOrNull() ?: 0f,
            fat = fat.toFloatOrNull() ?: 0f, carbs = carbs.toFloatOrNull() ?: 0f, isCustom = true
        )

        WeightInputDialog(
            product = tempProduct,
            selectedDateTimestamp = viewModel.selectedTimestamp,
            onDismiss = { showWeightDialog = false },
            onConfirm = { grams, addToShoppingList ->
                showWeightDialog = false
                viewModel.saveProductAndAddToDiary(
                    name = name, brand = brand, calories = calories,
                    protein = protein, fat = fat, carbs = carbs,
                    amountGrams = grams,
                    addToShoppingList = addToShoppingList,
                    onSuccess = onNavigateBackToDiary
                )
            }
        )
    }
}