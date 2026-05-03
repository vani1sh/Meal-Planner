package com.example.mealplanner.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    var currentMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val monthYearString = remember(currentMonth) {
        val format = SimpleDateFormat("LLLL yyyy", Locale("ru"))
        format.format(currentMonth.time).replaceFirstChar { it.uppercase() }
    }

    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = currentMonth.get(Calendar.DAY_OF_WEEK)
    val emptyCells = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    val today = remember { Calendar.getInstance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выбрать дату") },
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newMonth = currentMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, -1)
                    currentMonth = newMonth
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущий месяц")
                }

                Text(
                    text = monthYearString,
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = {
                    val newMonth = currentMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, 1)
                    currentMonth = newMonth
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующий месяц")
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                items(emptyCells) {
                    Spacer(modifier = Modifier.aspectRatio(1f))
                }

                items(daysInMonth) { dayIndex ->
                    val dayOfMonth = dayIndex + 1

                    val isToday = currentMonth.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            currentMonth.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            dayOfMonth == today.get(Calendar.DAY_OF_MONTH)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                val selectedDate = currentMonth.clone() as Calendar
                                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                onDateSelected(selectedDate.timeInMillis)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}