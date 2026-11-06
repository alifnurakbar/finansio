package com.finansio.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.finansio.app.data.repository.CategoryRepository
import com.finansio.app.data.repository.TransactionRepository
import com.finansio.app.ui.category.CategoryViewModel
import com.finansio.app.ui.MonthFilterViewModel
import com.finansio.app.ui.statistics.StatisticsViewModel
import com.finansio.app.ui.transaction.form.TransactionFormViewModel
import com.finansio.app.ui.transaction.history.TransactionHistoryViewModel
import com.finansio.app.ui.transaction.list.HomeViewModel

@Suppress("UNCHECKED_CAST")
class FinansioViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val aiRepository: com.finansio.app.data.repository.AiRepository,
    private val monthFilterViewModel: MonthFilterViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(transactionRepository, monthFilterViewModel) as T
            }
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> {
                StatisticsViewModel(transactionRepository, monthFilterViewModel, aiRepository) as T
            }
            modelClass.isAssignableFrom(CategoryViewModel::class.java) -> {
                CategoryViewModel(categoryRepository) as T
            }
            modelClass.isAssignableFrom(TransactionFormViewModel::class.java) -> {
                TransactionFormViewModel(transactionRepository, categoryRepository) as T
            }
            modelClass.isAssignableFrom(TransactionHistoryViewModel::class.java) -> {
                TransactionHistoryViewModel(transactionRepository) as T
            }
            modelClass.isAssignableFrom(com.finansio.app.ui.scanner.ScanResultViewModel::class.java) -> {
                com.finansio.app.ui.scanner.ScanResultViewModel(aiRepository, categoryRepository, transactionRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
