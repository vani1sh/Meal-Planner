package com.example.mealplanner.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.mealplanner.domain.repository.AuthRepository
import com.example.mealplanner.domain.repository.FoodRepository
import com.example.mealplanner.presentation.components.WeightInputDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val repository: FoodRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val selectedTimestamp: Long = savedStateHandle.get<Long>("timestamp") ?: System.currentTimeMillis()
    private val selectedMealType: String = savedStateHandle.get<String>("mealType") ?: MealType.SNACK.name

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()

    val customProducts: StateFlow<List<Product>> = authRepository.currentUser
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList())
            else repository.getCustomProductsFlow(userId)
        }
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes: StateFlow<List<Product>> = authRepository.currentUser
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList())
            else repository.getRecipesFlow(userId)
        }
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val userId = authRepository.currentUser.firstOrNull() ?: return@launch

            val result = repository.searchProducts(query, userId)
            if (result.isSuccess) {
                _searchResults.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun addToDiary(product: Product, amountGrams: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val userId = authRepository.currentUser.firstOrNull() ?: return@launch

            repository.addDiaryEntry(
                DiaryEntry(
                    product = product,
                    amountGrams = amountGrams,
                    timestamp = selectedTimestamp,
                    mealType = MealType.valueOf(selectedMealType)
                ),
                userId = userId
            )
            onSuccess()
        }
    }

    fun addProductToShoppingList(product: Product, amountGrams: Int) {
        viewModelScope.launch {
            val userId = authRepository.currentUser.firstOrNull() ?: return@launch
            repository.addProductToShoppingList(product, amountGrams, userId)
        }
    }

    fun deleteItem(product: Product) {
        viewModelScope.launch {
            val userId = authRepository.currentUser.firstOrNull() ?: return@launch
            if (product.isRecipe) {
                repository.deleteRecipe(product.id, userId)
            } else {
                repository.deleteCustomProduct(product.id, userId)
            }
            updateSearchQuery(_searchQuery.value)
        }
    }

    fun updateCustomProductMacros(product: Product, cals: Float, p: Float, f: Float, c: Float) {
        viewModelScope.launch {
            val userId = authRepository.currentUser.firstOrNull() ?: return@launch

            val updatedProduct = product.copy(calories = cals, protein = p, fat = f, carbs = c)
            repository.updateCustomProduct(updatedProduct, userId)
            updateSearchQuery(_searchQuery.value)
        }
    }
}

@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel,
    isPicker: Boolean = false,
    onProductPickedForRecipe: (Product, Int) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onNavigateToCreateProduct: () -> Unit,
    onNavigateToCreateRecipe: () -> Unit,
    onEditRecipeClick: (String) -> Unit = {}
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val customProducts by viewModel.customProducts.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()

    var selectedProductForDiary by remember { mutableStateOf<Product?>(null) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }
    var expandedMenuFor by remember { mutableStateOf<String?>(null) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = if (isPicker) {
        listOf("Поиск", "Мои продукты")
    } else {
        listOf("Поиск", "Мои продукты", "Рецепты")
    }

    Scaffold(
        floatingActionButton = {
            when (selectedTabIndex) {
                1 -> {
                    FloatingActionButton(
                        onClick = onNavigateToCreateProduct,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Создать свой продукт")
                    }
                }
                2 -> {
                    if (!isPicker) {
                        FloatingActionButton(
                            onClick = onNavigateToCreateRecipe,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Создать рецепт")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            viewModel.updateSearchQuery("")
                        },
                        text = { Text(title) }
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = {
                    Text(when(selectedTabIndex) {
                        0 -> "Поиск"
                        1 -> "Поиск в своих продуктах"
                        else -> "Поиск в рецептах"
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            when (selectedTabIndex) {
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { product ->
                            ProductListItem(
                                product = product,
                                expandedMenuFor = expandedMenuFor,
                                onMenuClick = { expandedMenuFor = it },
                                onDismissMenu = { expandedMenuFor = null },
                                onProductClick = { selectedProductForDiary = product },
                                onEditClick = { selectedProductForEdit = product },
                                onDeleteClick = { viewModel.deleteItem(product) },
                                onEditRecipeClick = onEditRecipeClick
                            )
                        }
                    }
                }
                1 -> {
                    if (customProducts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Вы еще не добавили ни одного продукта", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(customProducts) { product ->
                                ProductListItem(
                                    product = product,
                                    expandedMenuFor = expandedMenuFor,
                                    onMenuClick = { expandedMenuFor = it },
                                    onDismissMenu = { expandedMenuFor = null },
                                    onProductClick = { selectedProductForDiary = product },
                                    onEditClick = { selectedProductForEdit = product },
                                    onDeleteClick = { viewModel.deleteItem(product) },
                                    onEditRecipeClick = onEditRecipeClick
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (recipes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("У вас пока нет сохраненных рецептов", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(recipes) { recipe ->
                                ProductListItem(
                                    product = recipe,
                                    expandedMenuFor = expandedMenuFor,
                                    onMenuClick = { expandedMenuFor = it },
                                    onDismissMenu = { expandedMenuFor = null },
                                    onProductClick = { selectedProductForDiary = recipe },
                                    onEditClick = {  },
                                    onDeleteClick = { viewModel.deleteItem(recipe) },
                                    onEditRecipeClick = onEditRecipeClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedProductForDiary?.let { product ->
        WeightInputDialog(
            product = product,
            selectedDateTimestamp = viewModel.selectedTimestamp,
            onDismiss = { selectedProductForDiary = null },
            onConfirm = { grams, addToShoppingList ->
                if (isPicker) {
                    onProductPickedForRecipe(product, grams)
                    selectedProductForDiary = null
                } else {
                    viewModel.addToDiary(product, grams, onSuccess = {
                        selectedProductForDiary = null
                        onBack()
                    })
                    if (addToShoppingList) {
                        viewModel.addProductToShoppingList(product, grams)
                    }
                }
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
fun ProductListItem(
    product: Product,
    expandedMenuFor: String?,
    onMenuClick: (String) -> Unit,
    onDismissMenu: () -> Unit,
    onProductClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditRecipeClick: (String) -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onProductClick() },
        headlineContent = { Text(product.name) },
        supportingContent = {
            val color = if (product.isRecipe) MaterialTheme.colorScheme.tertiary
            else if (product.isCustom) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant

            Text(
                text = product.brand ?: (if (product.isRecipe) "Рецепт" else if (product.isCustom) "Ваш продукт" else "Неизвестный бренд"),
                color = color
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${product.calories.toInt()} ккал", modifier = Modifier.padding(end = 8.dp))

                if (product.isCustom || product.isRecipe) {
                    Box {
                        IconButton(onClick = { onMenuClick(product.id) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(
                            expanded = expandedMenuFor == product.id,
                            onDismissRequest = onDismissMenu
                        ) {
                            if (!product.isRecipe) {
                                DropdownMenuItem(
                                    text = { Text("Изменить КБЖУ") },
                                    onClick = {
                                        onDismissMenu()
                                        onEditClick()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Изменить рецепт") },
                                    onClick = { onDismissMenu(); onEditRecipeClick(product.id) })
                            }
                            DropdownMenuItem(
                                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onDismissMenu()
                                    onDeleteClick()
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