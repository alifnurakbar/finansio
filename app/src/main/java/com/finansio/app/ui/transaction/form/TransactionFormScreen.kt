package com.finansio.app.ui.transaction.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.ui.components.CategoryChip
import com.finansio.app.ui.components.FinansioTopBar

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextAlign
import com.finansio.app.util.CurrencyVisualTransformation
import com.finansio.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    viewModel: TransactionFormViewModel,
    onBack: () -> Unit
) {
    val amount by viewModel.amount.collectAsState()
    val type by viewModel.type.collectAsState()
    val note by viewModel.note.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()

    val typeOptions = listOf(TransactionType.EXPENSE, TransactionType.INCOME)

    Scaffold() { padding ->
        Column(
            modifier = Modifier
                .padding(paddingValues = padding)
                .fillMaxSize()
        ) {
            FinansioTopBar(
                title = if (isEditing) "Edit Transaksi" else "Catat Transaksi",
                onBackClick = onBack
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    typeOptions.forEach { option ->
                        val isSelected = type == option
                        val bgColor = if (isSelected) {
                            if (option == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        } else Color.Transparent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(bgColor)
                                .clickable { viewModel.setType(option) }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = if (option == TransactionType.INCOME) "Income" else "Outcome",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = viewModel::setAmount,
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CurrencyVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pilih Kategori", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filteredCategories = categories.filter { it.type == type }
                    filteredCategories.forEach { cat ->
                        val isCatSelected = selectedCategory?.id == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.selectCategory(cat) }
                                .then(
                                    if (isCatSelected) Modifier.border(
                                        2.dp,
                                        Color.White,
                                        RoundedCornerShape(50)
                                    )
                                    else Modifier
                                )
                        ) {
                            CategoryChip(name = cat.name, colorHex = cat.colorHex)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val date by viewModel.date.collectAsState()
                val showDatePicker = remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker.value = true }) {
                    OutlinedTextField(
                        value = DateUtils.formatDisplayDate(date),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tanggal") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (showDatePicker.value) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker.value = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                                showDatePicker.value = false
                            }) {
                                Text("Pilih")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker.value = false }) {
                                Text("Batal")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = viewModel::setNote,
                    label = { Text("Catatan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isEditing) {
                        OutlinedButton(
                            onClick = { viewModel.deleteTransaction(onBack) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hapus")
                        }
                    }

                    Button(
                        onClick = { viewModel.saveTransaction(onBack) },
                        modifier = Modifier
                            .weight(if (isEditing) 1f else 2f)
                            .height(56.dp),
                        enabled = amount.isNotBlank() && selectedCategory != null
                    ) {
                        Text(if (isEditing) "Simpan Perubahan" else "Simpan Transaksi")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
