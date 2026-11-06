package com.finansio.app.ui.transaction.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.finansio.app.ui.components.EmptyStateView
import com.finansio.app.ui.components.FinansioCard
import com.finansio.app.ui.components.MonthNavigatorBar
import com.finansio.app.ui.components.SegmentedPillFilter
import com.finansio.app.ui.components.TransactionRow
import com.finansio.app.ui.MonthFilterViewModel
import com.finansio.app.util.CurrencyFormatter
import com.finansio.app.util.DateUtils
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.Line

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    monthFilterViewModel: MonthFilterViewModel,
    onNavigateToTransaction: (Int) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val income by viewModel.totalIncome.collectAsState()
    val expense by viewModel.totalExpense.collectAsState()
    val monthlyIncomeData by viewModel.monthlyIncomeData.collectAsState()
    val monthlyExpenseData by viewModel.monthlyExpenseData.collectAsState()
    val selectedMonth by monthFilterViewModel.selectedMonth.collectAsState()

    Scaffold() { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding)
                .padding(16.dp)
        ) {
            FinansioCard(modifier = Modifier.fillMaxWidth()) {
                MonthNavigatorBar(
                    label = DateUtils.formatMonthYear(selectedMonth.year, selectedMonth.month),
                    onPrev = { monthFilterViewModel.prevMonth() },
                    onNext = { monthFilterViewModel.nextMonth() }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatRupiah(balance),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                LineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    data = listOf(
                        Line(
                            label = "Income",
                            values = monthlyIncomeData,
                            color = SolidColor(MaterialTheme.colorScheme.secondary),
                            curvedEdges = true
                        ),
                        Line(
                            label = "Expense",
                            values = monthlyExpenseData,
                            color = SolidColor(MaterialTheme.colorScheme.error),
                            curvedEdges = true
                        )
                    ),
                    labelHelperProperties = LabelHelperProperties(
                        textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                    ),
                    indicatorProperties = HorizontalIndicatorProperties(
                        textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FinansioCard(modifier = Modifier.weight(1f)) {
                    Text("Income", style = MaterialTheme.typography.labelMedium)
                    Text(
                        CurrencyFormatter.formatRupiah(income),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                FinansioCard(modifier = Modifier.weight(1f)) {
                    Text("Outcome", style = MaterialTheme.typography.labelMedium)
                    Text(
                        CurrencyFormatter.formatRupiah(expense),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onNavigateToHistory) {
                    Text("Lihat semua")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                EmptyStateView("Belum ada catatan bulan ini — yuk mulai catat")
            } else {
                LazyColumn {
                    items(transactions) { tx ->
                        Box(modifier = Modifier.clickable { onNavigateToTransaction(tx.id) }) {
                            TransactionRow(transaction = tx, categoryColorHex = null)
                        }
                    }
                }
            }
        }
    }
}
