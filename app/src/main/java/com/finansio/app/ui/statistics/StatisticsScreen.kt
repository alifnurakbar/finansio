package com.finansio.app.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finansio.app.data.local.dao.CategorySummary
import com.finansio.app.ui.MonthFilterViewModel
import com.finansio.app.ui.components.FinansioCard
import com.finansio.app.ui.components.FinansioTopBar
import com.finansio.app.ui.components.MonthNavigatorBar
import com.finansio.app.ui.components.SegmentedPillFilter
import com.finansio.app.ui.theme.Coral
import com.finansio.app.ui.theme.Lavender
import com.finansio.app.ui.theme.Lime
import com.finansio.app.ui.theme.SurfaceRaised
import com.finansio.app.ui.theme.TextSecondary
import com.finansio.app.util.CurrencyFormatter
import com.finansio.app.util.DateUtils
import java.util.Calendar

// Warna tetap untuk jenis transaksi di semua grafik statistik
private val IncomeColor = Lime
private val ExpenseColor = Coral

private fun parseHex(hex: String?): Color =
    try { Color(android.graphics.Color.parseColor(hex ?: "#8E8F94")) } catch (e: Exception) { TextSecondary }

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    monthFilterViewModel: MonthFilterViewModel
) {
    val filterIndex by viewModel.filterIndex.collectAsState()
    val summaries by viewModel.categorySummaries.collectAsState()
    val selectedMonth by monthFilterViewModel.selectedMonth.collectAsState()
    val weeklyIncome by viewModel.weeklyDailyIncome.collectAsState()
    val weeklyExpense by viewModel.weeklyDailyExpense.collectAsState()
    val totalWeeklyIncome by viewModel.weeklyTotalIncome.collectAsState()
    val totalWeeklyExpense by viewModel.weeklyTotalExpense.collectAsState()
    val insightState by viewModel.insightState.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val trend by viewModel.monthlyTrend.collectAsState()

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            FinansioTopBar(title = "Statistik")

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SegmentedPillFilter(
                        options = listOf("Minggu", "Bulan"),
                        selectedIndex = filterIndex,
                        onOptionSelected = viewModel::setFilter
                    )
                }

                if (filterIndex == 0) {
                    // ---------------- MODE MINGGU ----------------
                    item {
                        SummaryRow(
                            incomeLabel = "Income minggu ini", income = totalWeeklyIncome,
                            expenseLabel = "Outcome minggu ini", expense = totalWeeklyExpense
                        )
                    }
                    item {
                        val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                        val data = days.mapIndexed { i, d ->
                            PeriodTotal(d, weeklyIncome.getOrElse(i) { 0.0 }.toLong(), weeklyExpense.getOrElse(i) { 0.0 }.toLong())
                        }
                        // Calendar: Minggu = 1, Senin = 2 → indeks 0 untuk Senin
                        val today = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
                        ChartCard(title = "Arus Kas Harian", subtitle = "Pemasukan dan pengeluaran per hari minggu ini") {
                            if (data.all { it.income == 0L && it.expense == 0L }) {
                                EmptyChart("Belum ada transaksi minggu ini")
                            } else {
                                GroupedColumnChart(
                                    data = data,
                                    incomeColor = IncomeColor,
                                    expenseColor = ExpenseColor,
                                    initialSelected = today
                                )
                            }
                        }
                    }
                } else {
                    // ---------------- MODE BULAN ----------------
                    item {
                        MonthNavigatorBar(
                            label = DateUtils.formatMonthYear(selectedMonth.year, selectedMonth.month),
                            onPrev = { monthFilterViewModel.prevMonth() },
                            onNext = { monthFilterViewModel.nextMonth() }
                        )
                    }
                    item {
                        BalanceHeroCard(
                            income = monthlyIncome,
                            expense = monthlyExpense,
                            previousExpense = trend.getOrNull(trend.size - 2)?.expense,
                            previousLabel = trend.getOrNull(trend.size - 2)?.label
                        )
                    }
                    item {
                        ChartCard(title = "Tren 6 Bulan", subtitle = "Perbandingan pemasukan dan pengeluaran") {
                            if (trend.all { it.income == 0L && it.expense == 0L }) {
                                EmptyChart("Belum ada transaksi dalam 6 bulan terakhir")
                            } else {
                                GroupedColumnChart(
                                    data = trend,
                                    incomeColor = IncomeColor,
                                    expenseColor = ExpenseColor,
                                    initialSelected = trend.lastIndex
                                )
                            }
                        }
                    }
                    item {
                        CategoryBreakdownCard(summaries = summaries)
                    }
                }

                item {
                    InsightCard(state = insightState, onGenerate = viewModel::generateInsight)
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    FinansioCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun EmptyChart(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceRaised),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun SummaryRow(incomeLabel: String, income: Long, expenseLabel: String, expense: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniStat(Icons.Rounded.ArrowDownward, IncomeColor, incomeLabel, income, Modifier.weight(1f))
        MiniStat(Icons.Rounded.ArrowUpward, ExpenseColor, expenseLabel, expense, Modifier.weight(1f))
    }
}

@Composable
private fun MiniStat(icon: ImageVector, accent: Color, label: String, value: Long, modifier: Modifier) {
    FinansioCard(modifier = modifier) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(
            CurrencyFormatter.formatRupiah(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BalanceHeroCard(income: Long, expense: Long, previousExpense: Long?, previousLabel: String?) {
    val balance = income - expense
    FinansioCard(modifier = Modifier.fillMaxWidth()) {
        Text("Saldo bulan ini", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(
            CurrencyFormatter.formatRupiah(balance),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (balance < 0) Coral else MaterialTheme.colorScheme.onSurface
        )

        // Perubahan pengeluaran dibanding bulan lalu
        if (previousExpense != null && previousExpense > 0 && previousLabel != null) {
            val change = (expense - previousExpense) * 100f / previousExpense
            val up = change > 0
            // Pengeluaran naik = kurang baik (coral), turun = baik (lime)
            val tone = if (up) Coral else Lime
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(tone.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (up) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Pengeluaran ${if (up) "naik" else "turun"} %.0f%% dari $previousLabel".format(kotlin.math.abs(change)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        // Batang rasio: seberapa besar pengeluaran dibanding pemasukan
        val ratio = if (income > 0) (expense.toFloat() / income).coerceIn(0f, 1f) else if (expense > 0) 1f else 0f
        Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(SurfaceRaised)) {
            if (ratio > 0f) {
                Box(Modifier.fillMaxHeight().weight(ratio).background(ExpenseColor))
            }
            if (ratio < 1f) {
                if (ratio > 0f) Spacer(Modifier.width(2.dp))
                Box(Modifier.fillMaxHeight().weight(1f - ratio).background(IncomeColor))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                LegendDot(IncomeColor, "Income")
                Text(CurrencyFormatter.formatRupiah(income), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                LegendDot(ExpenseColor, "Outcome")
                Text(CurrencyFormatter.formatRupiah(expense), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        if (income > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Kamu memakai %.0f%% dari pemasukan bulan ini".format(expense * 100f / income),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(summaries: List<CategorySummary>) {
    ChartCard(title = "Pengeluaran per Kategori", subtitle = "Ketuk donat atau daftar untuk menyorot") {
        if (summaries.isEmpty()) {
            EmptyChart("Belum ada pengeluaran di bulan ini")
            return@ChartCard
        }
        var selected by remember(summaries) { mutableStateOf<Int?>(null) }
        val slices = summaries.map { DonutSlice(it.categoryName, it.totalAmount, parseHex(it.colorHex)) }
        val total = slices.sumOf { it.value }.coerceAtLeast(1L)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DonutChart(slices = slices, selectedIndex = selected, onSelect = { selected = it })
        }
        Spacer(Modifier.height(20.dp))

        // Legenda: nama + nominal + persen, dengan batang tipis berwarna kategori
        slices.forEachIndexed { i, slice ->
            val percent = slice.value * 100f / total
            val dimmed = selected != null && selected != i
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected == i) SurfaceRaised else Color.Transparent)
                    .clickable { selected = if (selected == i) null else i }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color.copy(alpha = if (dimmed) 0.3f else 1f)))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        slice.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        color = if (dimmed) TextSecondary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        CurrencyFormatter.formatRupiah(slice.value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dimmed) TextSecondary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "  %.0f%%".format(percent),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.widthIn(min = 40.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(SurfaceRaised)) {
                    Box(
                        Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().clip(RoundedCornerShape(50))
                            .background(slice.color.copy(alpha = if (dimmed) 0.3f else 1f))
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightCard(state: InsightUiState, onGenerate: () -> Unit) {
    FinansioCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Lavender.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Lavender, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("AI Cashflow Insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Analisis otomatis pola keuanganmu", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(14.dp))

        when (state) {
            is InsightUiState.Success -> {
                Text(
                    state.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceRaised).padding(14.dp)
                )
                Spacer(Modifier.height(12.dp))
            }
            is InsightUiState.Error -> {
                Text(state.message, style = MaterialTheme.typography.bodySmall, color = Coral)
                Spacer(Modifier.height(12.dp))
            }
            else -> {}
        }

        Button(
            onClick = onGenerate,
            enabled = state !is InsightUiState.Loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Lavender, contentColor = MaterialTheme.colorScheme.background)
        ) {
            if (state is InsightUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.background)
                Spacer(Modifier.width(8.dp))
                Text("Menganalisis…")
            } else {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (state is InsightUiState.Success) "Perbarui Insight" else "Buat Insight", fontWeight = FontWeight.Bold)
            }
        }
    }
}
