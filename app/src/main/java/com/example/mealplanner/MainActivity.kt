package com.example.mealplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mealplanner.presentation.add_product.AddProductScreen
import com.example.mealplanner.presentation.add_product.AddProductViewModel
import com.example.mealplanner.presentation.calendar.CalendarScreen
import com.example.mealplanner.presentation.create_product.CreateProductScreen
import com.example.mealplanner.presentation.create_product.CreateProductViewModel
import com.example.mealplanner.presentation.diary.DiaryScreen
import com.example.mealplanner.presentation.diary.DiaryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MealPlannerNavGraph()
                }
            }
        }
    }
}

@Composable
fun MealPlannerNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "diary") {
        composable("diary") { backStackEntry ->
            val viewModel: DiaryViewModel = hiltViewModel()

            val savedStateHandle = backStackEntry.savedStateHandle
            val selectedDate by savedStateHandle.getStateFlow<Long?>("selected_date", null).collectAsStateWithLifecycle()

            LaunchedEffect(selectedDate) {
                selectedDate?.let {
                    viewModel.setDate(it)
                    savedStateHandle.remove<Long>("selected_date")
                }
            }

            DiaryScreen(
                viewModel = viewModel,
                onNavigateToAddProduct = { timestamp, mealType ->
                    navController.navigate("add_product/$timestamp/${mealType.name}")
                },
                onNavigateToCalendar = {
                    navController.navigate("calendar")
                }
            )
        }

        composable("calendar") {
            CalendarScreen(
                onBack = { navController.popBackStack() },
                onDateSelected = { timestamp ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_date", timestamp)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "add_product/{timestamp}/{mealType}",
            arguments = listOf(
                navArgument("timestamp") { type = NavType.LongType },
                navArgument("mealType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: 0L
            val mealType = backStackEntry.arguments?.getString("mealType") ?: ""

            val viewModel: AddProductViewModel = hiltViewModel()
            AddProductScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToCreateProduct = { navController.navigate("create_product/$timestamp/$mealType") }
            )
        }

        composable(
            route = "create_product/{timestamp}/{mealType}",
            arguments = listOf(
                navArgument("timestamp") { type = NavType.LongType },
                navArgument("mealType") { type = NavType.StringType }
            )
        ) {
            val viewModel: CreateProductViewModel = hiltViewModel()
            CreateProductScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateBackToDiary = {
                    navController.popBackStack("diary", inclusive = false)
                }
            )
        }
    }
}