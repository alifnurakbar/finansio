package com.finansio.app.ui

import androidx.lifecycle.ViewModel
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class SelectedMonth(val year: Int, val month: Int)

class MonthFilterViewModel : ViewModel() {

    private val now = Calendar.getInstance(DateUtils.localeID)

    private val _selectedMonth = MutableStateFlow(
        SelectedMonth(
            year = now.get(Calendar.YEAR),
            month = now.get(Calendar.MONTH) // 0-based (Calendar.JANUARY = 0)
        )
    )
    val selectedMonth: StateFlow<SelectedMonth> = _selectedMonth.asStateFlow()

    fun nextMonth() {
        val current = _selectedMonth.value
        val cal = Calendar.getInstance(DateUtils.localeID)
        cal.set(current.year, current.month, 1)
        cal.add(Calendar.MONTH, 1)
        _selectedMonth.value = SelectedMonth(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH)
        )
    }

    fun prevMonth() {
        val current = _selectedMonth.value
        val cal = Calendar.getInstance(DateUtils.localeID)
        cal.set(current.year, current.month, 1)
        cal.add(Calendar.MONTH, -1)
        _selectedMonth.value = SelectedMonth(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH)
        )
    }

    fun getLabel(): String = DateUtils.formatMonthYear(_selectedMonth.value.year, _selectedMonth.value.month)
}
