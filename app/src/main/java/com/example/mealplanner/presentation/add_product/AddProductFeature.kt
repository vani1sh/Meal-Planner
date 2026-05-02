package com.example.mealplanner.presentation.add_product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.Product
import com.example.mealplanner.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()

    fun search(query: String) {
        if (query.length < 3) return
        viewModelScope.launch {
            val result = repository.searchProducts(query)
            if (result.isSuccess) {
                _searchResults.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun addToDiary(product: Product, amountGrams: Int) {
        viewModelScope.launch {
            repository.addDiaryEntry(
                DiaryEntry(
                    product = product,
                    amountGrams = amountGrams,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}

@Composable
fun AddProductScreen(viewModel: AddProductViewModel, onBack: () -> Unit) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it)
            },
            label = { Text("Поиск продукта (API + Свои)") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        LazyColumn {
            items(searchResults) { product ->
                ListItem(
                    modifier = Modifier.clickable {
                        viewModel.addToDiary(product, 100)
                        onBack()
                    },
                    headlineContent = { Text(product.name) },
                    supportingContent = { Text(product.brand ?: "Неизвестный бренд") },
                    trailingContent = { Text("${product.calories.toInt()} ккал / 100г") }
                )
                Divider()
            }
        }
    }
}