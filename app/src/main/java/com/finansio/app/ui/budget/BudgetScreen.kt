package com.finansio.app.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.repository.BudgetLevel
import com.finansio.app.data.repository.BudgetProgress
import com.finansio.app.ui.MonthFilterViewModel
import com.finansio.app.ui.components.EmptyStateView
import com.finansio.app.ui.components.FinansioCard
import com.finansio.app.ui.components.FinansioTopBar
import com.finansio.app.ui.components.MonthNavigatorBar
import com.finansio.app.ui.theme.Coral
import com.finansio.app.ui.theme.Lime
import com.finansio.app.ui.theme.TextSecondary
import com.finansio.app.ui.theme.Yellow
import com.finansio.app.util.CurrencyFormatter
import com.finansio.app.util.CurrencyVisualTransformation
import com.finansio.app.util.DateUtils

private fun BudgetLevel.color(): Color = when (this) {
    BudgetLevel.SAFE -> Lime
    BudgetLevel.WARNING -> Yellow
    BudgetLevel.OVER -> Coral
}

private fun parseColor(hex: String): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { TextSecondary }

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    monthFilterViewModel: MonthFilterViewModel
) {
    val budgets by viewModel.budgets.collectAsState()
    val available by viewModel.availableCategories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BudgetProgress?>(null) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FinansioTopBar(title = "Anggaran")

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MonthNavigatorBar(
                        label = DateUtils.formatMonthYear(selectedMonth.year, selectedMonth.month),
                        onPrev = { monthFilterViewModel.prevMonth() },
                        onNext = { monthFilterViewModel.nextMonth() }
                    )
                }

                if (budgets.isNotEmpty()) {
                    item { BudgetSummaryCard(budgets) }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anggaran per Kategori", style = MaterialTheme.typography.titleMedium)
                        if (available.isNotEmpty()) {
                            FilledTonalButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tambah")
                            }
                        }
                    }
                }

                if (budgets.isEmpty()) {
                    item {
                        EmptyStateView(
                            "Belum ada anggaran.\nTekan \"Tambah\" untuk membatasi pengeluaran per kategori."
                        )
                    }
                } else {
                    items(budgets, key = { it.category.id }) { progress ->
                        BudgetCard(progress, onClick = { editing = progress })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        BudgetFormDialog(
            title = "Tambah Anggaran",
            categories = available,
            initialCategory = null,
            initialLimit = null,
            onDismiss = { showAddDialog = false },
            onSave = { categoryId, limit ->
                viewModel.saveBudget(categoryId, limit)
                showAddDialog = false
            },
            onDelete = null
        )
    }

    editing?.let { progress ->
        BudgetFormDialog(
            title = "Ubah Anggaran",
            categories = listOf(progress.category),
            initialCategory = progress.category,
            initialLimit = progress.limit,
            onDismiss = { editing = null },
            onSave = { categoryId, limit ->
                viewModel.saveBudget(categoryId, limit)
                editing = null
            },
            onDelete = {
                viewModel.deleteBudget(progress.category.id)
                editing = null
            }
        )
    }
}

@Composable
private fun BudgetSummaryCard(budgets: List<BudgetProgress>) {
    val totalLimit = budgets.sumOf { it.limit }
    val totalSpent = budgets.sumOf { it.spent }
    val percent = if (totalLimit > 0) (totalSpent * 100 / totalLimit).toInt() else 0
    val level = when {
        percent >= 100 -> BudgetLevel.OVER
        percent >= 80 -> BudgetLevel.WARNING
        else -> BudgetLevel.SAFE
    }
    FinansioCard(modifier = Modifier.fillMaxWidth()) {
        Text("Total Terpakai", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                CurrencyFormatter.formatRupiah(totalSpent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "  / ${CurrencyFormatter.formatRupiah(totalLimit)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        BudgetProgressBar(percent, level.color())
        Spacer(Modifier.height(8.dp))
        val remaining = totalLimit - totalSpent
        Text(
            if (remaining >= 0) "Sisa ${CurrencyFormatter.formatRupiah(remaining)} dari ${budgets.size} anggaran"
            else "Lebih ${CurrencyFormatter.formatRupiah(-remaining)} dari total anggaran",
            style = MaterialTheme.typography.bodySmall,
            color = if (remaining >= 0) TextSecondary else Coral
        )
    }
}

@Composable
private fun BudgetCard(progress: BudgetProgress, onClick: () -> Unit) {
    val levelColor = progress.level.color()
    FinansioCard(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(12.dp).clip(CircleShape)
                    .background(parseColor(progress.category.colorHex))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                progress.category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = when (progress.level) {
                    BudgetLevel.SAFE -> Icons.Rounded.CheckCircle
                    BudgetLevel.WARNING -> Icons.Rounded.Warning
                    BudgetLevel.OVER -> Icons.Rounded.Error
                },
                contentDescription = null,
                tint = levelColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("${progress.percent}%", color = levelColor, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        BudgetProgressBar(progress.percent, levelColor)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${CurrencyFormatter.formatRupiah(progress.spent)} dari ${CurrencyFormatter.formatRupiah(progress.limit)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                if (progress.remaining >= 0) "Sisa ${CurrencyFormatter.formatRupiah(progress.remaining)}"
                else "Lebih ${CurrencyFormatter.formatRupiah(-progress.remaining)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (progress.remaining >= 0) TextSecondary else Coral,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BudgetProgressBar(percent: Int, color: Color) {
    LinearProgressIndicator(
        progress = { (percent / 100f).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

@Composable
private fun BudgetFormDialog(
    title: String,
    categories: List<CategoryEntity>,
    initialCategory: CategoryEntity?,
    initialLimit: Long?,
    onDismiss: () -> Unit,
    onSave: (Int, Long) -> Unit,
    onDelete: (() -> Unit)?
) {
    var selected by remember { mutableStateOf(initialCategory ?: categories.firstOrNull()) }
    var amountText by remember { mutableStateOf(initialLimit?.toString() ?: "") }
    val amount = amountText.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (initialCategory == null) {
                    Text("Kategori pengeluaran", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Column(modifier = Modifier.heightIn(max = 220.dp)) {
                        LazyColumn {
                            items(categories, key = { it.id }) { category ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selected = category }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selected?.id == category.id, onClick = { selected = category })
                                    Box(
                                        Modifier.size(10.dp).clip(CircleShape)
                                            .background(parseColor(category.colorHex))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(category.name)
                                }
                            }
                        }
                    }
                } else {
                    Text("Kategori: ${initialCategory.name}", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() }.take(13) },
                    label = { Text("Batas per bulan (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CurrencyVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Anggaran ini berlaku otomatis setiap bulan sampai Anda ubah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                        Text("Hapus anggaran ini", color = Coral)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let { onSave(it.id, amount) } },
                enabled = selected != null && amount > 0
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
