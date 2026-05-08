package com.example.mealplanner.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealplanner.data.local.entity.ShoppingListItemEntity
import com.example.mealplanner.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {

    val shoppingList: StateFlow<List<ShoppingListItemEntity>> = repository.getShoppingList()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateItem(item: ShoppingListItemEntity, newWeight: Int) {
        viewModelScope.launch {
            repository.updateShoppingListItem(item.copy(amountGrams = newWeight))
        }
    }

    fun deleteItem(item: ShoppingListItemEntity) {
        viewModelScope.launch {
            repository.deleteShoppingListItem(item)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel) {
    val items by viewModel.shoppingList.collectAsStateWithLifecycle()
    var editingItem by remember { mutableStateOf<ShoppingListItemEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Список покупок") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(items) { item ->
                ShoppingListItemRow(
                    item = item,
                    onEdit = { editingItem = item },
                    onDelete = { viewModel.deleteItem(item) }
                )
                Divider()
            }
        }
    }

    editingItem?.let { item ->
        var weightInput by remember { mutableStateOf(item.amountGrams.toString()) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Изменить вес") },
            text = {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Вес (г)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newWeight = weightInput.toIntOrNull() ?: item.amountGrams
                    viewModel.updateItem(item, newWeight)
                    editingItem = null
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingListItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            item.note?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${item.amountGrams} г", modifier = Modifier.padding(end = 8.dp))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Изменить")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}