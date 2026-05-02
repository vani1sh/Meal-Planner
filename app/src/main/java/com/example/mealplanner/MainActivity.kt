package com.example.mealplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mealplanner.presentation.add_product.AddProductScreen
import com.example.mealplanner.presentation.add_product.AddProductViewModel
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
        composable("diary") {
            DiaryScreen(
                viewModel = hiltViewModel(),
                onNavigateToAddProduct = { timestamp, mealType ->
                    navController.navigate("add_product/$timestamp/${mealType.name}")
                }
            )
        }
        composable(
            route = "add_product/{timestamp}/{mealType}",
            arguments = listOf(
                navArgument("timestamp") { type = NavType.LongType },
                navArgument("mealType") { type = NavType.StringType }
            )
        ) {
            AddProductScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}