package com.finansio.app.ui.transaction.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.data.repository.CategoryRepository
import com.finansio.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

/** State dan aksi form transaksi (muat, simpan, hapus). */
class TransactionFormViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val amount = MutableStateFlow("")
    val type = MutableStateFlow(TransactionType.EXPENSE)
    val note = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val date = MutableStateFlow(System.currentTimeMillis())
    
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isEditing = MutableStateFlow(false)
    private var currentTransactionId: Int? = null

    fun loadTransaction(id: Int) {
        if (id == -1) {
            reset()
            return
        }
        viewModelScope.launch {
            val tx = transactionRepository.getById(id)
            if (tx != null) {
                currentTransactionId = tx.id
                date.value = tx.date
                amount.value = tx.amount.toString()
                type.value = tx.type
                note.value = tx.note ?: ""
                isEditing.value = true
                
                selectedCategory.value = categories.value.find { it.id == tx.categoryId } ?: categoryRepository.getAll().stateIn(viewModelScope).value.find { it.id == tx.categoryId }
            }
        }
    }

    fun reset() {
        amount.value = ""
        note.value = ""
        selectedCategory.value = null
        currentTransactionId = null
        date.value = System.currentTimeMillis()
        isEditing.value = false
        type.value = TransactionType.EXPENSE
    }

    fun setAmount(value: String) { amount.value = value.filter { it.isDigit() } }
    fun setType(newType: TransactionType) { 
        if (type.value != newType) {
            type.value = newType
            selectedCategory.value = null
        }
    }
    fun setNote(value: String) { note.value = value }
    fun selectCategory(category: CategoryEntity) { selectedCategory.value = category }
    fun setDate(value: Long) { date.value = value }
    
    fun saveTransaction(onComplete: () -> Unit) {
        val category = selectedCategory.value ?: return
        val currentAmount = amount.value.toLongOrNull() ?: return
        
        viewModelScope.launch {
            val transaction = TransactionEntity(
                id = currentTransactionId ?: 0,
                amount = currentAmount,
                type = type.value,
                categoryId = category.id,
                categoryName = category.name,
                note = note.value.ifBlank { null },
                date = date.value
            )
            
            if (currentTransactionId != null) {
                transactionRepository.update(transaction)
            } else {
                transactionRepository.insert(transaction)
            }
            
            reset()
            onComplete()
        }
    }

    fun deleteTransaction(onComplete: () -> Unit) {
        val id = currentTransactionId ?: return
        viewModelScope.launch {
            val existing = transactionRepository.getById(id)
            if (existing != null) {
                transactionRepository.delete(existing)
            }
            reset()
            onComplete()
        }
    }
}
