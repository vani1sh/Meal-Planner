package com.example.mealplanner.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mealplanner.domain.model.Product
import java.util.Calendar
import java.util.Locale


fun calculateDaysDifference(selectedTimestamp: Long): Int {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val targetDate = Calendar.getInstance().apply {
        timeInMillis = selectedTimestamp
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val diffInMillis = targetDate.timeInMillis - today.timeInMillis
    return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
}

@Composable
fun WeightInputDialog(
    product: Product,
    selectedDateTimestamp: Long,
    onDismiss: () -> Unit,
    onConfirm: (Int, Boolean) -> Unit
) {
    var weightInput by remember { mutableStateOf("100") }
    val weight = weightInput.toIntOrNull() ?: 0

    val currentCals = (product.calories * weight / 100f)
    val currentProtein = (product.protein * weight / 100f)
    val currentFat = (product.fat * weight / 100f)
    val currentCarbs = (product.carbs * weight / 100f)

    val daysDiff = remember(selectedDateTimestamp) { calculateDaysDifference(selectedDateTimestamp) }

    val showCheckbox = daysDiff == 0 || daysDiff == 1
    val isAutoAdded = daysDiff > 1
    val isNeverAdded = daysDiff < 0

    var addToShoppingList by remember {
        mutableStateOf(daysDiff == 1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить ${product.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Вес (г)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroValue("Ккал", String.format(Locale("ru"), "%.1f", currentCals))
                        MacroValue("Белки", String.format(Locale("ru"), "%.1f", currentProtein))
                        MacroValue("Жиры", String.format(Locale("ru"), "%.1f", currentFat))
                        MacroValue("Углев", String.format(Locale("ru"), "%.1f", currentCarbs))
                    }
                }

                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = addToShoppingList,
                            onCheckedChange = { addToShoppingList = it }
                        )
                        Text("Добавить в список покупок", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (isAutoAdded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Будет автоматически добавлено в список покупок",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalAddFlag = when {
                    isNeverAdded -> false
                    isAutoAdded -> true
                    else -> addToShoppingList
                }
                onConfirm(weightInput.toIntOrNull() ?: 0, finalAddFlag)
            }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun MacroValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}