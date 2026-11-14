package com.finansio.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.data.repository.BudgetProgress
import com.finansio.app.data.repository.BudgetRepository
import com.finansio.app.data.repository.CategoryRepository
import com.finansio.app.ui.MonthFilterViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    monthFilterViewModel: MonthFilterViewModel
) : ViewModel() {

    val selectedMonth = monthFilterViewModel.selectedMonth

    val budgets: StateFlow<List<BudgetProgress>> = selectedMonth
        .flatMapLatest { budgetRepository.progressForMonth(it.year, it.month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Kategori pengeluaran yang belum punya anggaran (pilihan saat menambah anggaran). */
    val availableCategories: StateFlow<List<CategoryEntity>> = combine(
        categoryRepository.getByType(TransactionType.EXPENSE),
        budgets
    ) { categories, current ->
        val used = current.map { it.category.id }.toSet()
        categories.filterNot { it.id in used }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBudget(categoryId: Int, limit: Long) {
        if (limit <= 0) return
        viewModelScope.launch { budgetRepository.setBudget(categoryId, limit) }
    }

    fun deleteBudget(categoryId: Int) {
        viewModelScope.launch { budgetRepository.deleteBudget(categoryId) }
    }
}
