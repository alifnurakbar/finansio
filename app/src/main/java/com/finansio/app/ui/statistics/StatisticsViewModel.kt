package com.finansio.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansio.app.data.local.dao.CategorySummary
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.data.repository.AiRepository
import com.finansio.app.data.repository.TransactionRepository
import com.finansio.app.ui.MonthFilterViewModel
import com.finansio.app.ui.SelectedMonth
import com.finansio.app.util.CurrencyFormatter
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// State for the AI Cashflow Insight feature
sealed class InsightUiState {
    object Idle : InsightUiState()
    object Loading : InsightUiState()
    data class Success(val text: String) : InsightUiState()
    data class Error(val message: String) : InsightUiState()
}

class StatisticsViewModel(
    private val transactionRepository: TransactionRepository,
    private val monthFilterViewModel: MonthFilterViewModel,
    private val aiRepository: AiRepository
) : ViewModel() {

    val filterIndex = MutableStateFlow(1) // 0: Minggu, 1: Bulan

    private val selectedMonth: StateFlow<SelectedMonth> = monthFilterViewModel.selectedMonth

    // --- INSIGHT STATE ---
    private val _insightState = MutableStateFlow<InsightUiState>(InsightUiState.Idle)
    val insightState: StateFlow<InsightUiState> = _insightState

    // Combine filterIndex + selectedMonth so that the query re-runs when either changes
    val categorySummaries: StateFlow<List<CategorySummary>> =
        combine(filterIndex, selectedMonth) { index, month -> Pair(index, month) }
            .flatMapLatest { (index, month) ->
                val start = if (index == 0) DateUtils.getStartOfWeek()
                            else DateUtils.getStartOfMonth(month.year, month.month)
                val end   = if (index == 0) DateUtils.getEndOfWeek()
                            else DateUtils.getEndOfMonth(month.year, month.month)
                transactionRepository.getExpenseByCategoryInRange(start, end)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Total income & outcome untuk bulan yang dipilih (dipakai kartu ringkasan mode Bulan)
    private fun monthlyTotal(type: TransactionType): StateFlow<Long> =
        selectedMonth.flatMapLatest { month ->
            transactionRepository.getTotalByTypeAndDateRange(
                type,
                DateUtils.getStartOfMonth(month.year, month.month),
                DateUtils.getEndOfMonth(month.year, month.month)
            )
        }.map { it ?: 0L }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /** Income & outcome 6 bulan terakhir sampai bulan yang dipilih (untuk grafik tren). */
    val monthlyTrend: StateFlow<List<PeriodTotal>> = selectedMonth.flatMapLatest { month ->
        val firstCal = java.util.Calendar.getInstance(DateUtils.localeID).apply {
            set(month.year, month.month, 1)
            add(java.util.Calendar.MONTH, -5)
        }
        val start = DateUtils.getStartOfMonth(firstCal.get(java.util.Calendar.YEAR), firstCal.get(java.util.Calendar.MONTH))
        val end = DateUtils.getEndOfMonth(month.year, month.month)
        transactionRepository.getByDateRange(start, end).map { transactions ->
            (0 until 6).map { i ->
                val cal = (firstCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, i) }
                val y = cal.get(java.util.Calendar.YEAR)
                val m = cal.get(java.util.Calendar.MONTH)
                val mStart = DateUtils.getStartOfMonth(y, m)
                val mEnd = DateUtils.getEndOfMonth(y, m)
                val inMonth = transactions.filter { it.date in mStart..mEnd }
                PeriodTotal(
                    label = java.text.SimpleDateFormat("MMM", DateUtils.localeID).format(cal.time),
                    income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    expense = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyIncome: StateFlow<Long> = monthlyTotal(TransactionType.INCOME)
    val monthlyExpense: StateFlow<Long> = monthlyTotal(TransactionType.EXPENSE)

    private val currentWeekTransactions =filterIndex.flatMapLatest { index ->
        if (index == 0) {
            transactionRepository.getByDateRange(DateUtils.getStartOfWeek(), DateUtils.getEndOfWeek())
        } else {
            flowOf(emptyList())
        }
    }

    val weeklyDailyIncome: StateFlow<List<Double>> = currentWeekTransactions.map { transactions ->
        calculateDailyTotals(transactions, TransactionType.INCOME)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0.0 })

    val weeklyDailyExpense: StateFlow<List<Double>> = currentWeekTransactions.map { transactions ->
        calculateDailyTotals(transactions, TransactionType.EXPENSE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0.0 })

    val weeklyTotalIncome: StateFlow<Long> = weeklyDailyIncome
        .map { it.sum().toLong() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val weeklyTotalExpense: StateFlow<Long> = weeklyDailyExpense
        .map { it.sum().toLong() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setFilter(index: Int) {
        filterIndex.value = index
    }

    fun generateInsight() {
        viewModelScope.launch {
            _insightState.value = InsightUiState.Loading

            val month = selectedMonth.value
            val monthLabel = DateUtils.formatMonthYear(month.year, month.month)

            // Hitung bulan lalu dari selectedMonth
            val cal = java.util.Calendar.getInstance(DateUtils.localeID)
            cal.set(month.year, month.month, 1)
            cal.add(java.util.Calendar.MONTH, -1)
            val prevMonth = SelectedMonth(
                year = cal.get(java.util.Calendar.YEAR),
                month = cal.get(java.util.Calendar.MONTH)
            )
            val prevMonthLabel = DateUtils.formatMonthYear(prevMonth.year, prevMonth.month)

            // Range bulan ini
            val start = DateUtils.getStartOfMonth(month.year, month.month)
            val end = DateUtils.getEndOfMonth(month.year, month.month)

            // Range bulan lalu
            val prevStart = DateUtils.getStartOfMonth(prevMonth.year, prevMonth.month)
            val prevEnd = DateUtils.getEndOfMonth(prevMonth.year, prevMonth.month)

            // Fetch data bulan ini
            val income = transactionRepository
                .getTotalByTypeAndDateRange(TransactionType.INCOME, start, end)
                .first() ?: 0L
            val expense = transactionRepository
                .getTotalByTypeAndDateRange(TransactionType.EXPENSE, start, end)
                .first() ?: 0L
            val balance = income - expense
            val savingRate = if (income > 0) ((income - expense) * 100.0 / income) else 0.0
            val categories = transactionRepository
                .getExpenseByCategoryInRange(start, end)
                .first()

            // Fetch data bulan lalu
            val prevIncome = transactionRepository
                .getTotalByTypeAndDateRange(TransactionType.INCOME, prevStart, prevEnd)
                .first() ?: 0L
            val prevExpense = transactionRepository
                .getTotalByTypeAndDateRange(TransactionType.EXPENSE, prevStart, prevEnd)
                .first() ?: 0L

            // Top 3 kategori pengeluaran
            val top3Categories = categories.take(3)
            val categoryBreakdown = if (top3Categories.isEmpty()) {
                "Belum ada pengeluaran per kategori bulan ini."
            } else {
                top3Categories.mapIndexed { i, summary ->
                    val pct = if (expense > 0) (summary.totalAmount * 100.0 / expense) else 0.0
                    "${i + 1}. ${summary.categoryName}: ${CurrencyFormatter.formatRupiah(summary.totalAmount)} (${String.format("%.1f", pct)}%)"
                }.joinToString("\n")
            }

            // Semua kategori untuk konteks lengkap
            val allCategoryBreakdown = if (categories.isEmpty()) {
                "Tidak ada."
            } else {
                categories.joinToString("\n") { summary ->
                    "- ${summary.categoryName}: ${CurrencyFormatter.formatRupiah(summary.totalAmount)}"
                }
            }

            val expenseChange = if (prevExpense > 0) {
                val pct = ((expense - prevExpense) * 100.0 / prevExpense)
                val direction = if (pct >= 0) "naik" else "turun"
                "Pengeluaran $direction ${String.format("%.1f", Math.abs(pct))}% dibanding bulan lalu ($prevMonthLabel: ${CurrencyFormatter.formatRupiah(prevExpense)})"
            } else {
                "Tidak ada data pengeluaran bulan lalu ($prevMonthLabel) untuk dibandingkan."
            }

            val prompt = """
                Analisis data keuangan berikut dan berikan insight menyeluruh.

                === BULAN INI: $monthLabel ===
                - Total Pemasukan : ${CurrencyFormatter.formatRupiah(income)}
                - Total Pengeluaran: ${CurrencyFormatter.formatRupiah(expense)}
                - Saldo            : ${CurrencyFormatter.formatRupiah(balance)}
                - Saving Rate      : ${String.format("%.1f", savingRate)}% dari pemasukan

                === PERBANDINGAN BULAN LALU ===
                - Pemasukan bulan lalu ($prevMonthLabel): ${CurrencyFormatter.formatRupiah(prevIncome)}
                - $expenseChange

                === TOP 3 KATEGORI PENGELUARAN ===
                $categoryBreakdown

                === SEMUA KATEGORI PENGELUARAN ===
                $allCategoryBreakdown

                Berikan analisis keuangan yang menyeluruh mencakup: ringkasan kondisi keuangan bulan ini, evaluasi kategori pengeluaran terbesar, perbandingan dengan bulan lalu, evaluasi saving rate, dan minimal 1-2 saran konkret yang bisa langsung diterapkan.
            """.trimIndent()

            val result = aiRepository.generateCashflowInsight(prompt)
            _insightState.value = result.fold(
                onSuccess = { InsightUiState.Success(it) },
                onFailure = { InsightUiState.Error(it.message ?: "Gagal mendapatkan insight dari AI") }
            )
        }
    }


    private fun calculateDailyTotals(
        transactions: List<com.finansio.app.data.local.entity.TransactionEntity>,
        type: TransactionType
    ): List<Double> {
        val dailyTotals = DoubleArray(7) { 0.0 }
        val filtered = transactions.filter { it.type == type }
        for (tx in filtered) {
            val calendar = java.util.Calendar.getInstance(DateUtils.localeID)
            calendar.timeInMillis = tx.date
            var dayIndex = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 2
            if (dayIndex < 0) dayIndex = 6
            dailyTotals[dayIndex] += tx.amount.toDouble()
        }
        return dailyTotals.toList()
    }
}

/** Total pemasukan & pengeluaran untuk satu periode (hari atau bulan) pada grafik kolom. */
data class PeriodTotal(val label: String, val income: Long, val expense: Long)
