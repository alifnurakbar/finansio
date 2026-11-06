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

@Composable
fun AppNavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    statisticsViewModel: StatisticsViewModel,
    categoryViewModel: CategoryViewModel,
    transactionFormViewModel: TransactionFormViewModel,
    transactionHistoryViewModel: TransactionHistoryViewModel,
    scanResultViewModel: ScanResultViewModel,
    monthFilterViewModel: MonthFilterViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                monthFilterViewModel = monthFilterViewModel,
                onNavigateToTransaction = { id ->
                    navController.navigate(Screen.TransactionForm.createRoute(id))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.TransactionHistory.route)
                }
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
            CategoryScreen(viewModel = categoryViewModel)
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
