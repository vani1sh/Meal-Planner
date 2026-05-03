package com.example.mealplanner.presentation.add_product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.mealplanner.presentation.components.WeightInputDialog
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

    private val _currentQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()

    fun search(query: String) {
        _currentQuery.value = query
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

    fun deleteCustomProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteCustomProduct(productId)
            search(_currentQuery.value)
        }
    }

    fun updateCustomProductMacros(product: Product, cals: Float, p: Float, f: Float, c: Float) {
        viewModelScope.launch {
            val updatedProduct = product.copy(calories = cals, protein = p, fat = f, carbs = c)
            repository.updateCustomProduct(updatedProduct)
            search(_currentQuery.value)
        }
    }
}

@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel,
    onBack: () -> Unit,
    onNavigateToCreateProduct: () -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    var selectedProductForDiary by remember { mutableStateOf<Product?>(null) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }

    var expandedMenuFor by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it)
                },
                label = { Text("Поиск продукта") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(onClick = onNavigateToCreateProduct) {
                Text("+ Свой")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(searchResults) { product ->
                ListItem(
                    modifier = Modifier.clickable { selectedProductForDiary = product },
                    headlineContent = { Text(product.name) },
                    supportingContent = {
                        Text(
                            text = product.brand ?: (if (product.isCustom) "Ваш продукт" else "Неизвестный бренд"),
                            color = if (product.isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${product.calories.toInt()} ккал", modifier = Modifier.padding(end = 8.dp))

                            if (product.isCustom) {
                                Box {
                                    IconButton(onClick = { expandedMenuFor = product.id }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                                    }
                                    DropdownMenu(
                                        expanded = expandedMenuFor == product.id,
                                        onDismissRequest = { expandedMenuFor = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Изменить КБЖУ") },
                                            onClick = {
                                                expandedMenuFor = null
                                                selectedProductForEdit = product
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                expandedMenuFor = null
                                                viewModel.deleteCustomProduct(product.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    selectedProductForDiary?.let { product ->
        WeightInputDialog(
            product = product,
            onDismiss = { selectedProductForDiary = null },
            onConfirm = { grams ->
                viewModel.addToDiary(product, grams, onSuccess = {
                    selectedProductForDiary = null
                    onBack()
                })
            }
        )
    }

    selectedProductForEdit?.let { product ->
        EditMacrosDialog(
            product = product,
            onDismiss = { selectedProductForEdit = null },
            onConfirm = { cals, p, f, c ->
                viewModel.updateCustomProductMacros(product, cals, p, f, c)
                selectedProductForEdit = null
            }
        )
    }
}

@Composable
fun EditMacrosDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Float, Float, Float, Float) -> Unit
) {
    var cals by remember { mutableStateOf(product.calories.toString()) }
    var protein by remember { mutableStateOf(product.protein.toString()) }
    var fat by remember { mutableStateOf(product.fat.toString()) }
    var carbs by remember { mutableStateOf(product.carbs.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать КБЖУ") },
        text = {
            Column {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cals,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) cals = it },
                        label = { Text("Ккал") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) protein = it },
                        label = { Text("Б, г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) fat = it },
                        label = { Text("Ж, г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) carbs = it },
                        label = { Text("У, г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    cals.toFloatOrNull() ?: 0f,
                    protein.toFloatOrNull() ?: 0f,
                    fat.toFloatOrNull() ?: 0f,
                    carbs.toFloatOrNull() ?: 0f
                )
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}