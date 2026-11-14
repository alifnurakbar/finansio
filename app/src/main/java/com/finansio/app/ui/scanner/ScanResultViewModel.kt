package com.finansio.app.ui.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.data.repository.AiRepository
import com.finansio.app.data.repository.CategoryRepository
import com.finansio.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** Alur scan struk dan suara: OCR, kirim ke AI, cocokkan kategori, lalu simpan sebagai transaksi. */
sealed class ScanUiState {
    object Idle : ScanUiState()
    object Scanning : ScanUiState()
    data class OcrProcessing(val rawText: String = "") : ScanUiState()
    data class Parsing(val rawText: String) : ScanUiState()
    data class Success(val items: List<ScannedItemUiModel>) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class ScanResultViewModel(
    private val aiRepository: AiRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState

    private val _isVoiceMode = MutableStateFlow(false)
    val isVoiceMode: StateFlow<Boolean> = _isVoiceMode

    // Ambil kategori langsung dari database saat dibutuhkan. Sebelumnya memakai
    // stateIn(WhileSubscribed) yang tidak pernah di-collect, sehingga daftarnya selalu kosong.
    private suspend fun loadCategories(): List<CategoryEntity> = categoryRepository.getAll().first()

    // Kategori cadangan bila AI tidak yakin: pengeluaran -> "Belanja", pemasukan -> "Lainnya".
    private fun fallbackCategory(categories: List<CategoryEntity>, type: TransactionType): CategoryEntity? {
        val name = if (type == TransactionType.EXPENSE) "Belanja" else "Lainnya"
        return categories.find { it.type == type && it.name.equals(name, ignoreCase = true) }
            ?: categories.firstOrNull { it.type == type }
    }

    fun processScannedImage(context: Context, imageUri: Uri) {
        _isVoiceMode.value = false
        _scanState.value = ScanUiState.Idle
        viewModelScope.launch {
            try {
                _scanState.value = ScanUiState.OcrProcessing()
                val rawText = ReceiptScannerHelper.recognizeText(context, imageUri)
                
                if (rawText.isBlank()) {
                    _scanState.value = ScanUiState.Error("Tidak ada teks yang terbaca dari gambar.")
                    return@launch
                }

                _scanState.value = ScanUiState.Parsing(rawText)

                val currentCategories = loadCategories()
                val result = aiRepository.parseReceiptText(rawText, currentCategories)

                result.onSuccess { parseResult ->
                    val uiItems = parseResult.items.map { item ->
                        val matchedCategory = currentCategories.find { 
                            it.name.equals(item.suggestedCategory, ignoreCase = true) 
                        } ?: fallbackCategory(currentCategories, TransactionType.EXPENSE)
                        
                        val dateMillis = parseDateToMillis(parseResult.date) ?: System.currentTimeMillis()
                        
                        ScannedItemUiModel(
                            name = item.name,
                            price = item.price,
                            quantity = item.quantity,
                            categoryId = matchedCategory?.id,
                            categoryName = matchedCategory?.name ?: "Lainnya",
                            note = "${item.name} (${item.quantity}x) at ${parseResult.merchantName ?: "Toko"}",
                            date = dateMillis
                        )
                    }
                    _scanState.value = ScanUiState.Success(uiItems)
                }.onFailure {
                    _scanState.value = ScanUiState.Error(it.message ?: "Gagal memproses struk dengan AI.")
                }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Terjadi kesalahan yang tidak terduga.")
            }
        }
    }

    fun processVoiceText(voiceText: String) {
        _isVoiceMode.value = true
        _scanState.value = ScanUiState.Idle
        viewModelScope.launch {
            try {
                if (voiceText.isBlank()) {
                    _scanState.value = ScanUiState.Error("Tidak ada suara yang terekam.")
                    return@launch
                }

                _scanState.value = ScanUiState.Parsing(voiceText)

                val currentCategories = loadCategories()
                val result = aiRepository.parseVoiceText(voiceText, currentCategories)

                result.onSuccess { parseResult ->
                    val uiItems = parseResult.items.map { item ->
                        val itemType = if (item.suggestedType?.equals("INCOME", ignoreCase = true) == true) {
                            TransactionType.INCOME
                        } else {
                            TransactionType.EXPENSE
                        }
                        
                        val matchedCategory = currentCategories.find { 
                            it.name.equals(item.suggestedCategory, ignoreCase = true) && it.type == itemType
                        } ?: fallbackCategory(currentCategories, itemType)
                        
                        // Untuk voice, selalu pakai waktu saat ini (Groq tidak tahu tanggal hari ini)
                        val dateMillis = System.currentTimeMillis()
                        
                        ScannedItemUiModel(
                            name = item.name,
                            price = item.price,
                            quantity = item.quantity,
                            categoryId = matchedCategory?.id,
                            categoryName = matchedCategory?.name ?: "Lainnya",
                            note = "${item.name} (${item.quantity}x) (dari suara)",
                            date = dateMillis,
                            type = itemType
                        )
                    }
                    _scanState.value = ScanUiState.Success(uiItems)
                }.onFailure {
                    _scanState.value = ScanUiState.Error(it.message ?: "Gagal memproses suara dengan AI.")
                }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Terjadi kesalahan yang tidak terduga.")
            }
        }
    }

    fun removeItem(id: String) {
        val currentState = _scanState.value
        if (currentState is ScanUiState.Success) {
            val newItems = currentState.items.filter { it.id != id }
            _scanState.value = ScanUiState.Success(newItems)
        }
    }

    fun updateItem(updated: ScannedItemUiModel) {
        val currentState = _scanState.value
        if (currentState is ScanUiState.Success) {
            val newItems = currentState.items.map { if (it.id == updated.id) updated else it }
            _scanState.value = ScanUiState.Success(newItems)
        }
    }

    fun saveAllTransactions(onComplete: () -> Unit) {
        val currentState = _scanState.value
        if (currentState is ScanUiState.Success) {
            viewModelScope.launch {
                currentState.items.forEach { item ->
                    val transaction = TransactionEntity(
                        amount = item.price * item.quantity,
                        type = item.type,
                        categoryId = item.categoryId,
                        categoryName = item.categoryName,
                        note = item.note,
                        date = item.date
                    )
                    transactionRepository.insert(transaction)
                }
                _scanState.value = ScanUiState.Idle
                _isVoiceMode.value = false
                onComplete()
            }
        }
    }

    private fun parseDateToMillis(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}
