package com.example.mealplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mealplanner.domain.model.Product
import com.example.mealplanner.domain.repository.AuthRepository
import com.example.mealplanner.presentation.AddProductScreen
import com.example.mealplanner.presentation.AddProductViewModel
import com.example.mealplanner.presentation.CalendarScreen
import com.example.mealplanner.presentation.CreateProductScreen
import com.example.mealplanner.presentation.CreateProductViewModel
import com.example.mealplanner.presentation.CreateRecipeScreen
import com.example.mealplanner.presentation.CreateRecipeViewModel
import com.example.mealplanner.presentation.DiaryScreen
import com.example.mealplanner.presentation.DiaryViewModel
import com.example.mealplanner.presentation.ShoppingListScreen
import com.example.mealplanner.presentation.auth.AuthScreen
import com.example.mealplanner.presentation.components.SplashViewModel
import com.example.mealplanner.presentation.onboarding.OnboardingScreen
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


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
                    MealPlannerAppContent()
                }
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onSuccess()
        }
    }
}

@Composable
fun MealPlannerAppContent(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute == "diary" || currentRoute == "shopping_list"

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выход из аккаунта") },
            text = { Text("Вы уверены, что хотите выйти из приложения?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        mainViewModel.logout {
                            navController.navigate("splash") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Дневник") },
                        label = { Text("Дневник") },
                        selected = currentRoute == "diary",
                        onClick = {
                            if (currentRoute != "diary") {
                                navController.navigate("diary") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Покупки") },
                        label = { Text("Список покупок") },
                        selected = currentRoute == "shopping_list",
                        onClick = {
                            if (currentRoute != "shopping_list") {
                                navController.navigate("shopping_list") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Выход",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else 0.dp)
        ) {
            MealPlannerNavGraph(navController = navController)
        }
    }
}

@Composable
fun MealPlannerNavGraph(navController: NavHostController) {
    //val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash", modifier = Modifier.fillMaxSize()) {

        composable("splash") {
            val viewModel: SplashViewModel = hiltViewModel()
            val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.checkAuthState()
            }

            LaunchedEffect(startRoute) {
                startRoute?.let { route ->
                    navController.navigate(route) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable("auth") {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate("splash") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate("diary") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

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

        composable("shopping_list") {
            ShoppingListScreen(viewModel = hiltViewModel())
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
                    navController.popBackStack() //popBackStack("diary", inclusive = false)
                }
            )
        }
    }
}