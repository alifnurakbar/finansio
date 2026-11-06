package com.finansio.app.ui.transaction.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.repository.TransactionRepository
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionHistoryViewModel(private val repository: TransactionRepository) : ViewModel() {

    val currentMonthOffset = MutableStateFlow(0)
    val searchQuery = MutableStateFlow("")

    val monthLabel: StateFlow<String> = currentMonthOffset.map { offset ->
        DateUtils.getMonthLabel(offset)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DateUtils.getMonthLabel(0))

    // Transactions for the selected month
    private val allTransactionsForMonth = currentMonthOffset.flatMapLatest { offset ->
        val start = getStartOfMonthWithOffset(offset)
        val end = getEndOfMonthWithOffset(offset)
        repository.getByDateRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered by search query
    val filteredTransactions = combine(allTransactionsForMonth, searchQuery) { transactions, query ->
        if (query.isBlank()) {
            transactions
        } else {
            transactions.filter { tx ->
                val noteMatch = tx.note?.contains(query, ignoreCase = true) == true
                val categoryMatch = tx.categoryName.contains(query, ignoreCase = true)
                noteMatch || categoryMatch
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextMonth() {
        if (currentMonthOffset.value < 0) {
            currentMonthOffset.value += 1
        }
    }

    fun previousMonth() {
        currentMonthOffset.value -= 1
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    private fun getStartOfMonthWithOffset(offset: Int): Long {
        val calendar = java.util.Calendar.getInstance(DateUtils.localeID)
        calendar.add(java.util.Calendar.MONTH, offset)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfMonthWithOffset(offset: Int): Long {
        val calendar = java.util.Calendar.getInstance(DateUtils.localeID)
        calendar.add(java.util.Calendar.MONTH, offset)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
