package com.example.mealplanner.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealplanner.domain.model.DiaryEntry
import com.example.mealplanner.domain.model.MealType
import com.example.mealplanner.domain.model.Product
import com.example.mealplanner.domain.model.RecipeIngredient
import com.example.mealplanner.domain.repository.FoodRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateRecipeViewModel @Inject constructor(
    private val repository: FoodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val selectedTimestamp: Long = savedStateHandle.get<Long>("timestamp") ?: System.currentTimeMillis()
    private val selectedMealType: String = savedStateHandle.get<String>("mealType") ?: "SNACK"

    private val editingRecipeId: String? = savedStateHandle.get<String>("recipeId")

    val isEditing: Boolean = editingRecipeId != null

    var recipeName by mutableStateOf("")

    private val _ingredients = MutableStateFlow<List<RecipeIngredient>>(emptyList())
    val ingredients = _ingredients.asStateFlow()

    init {
        editingRecipeId?.let { id ->
            viewModelScope.launch {
                repository.getRecipeById(id).getOrNull()?.let { recipe ->
                    recipeName = recipe.name
                    if (!recipe.recipeIngredientsJson.isNullOrBlank()) {
                        val type = object : TypeToken<List<RecipeIngredient>>() {}.type
                        _ingredients.value = Gson().fromJson(recipe.recipeIngredientsJson, type) ?: emptyList()
                    }
                }
            }
        }
    }

    val recipeSummary = _ingredients.map { list ->
        if (list.isEmpty()) return@map Product("", "", null, 0f, 0f, 0f, 0f, true, true)

        val totalWeight = list.sumOf { it.amountGrams.toDouble() }.toFloat()

        val totalCals = list.sumOf { (it.product.calories * it.amountGrams / 100.0) }.toFloat()
        val totalProt = list.sumOf { (it.product.protein * it.amountGrams / 100.0) }.toFloat()
        val totalFat = list.sumOf { (it.product.fat * it.amountGrams / 100.0) }.toFloat()
        val totalCarb = list.sumOf { (it.product.carbs * it.amountGrams / 100.0) }.toFloat()

        Product(
            id = editingRecipeId ?: UUID.randomUUID().toString(),
            name = recipeName,
            brand = "Рецепт",
            calories = if (totalWeight > 0) (totalCals / totalWeight) * 100f else 0f,
            protein = if (totalWeight > 0) (totalProt / totalWeight) * 100f else 0f,
            fat = if (totalWeight > 0) (totalFat / totalWeight) * 100f else 0f,
            carbs = if (totalWeight > 0) (totalCarb / totalWeight) * 100f else 0f,
            isCustom = true,
            isRecipe = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Product("", "", null, 0f, 0f, 0f, 0f, true, true))

    fun addIngredient(product: Product, grams: Int) {
        _ingredients.update { it + RecipeIngredient(product, grams) }
    }

    fun removeIngredient(ingredient: RecipeIngredient) {
        _ingredients.update { it - ingredient }
    }

    fun saveAndFinish(onSuccess: () -> Unit) {
        if (recipeName.isBlank() || _ingredients.value.size < 2) return

        viewModelScope.launch {
            val finalRecipe = recipeSummary.value.copy(
                recipeIngredientsJson = Gson().toJson(_ingredients.value)
            )
            repository.saveRecipe(finalRecipe)

            if (editingRecipeId == null) {
                repository.addDiaryEntry(
                    DiaryEntry(
                        product = finalRecipe,
                        amountGrams = 100,
                        timestamp = selectedTimestamp,
                        mealType = MealType.valueOf(selectedMealType)
                    )
                )
            }
            onSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    viewModel: CreateRecipeViewModel,
    onNavigateToPickIngredient: () -> Unit,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val summary by viewModel.recipeSummary.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Конструктор рецепта") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                }
            )
        },

    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = viewModel.recipeName,
                onValueChange = { viewModel.recipeName = it },
                label = { Text("Название рецепта") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Итого на 100г готового блюда:", style = MaterialTheme.typography.labelLarge)
                    Text("${summary.calories.toInt()} ккал", style = MaterialTheme.typography.headlineSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Б: ${"%.1f".format(summary.protein)}г")
                        Text("Ж: ${"%.1f".format(summary.fat)}г")
                        Text("У: ${"%.1f".format(summary.carbs)}г")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Состав:", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(ingredients) { item ->
                    ListItem(
                        headlineContent = { Text(item.product.name) },
                        supportingContent = { Text("${item.amountGrams}г — ${item.product.brand ?: "Неизвестный бренд"}") },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeIngredient(item) }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }

                item {
                    ListItem(
                        modifier = Modifier.clickable { onNavigateToPickIngredient() },
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Добавить ингредиент",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }

            Button(
                onClick = { viewModel.saveAndFinish(onSuccess) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = viewModel.recipeName.isNotBlank() && ingredients.size >= 2
            ) {
                Text(if (viewModel.isEditing) "Сохранить" else "Сохранить и добавить в дневник")
            }
        }
    }
}