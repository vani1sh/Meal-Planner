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
import com.example.mealplanner.domain.model.Product
import com.example.mealplanner.presentation.AddProductScreen
import com.example.mealplanner.presentation.AddProductViewModel
import com.example.mealplanner.presentation.CalendarScreen
import com.example.mealplanner.presentation.CreateProductScreen
import com.example.mealplanner.presentation.CreateProductViewModel
import com.example.mealplanner.presentation.CreateRecipeScreen
import com.example.mealplanner.presentation.CreateRecipeViewModel
import com.example.mealplanner.presentation.DiaryScreen
import com.example.mealplanner.presentation.DiaryViewModel
import com.google.gson.Gson
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
            route = "add_product/{timestamp}/{mealType}?isPicker={isPicker}",
            arguments = listOf(
                navArgument("timestamp") { type = NavType.LongType },
                navArgument("mealType") { type = NavType.StringType },
                navArgument("isPicker") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: 0L
            val mealType = backStackEntry.arguments?.getString("mealType") ?: ""
            val isPicker = backStackEntry.arguments?.getBoolean("isPicker") ?: false

            val viewModel: AddProductViewModel = hiltViewModel()
            AddProductScreen(
                viewModel = viewModel,
                isPicker = isPicker,
                onProductPickedForRecipe = { product, grams ->
                    val gson = com.google.gson.Gson()
                    navController.previousBackStackEntry?.savedStateHandle?.set("picked_product", gson.toJson(product))
                    navController.previousBackStackEntry?.savedStateHandle?.set("picked_weight", grams)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                onNavigateToCreateProduct = { navController.navigate("create_product/$timestamp/$mealType") },
                onNavigateToCreateRecipe = { navController.navigate("create_recipe/$timestamp/$mealType") },
                onEditRecipeClick = { recipeId ->
                    navController.navigate("create_recipe/$timestamp/$mealType?recipeId=$recipeId")
                }
            )
        }

        composable(
            route = "create_product/{timestamp}/{mealType}?isPicker={isPicker}",
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

        composable(
            route = "create_recipe/{timestamp}/{mealType}?recipeId={recipeId}",
            arguments = listOf(
                navArgument("timestamp") { type = NavType.LongType },
                navArgument("mealType") { type = NavType.StringType },
                navArgument("recipeId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val viewModel: CreateRecipeViewModel = hiltViewModel()

            val savedStateHandle = backStackEntry.savedStateHandle
            val pickedProductJson by savedStateHandle.getStateFlow<String?>("picked_product", null).collectAsStateWithLifecycle()
            val pickedWeight by savedStateHandle.getStateFlow<Int>("picked_weight", 0).collectAsStateWithLifecycle()

            LaunchedEffect(pickedProductJson, pickedWeight) {
                if (pickedProductJson != null && pickedWeight > 0) {
                    val gson = com.google.gson.Gson()
                    val product = gson.fromJson(pickedProductJson, Product::class.java)
                    viewModel.addIngredient(product, pickedWeight)

                    savedStateHandle.remove<String>("picked_product")
                    savedStateHandle.remove<Int>("picked_weight")
                }
            }

            CreateRecipeScreen(
                viewModel = viewModel,
                onNavigateToPickIngredient = {
                    navController.navigate("add_product/0/SNACK?isPicker=true")
                },
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.popBackStack("diary", inclusive = false)
                }
            )
        }
    }
}