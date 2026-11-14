package com.finansio.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Statistics : Screen("statistics")
    object Category : Screen("category")
    object TransactionHistory : Screen("transaction_history")
    object TransactionForm : Screen("transaction_form/{id}") {
        fun createRoute(id: Int? = null) = "transaction_form/${id ?: -1}"
    }
    object ScanResult : Screen("scan_result")
    object Budget : Screen("budget")
    object Profile : Screen("profile")
}
