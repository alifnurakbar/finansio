package com.finansio.app.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.finansio.app.ui.components.CategoryChip
import com.finansio.app.ui.components.EmptyStateView
import com.finansio.app.ui.components.FinansioCard
import com.finansio.app.ui.components.FinansioTopBar
import com.finansio.app.ui.components.MonthNavigatorBar
import com.finansio.app.ui.components.SegmentedPillFilter
import com.finansio.app.ui.MonthFilterViewModel
import com.finansio.app.util.CurrencyFormatter
import com.finansio.app.util.DateUtils
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.Pie
import ir.ehsannarmani.compose_charts.models.LabelProperties

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

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            FinansioTopBar(title = "Statistik")
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    SegmentedPillFilter(
                        options = listOf("Minggu", "Bulan"),
                        selectedIndex = filterIndex,
                        onOptionSelected = viewModel::setFilter
                    )

                    // Month navigator: only shown in Bulan mode
                    if (filterIndex == 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MonthNavigatorBar(
                            label = DateUtils.formatMonthYear(selectedMonth.year, selectedMonth.month),
                            onPrev = { monthFilterViewModel.prevMonth() },
                            onNext = { monthFilterViewModel.nextMonth() }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (filterIndex == 0) { // Minggu
                    item {
                        FinansioCard(modifier = Modifier.fillMaxWidth()) {
                            ir.ehsannarmani.compose_charts.ColumnChart(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                data = listOf(
                                    ir.ehsannarmani.compose_charts.models.Bars(
                                        label = "Sen",
                                        values = listOf(
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyIncome[0], color = SolidColor(MaterialTheme.colorScheme.secondary), label = "Income"),
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyExpense[0], color = SolidColor(MaterialTheme.colorScheme.error), label = "Outcome")
                                        )
                                    ),
                                    ir.ehsannarmani.compose_charts.models.Bars(
                                        label = "Sel",
                                        values = listOf(
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyIncome[1], color = SolidColor(MaterialTheme.colorScheme.secondary), label = "Income"),
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyExpense[1], color = SolidColor(MaterialTheme.colorScheme.error), label = "Outcome")
                                        )
                                    ),
                                    ir.ehsannarmani.compose_charts.models.Bars(
                                        label = "Rab",
                                        values = listOf(
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyIncome[2], color = SolidColor(MaterialTheme.colorScheme.secondary), label = "Income"),
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyExpense[2], color = SolidColor(MaterialTheme.colorScheme.error), label = "Outcome")
                                        )
                                    ),
                                    ir.ehsannarmani.compose_charts.models.Bars(
                                        label = "Kam",
                                        values = listOf(
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyIncome[3], color = SolidColor(MaterialTheme.colorScheme.secondary), label = "Income"),
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyExpense[3], color = SolidColor(MaterialTheme.colorScheme.error), label = "Outcome")
                                        )
                                    ),
                                    ir.ehsannarmani.compose_charts.models.Bars(
                                        label = "Jum",
                                        values = listOf(
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyIncome[4], color = SolidColor(MaterialTheme.colorScheme.secondary), label = "Income"),
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyExpense[4], color = SolidColor(MaterialTheme.colorScheme.error), label = "Outcome")
                                        )
                                    ),
                                    ir.ehsannarmani.compose_charts.models.Bars(
                                        label = "Sab",
                                        values = listOf(
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyIncome[5], color = SolidColor(MaterialTheme.colorScheme.secondary), label = "Income"),
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyExpense[5], color = SolidColor(MaterialTheme.colorScheme.error), label = "Outcome")
                                        )
                                    ),
                                    ir.ehsannarmani.compose_charts.models.Bars(
                                        label = "Min",
                                        values = listOf(
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyIncome[6], color = SolidColor(MaterialTheme.colorScheme.secondary), label = "Income"),
                                            ir.ehsannarmani.compose_charts.models.Bars.Data(value = weeklyExpense[6], color = SolidColor(MaterialTheme.colorScheme.error), label = "Outcome")
                                        )
                                    )
                                ),
                                labelProperties = ir.ehsannarmani.compose_charts.models.LabelProperties(
                                    enabled = true,
                                    textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                ),
                                labelHelperProperties = LabelHelperProperties(
                                    textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                ),
                                indicatorProperties = HorizontalIndicatorProperties(
                                    textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            FinansioCard(modifier = Modifier.weight(1f)) {
                                Text("Income (Mg)", style = MaterialTheme.typography.labelMedium)
                                Text(CurrencyFormatter.formatRupiah(totalWeeklyIncome), style = MaterialTheme.typography.titleLarge)
                            }
                            FinansioCard(modifier = Modifier.weight(1f)) {
                                Text("Outcome (Mg)", style = MaterialTheme.typography.labelMedium)
                                Text(CurrencyFormatter.formatRupiah(totalWeeklyExpense), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                } else if (summaries.isEmpty()) {
                    item {
                        EmptyStateView("Belum ada pengeluaran di periode ini")
                    }
                } else {
                    item {
                        FinansioCard(modifier = Modifier.fillMaxWidth()) {
                            val chartData = summaries.map { 
                                Pie(
                                    label = it.categoryName, 
                                    data = it.totalAmount.toDouble(),
                                    color = try {
                                        Color(android.graphics.Color.parseColor(it.colorHex ?: "#8E8F94"))
                                    } catch(e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                            
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                                LocalTextStyle provides MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            ) {
                                PieChart(
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    data = chartData,
                                    labelHelperProperties = LabelHelperProperties(
                                        textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                    ),
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    items(summaries) { summary ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CategoryChip(name = summary.categoryName, colorHex = summary.colorHex ?: "#8E8F94")
                            Text(
                                text = CurrencyFormatter.formatRupiah(summary.totalAmount),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    FinansioCard(modifier = Modifier.fillMaxWidth()) {
                        Text("AI Cashflow Insight", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Analisis otomatis pola keuanganmu bulan ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { viewModel.generateInsight() },
                            enabled = insightState !is InsightUiState.Loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (insightState is InsightUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menganalisis...")
                            } else {
                                Text(
                                    if (insightState is InsightUiState.Success) "Perbarui Insight"
                                    else "✨ Buat Insight"
                                )
                            }
                        }
                        
                        when (val state = insightState) {
                            is InsightUiState.Success -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = state.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            is InsightUiState.Error -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
