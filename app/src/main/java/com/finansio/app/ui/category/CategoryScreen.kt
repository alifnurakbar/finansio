package com.finansio.app.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.ui.components.CategoryChip
import com.finansio.app.ui.components.FinansioTopBar

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel
) {
    val categories by viewModel.categories.collectAsState()
    
    var showForm by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CategoryEntity?>(null) }
    
    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            FinansioTopBar(title = "Kategori")
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    val incomes = categories.filter { it.type == TransactionType.INCOME }
                    val expenses = categories.filter { it.type == TransactionType.EXPENSE }
                    
                    item { 
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Income", style = MaterialTheme.typography.titleLarge) 
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(incomes) { cat ->
                        CategoryRow(
                            category = cat, 
                            onEdit = { 
                                selectedCategoryForEdit = cat
                                showForm = true
                            },
                            onDelete = { showDeleteConfirm = cat }
                        )
                    }
                    
                    item { 
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Outcome", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(expenses) { cat ->
                        CategoryRow(
                            category = cat, 
                            onEdit = { 
                                selectedCategoryForEdit = cat
                                showForm = true
                            },
                            onDelete = { showDeleteConfirm = cat }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.showAddCategoryEvent.collect {
            selectedCategoryForEdit = null
            showForm = true
        }
    }
    
    if (showForm) {
        CategoryFormDialog(
            initialCategory = selectedCategoryForEdit,
            onDismiss = { showForm = false },
            onSave = { 
                viewModel.saveCategory(it)
                showForm = false
            }
        )
    }
    
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Hapus Kategori") },
            text = { Text("Apakah kamu yakin ingin menghapus kategori '${showDeleteConfirm?.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(showDeleteConfirm!!)
                    showDeleteConfirm = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormDialog(
    initialCategory: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var type by remember { mutableStateOf(initialCategory?.type ?: TransactionType.EXPENSE) }
    var colorHex by remember { mutableStateOf(initialCategory?.colorHex ?: "#2196F3") }
    
    val colorOptions = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7", 
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", 
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39", 
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCategory == null) "Tambah Kategori" else "Edit Kategori") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Outcome") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Pilih Warna", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { colorHex = hex }
                                .then(
                                    if (colorHex == hex) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CategoryEntity(
                            id = initialCategory?.id ?: 0,
                            name = name,
                            type = type,
                            colorHex = colorHex
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun CategoryRow(category: CategoryEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CategoryChip(name = category.name, colorHex = category.colorHex)
        Row {
            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
