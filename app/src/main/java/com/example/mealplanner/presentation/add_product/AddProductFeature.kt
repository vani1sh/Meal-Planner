package com.example.mealplanner.presentation.add_product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.MealType
import com.example.mealplanner.domain.model.Product
import com.example.mealplanner.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val repository: FoodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val selectedTimestamp: Long = savedStateHandle.get<Long>("timestamp") ?: System.currentTimeMillis()
    private val selectedMealType: String = savedStateHandle.get<String>("mealType") ?: MealType.SNACK.name

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()

    fun search(query: String) {
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val result = repository.searchProducts(query)
            if (result.isSuccess) {
                _searchResults.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun addToDiary(product: Product, amountGrams: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addDiaryEntry(
                DiaryEntry(
                    product = product,
                    amountGrams = amountGrams,
                    timestamp = selectedTimestamp,
                    mealType = MealType.valueOf(selectedMealType)
                )
            )
            onSuccess()
        }
    }
}

@Composable
fun AddProductScreen(viewModel: AddProductViewModel, onBack: () -> Unit) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it)
            },
            label = { Text("Поиск продукта (API + Свои)") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true
        )

        LazyColumn {
            items(searchResults) { product ->
                ListItem(
                    modifier = Modifier.clickable {
                        selectedProduct = product
                    },
                    headlineContent = { Text(product.name) },
                    supportingContent = { Text(product.brand ?: "Неизвестный бренд") },
                    trailingContent = { Text("${product.calories.toInt()} ккал / 100г") }
                )
                HorizontalDivider()
            }
        }
    }

    selectedProduct?.let { product ->
        WeightInputDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onConfirm = { grams ->
                viewModel.addToDiary(product, grams, onSuccess = {
                    selectedProduct = null
                    onBack()
                })
            }
        )
    }
}

@Composable
fun WeightInputDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var weightInput by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Укажите вес порции") },
        text = {
            Column {
                Text("Продукт: ${product.name}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            weightInput = newValue
                        }
                    },
                    label = { Text("Вес (в граммах)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                val currentWeight = weightInput.toIntOrNull() ?: 0
                if (currentWeight > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val currentCals = (product.calories * currentWeight) / 100
                    Text("Будет добавлено: ${currentCals.toInt()} ккал", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grams = weightInput.toIntOrNull() ?: 0
                    if (grams > 0) {
                        onConfirm(grams)
                    }
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}