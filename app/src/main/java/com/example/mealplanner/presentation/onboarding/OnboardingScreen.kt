package com.example.mealplanner.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinishOnboarding: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Расчет нормы КБЖУ") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (state.resultGoal == null) {
                Text(
                    text = "Пожалуйста, заполните данные для точного расчета по формуле Миффлина-Сан Жеора:",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text("Пол", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.gender == Gender.MALE,
                        onClick = { viewModel.updateField { it.copy(gender = Gender.MALE) } }
                    )
                    Text("Мужской")

                    Spacer(modifier = Modifier.width(24.dp))

                    RadioButton(
                        selected = state.gender == Gender.FEMALE,
                        onClick = { viewModel.updateField { it.copy(gender = Gender.FEMALE) } }
                    )
                    Text("Женский")
                }

                OutlinedTextField(
                    value = state.age,
                    onValueChange = { viewModel.updateField { s -> s.copy(age = it) } },
                    label = { Text("Возраст (лет)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.weight,
                    onValueChange = { viewModel.updateField { s -> s.copy(weight = it) } },
                    label = { Text("Вес (кг)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.height,
                    onValueChange = { viewModel.updateField { s -> s.copy(height = it) } },
                    label = { Text("Рост (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Уровень активности", style = MaterialTheme.typography.labelLarge)
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.activityLevel.title,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ActivityLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.title) },
                                onClick = {
                                    viewModel.updateField { it.copy(activityLevel = level) }
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.calculateMacros() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.age.isNotBlank() && state.weight.isNotBlank() && state.height.isNotBlank()
                ) {
                    Text("Рассчитать")
                }

            } else {
                val goal = state.resultGoal!!

                Text(
                    text = "Отлично! Ваша суточная норма рассчитана и сохранена.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Суточная калорийность: ${goal.tdee} ккал", style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider()
                        Text("Белки: ${goal.protein} г", style = MaterialTheme.typography.bodyLarge)
                        Text("Жиры: ${goal.fat} г", style = MaterialTheme.typography.bodyLarge)
                        Text("Углеводы: ${goal.carbs} г", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Перейти к Дневнику")
                }
            }
        }
    }
}