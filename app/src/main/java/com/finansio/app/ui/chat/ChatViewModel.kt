package com.finansio.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansio.app.data.chat.FinanceContextBuilder
import com.finansio.app.data.remote.GroqMessage
import com.finansio.app.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
    val isError: Boolean = false
)

/**
 * Riwayat chat hanya disimpan di memori ViewModel ini. ViewModel dibuat per kunjungan
 * halaman chat, jadi riwayat hilang saat halaman ditutup (sesuai keputusan privasi).
 */
class ChatViewModel(
    private val aiRepository: AiRepository,
    private val contextBuilder: FinanceContextBuilder
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val suggestions = listOf(
        "Bulan ini aku paling boros di mana?",
        "Bagaimana kondisi keuanganku bulan ini?",
        "Apakah anggaranku aman sampai akhir bulan?",
        "Kasih tips hemat dari data pengeluaranku"
    )

    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty() || _isLoading.value) return
        // Pesan error sebelumnya tidak ikut dikirim sebagai riwayat
        _messages.update { list -> list.filterNot { it.isError } + ChatMessage(fromUser = true, text = question) }
        requestAnswer()
    }

    /** Ulangi pertanyaan terakhir setelah error. */
    fun retry() {
        _messages.update { list -> list.filterNot { it.isError } }
        if (_messages.value.lastOrNull()?.fromUser == true) requestAnswer()
    }

    private fun requestAnswer() {
        viewModelScope.launch {
            _isLoading.value = true
            // Data diambil ulang setiap pertanyaan supaya selalu sesuai transaksi terbaru
            val financeContext = contextBuilder.build()
            val history = _messages.value.map {
                GroqMessage(role = if (it.fromUser) "user" else "assistant", content = it.text)
            }
            aiRepository.chat(history, financeContext)
                .onSuccess { answer ->
                    _messages.update { it + ChatMessage(fromUser = false, text = answer) }
                }
                .onFailure { e ->
                    _messages.update {
                        it + ChatMessage(fromUser = false, text = e.message ?: "Gagal menghubungi AI.", isError = true)
                    }
                }
            _isLoading.value = false
        }
    }
}
