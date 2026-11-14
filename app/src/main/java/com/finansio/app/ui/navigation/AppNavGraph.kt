package com.finansio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.finansio.app.ui.category.CategoryScreen
import com.finansio.app.ui.category.CategoryViewModel
import com.finansio.app.ui.statistics.StatisticsScreen
import com.finansio.app.ui.statistics.StatisticsViewModel
import com.finansio.app.ui.transaction.form.TransactionFormScreen
import com.finansio.app.ui.transaction.form.TransactionFormViewModel
import com.finansio.app.ui.transaction.list.HomeScreen
import com.finansio.app.ui.transaction.list.HomeViewModel
import com.finansio.app.ui.scanner.ScanResultScreen
import com.finansio.app.ui.scanner.ScanResultViewModel

import com.finansio.app.ui.transaction.history.TransactionHistoryScreen
import com.finansio.app.ui.transaction.history.TransactionHistoryViewModel
import com.finansio.app.ui.MonthFilterViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.finansio.app.data.budget.BudgetAlertManager
import com.finansio.app.data.settings.SettingsRepository
import com.finansio.app.ui.budget.BudgetScreen
import com.finansio.app.ui.budget.BudgetViewModel
import com.finansio.app.ui.profile.ProfileScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finansio.app.ui.chat.ChatScreen
import com.finansio.app.ui.chat.ChatViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    statisticsViewModel: StatisticsViewModel,
    categoryViewModel: CategoryViewModel,
    transactionFormViewModel: TransactionFormViewModel,
    transactionHistoryViewModel: TransactionHistoryViewModel,
    scanResultViewModel: ScanResultViewModel,
    monthFilterViewModel: MonthFilterViewModel,
    budgetViewModel: BudgetViewModel,
    settingsRepository: SettingsRepository,
    budgetAlertManager: BudgetAlertManager,
    viewModelFactory: ViewModelProvider.Factory
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            val userName by settingsRepository.userName.collectAsState()
            val budgetAlerts by budgetAlertManager.alerts.collectAsState()
            HomeScreen(
                viewModel = homeViewModel,
                monthFilterViewModel = monthFilterViewModel,
                onNavigateToTransaction = { id ->
                    navController.navigate(Screen.TransactionForm.createRoute(id))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.TransactionHistory.route)
                },
                userName = userName,
                budgetAlerts = budgetAlerts,
                onDismissAlert = budgetAlertManager::dismiss,
                onAlertClick = { navController.navigateToTab(Screen.Budget.route) },
                onOpenChat = { navController.navigate(Screen.Chat.route) }
            )
        }
        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(
                viewModel = transactionHistoryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTransaction = { id ->
                    navController.navigate(Screen.TransactionForm.createRoute(id))
                }
            )
        }
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                viewModel = statisticsViewModel,
                monthFilterViewModel = monthFilterViewModel
            )
        }
        composable(Screen.Category.route) {
            CategoryScreen(viewModel = categoryViewModel, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.TransactionForm.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: -1
            androidx.compose.runtime.LaunchedEffect(id) {
                transactionFormViewModel.loadTransaction(id)
            }
            TransactionFormScreen(
                viewModel = transactionFormViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Chat.route) {
            // ViewModel terikat ke halaman ini: riwayat chat hilang saat halaman ditutup
            val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
            ChatScreen(viewModel = chatViewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Budget.route) {
            BudgetScreen(viewModel = budgetViewModel, monthFilterViewModel = monthFilterViewModel)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                settingsRepository = settingsRepository,
                onNavigateToCategories = { navController.navigate(Screen.Category.route) },
                onNavigateToBudget = { navController.navigateToTab(Screen.Budget.route) }
            )
        }
        composable(Screen.ScanResult.route) {
            ScanResultScreen(
                viewModel = scanResultViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManualForm = {
                    navController.popBackStack()
                    navController.navigate(Screen.TransactionForm.createRoute())
                }
            )
        }
    }
}

/** Pindah antar-tab menu bawah tanpa menumpuk riwayat layar. */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
