package com.finansio.app.ui.transaction.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.finansio.app.data.budget.BudgetAlert
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToHistory: () -> Unit,
    userName: String = "",
    budgetAlerts: List<BudgetAlert> = emptyList(),
    onDismissAlert: (BudgetAlert) -> Unit = {},
    onAlertClick: () -> Unit = {},
    onOpenChat: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val income by viewModel.totalIncome.collectAsState()
    val expense by viewModel.totalExpense.collectAsState()
    val monthlyIncomeData by viewModel.monthlyIncomeData.collectAsState()
    val monthlyExpenseData by viewModel.monthlyExpenseData.collectAsState()
    val selectedMonth by monthFilterViewModel.selectedMonth.collectAsState()

    Scaffold(
        floatingActionButton = {
            // Tombol Chat Asisten AI
            ExtendedFloatingActionButton(
                onClick = onOpenChat,
                icon = { Icon(androidx.compose.material.icons.Icons.Rounded.AutoAwesome, contentDescription = null) },
                text = { Text("Tanya AI", fontWeight = FontWeight.Bold) },
                containerColor = com.finansio.app.ui.theme.Lavender,
                contentColor = com.finansio.app.ui.theme.Background,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = if (userName.isBlank()) "Halo! 👋" else "Halo, $userName! 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Yuk cek keuanganmu hari ini",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            budgetAlerts.forEach { alert ->
                BudgetAlertBanner(alert, onClick = onAlertClick, onDismiss = { onDismissAlert(alert) })
                Spacer(modifier = Modifier.height(12.dp))
            }

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
                EmptyStateView("Belum ada catatan bulan ini â yuk mulai catat")
            } else {
                transactions.forEach { tx ->
                        Box(modifier = Modifier.clickable { onNavigateToTransaction(tx.id) }) {
                            TransactionRow(transaction = tx, categoryColorHex = null)
                        }
                }
                // Ruang supaya transaksi terakhir tidak tertutup tombol Tanya AI
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/** Banner peringatan anggaran dengan saran AI. Kuning = mendekati batas, merah = terlampaui. */
@Composable
private fun BudgetAlertBanner(alert: BudgetAlert, onClick: () -> Unit, onDismiss: () -> Unit) {
    val accent = if (alert.isOver) com.finansio.app.ui.theme.Coral else com.finansio.app.ui.theme.Yellow
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (alert.isOver) androidx.compose.material.icons.Icons.Rounded.Error
                else androidx.compose.material.icons.Icons.Rounded.Warning,
                contentDescription = null,
                tint = accent
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (alert.isOver) "Anggaran ${alert.categoryName} terlampaui (${alert.percent}%)"
                    else "Anggaran ${alert.categoryName} sudah ${alert.percent}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        androidx.compose.material.icons.Icons.Rounded.AutoAwesome,
                        contentDescription = "Saran AI",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(alert.message, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    androidx.compose.material.icons.Icons.Rounded.Close,
                    contentDescription = "Tutup",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
