package com.example.mealplanner.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mealplanner.domain.model.Product

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
                    Text(
                        text = "Будет добавлено: ${currentCals.toInt()} ккал",
                        style = MaterialTheme.typography.bodySmall
                    )
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