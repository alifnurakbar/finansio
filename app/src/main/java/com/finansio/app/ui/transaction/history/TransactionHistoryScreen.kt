package com.finansio.app.ui.transaction.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finansio.app.ui.components.EmptyStateView
import com.finansio.app.ui.components.TransactionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: TransactionHistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransaction: (Int) -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val monthLabel by viewModel.monthLabel.collectAsState()
    val offset by viewModel.currentMonthOffset.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Month Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan Sebelumnya")
                }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = viewModel::nextMonth,
                    enabled = offset < 0
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan Berikutnya")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cari berdasarkan catatan atau kategori...") },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (transactions.isEmpty()) {
                EmptyStateView(
                    if (searchQuery.isNotEmpty()) "Pencarian tidak ditemukan" 
                    else "Belum ada transaksi di bulan ini"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
