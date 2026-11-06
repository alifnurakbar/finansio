package com.finansio.app.ui.transaction.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.data.repository.TransactionRepository
import com.finansio.app.ui.MonthFilterViewModel
import com.finansio.app.ui.SelectedMonth
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.flow.*

class HomeViewModel(
    private val repository: TransactionRepository,
    private val monthFilterViewModel: MonthFilterViewModel
) : ViewModel() {

    val transactions: StateFlow<List<TransactionEntity>> = repository.getRecentTransactions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedMonth: StateFlow<SelectedMonth> = monthFilterViewModel.selectedMonth

    val totalIncome: StateFlow<Long> = selectedMonth.flatMapLatest { m ->
        repository.getTotalByTypeAndDateRange(
            TransactionType.INCOME,
            DateUtils.getStartOfMonth(m.year, m.month),
            DateUtils.getEndOfMonth(m.year, m.month)
        )
    }.map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalExpense: StateFlow<Long> = selectedMonth.flatMapLatest { m ->
        repository.getTotalByTypeAndDateRange(
            TransactionType.EXPENSE,
            DateUtils.getStartOfMonth(m.year, m.month),
            DateUtils.getEndOfMonth(m.year, m.month)
        )
    }.map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val balance: StateFlow<Long> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val currentMonthTransactions: Flow<List<TransactionEntity>> = selectedMonth.flatMapLatest { m ->
        repository.getByDateRange(
            DateUtils.getStartOfMonth(m.year, m.month),
            DateUtils.getEndOfMonth(m.year, m.month)
        )
    }

    val monthlyIncomeData: StateFlow<List<Double>> = currentMonthTransactions.map { txs ->
        calculateWeeklyTotals(txs, TransactionType.INCOME)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(5) { 0.0 })

    val monthlyExpenseData: StateFlow<List<Double>> = currentMonthTransactions.map { txs ->
        calculateWeeklyTotals(txs, TransactionType.EXPENSE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(5) { 0.0 })

    private fun calculateWeeklyTotals(
        transactions: List<TransactionEntity>,
        type: TransactionType
    ): List<Double> {
        val weeklyTotals = DoubleArray(5) { 0.0 }
        val filtered = transactions.filter { it.type == type }
        for (tx in filtered) {
            val calendar = java.util.Calendar.getInstance(DateUtils.localeID)
            calendar.timeInMillis = tx.date
            val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val weekIndex = when (dayOfMonth) {
                in 1..7 -> 0
                in 8..14 -> 1
                in 15..21 -> 2
                in 22..28 -> 3
                else -> 4
            }
            weeklyTotals[weekIndex] += tx.amount.toDouble()
        }
        return weeklyTotals.toList()
    }
}
