package com.finansio.app.data.chat

import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.data.repository.BudgetRepository
import com.finansio.app.data.repository.TransactionRepository
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * Menyusun ringkasan data keuangan untuk Chat Asisten AI.
 *
 * Privasi: yang dikirim ke AI hanya angka agregat (total per bulan dan per kategori),
 * status anggaran, dan transaksi bulan ini berupa tanggal, jenis, kategori, dan nominal.
 * Nama pengguna dan catatan (note) transaksi TIDAK pernah dikirim.
 */
class FinanceContextBuilder(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) {
    suspend fun build(): String {
        val now = Calendar.getInstance(DateUtils.localeID)
        val today = SimpleDateFormat("EEEE, d MMMM yyyy", DateUtils.localeID).format(now.time)
        val sb = StringBuilder()
        sb.appendLine("Tanggal hari ini: $today")
        sb.appendLine()

        // Ringkasan 3 bulan terakhir (bulan ini dan 2 bulan sebelumnya)
        for (offset in 0 downTo -2) {
            val cal = Calendar.getInstance(DateUtils.localeID).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, offset)
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val start = DateUtils.getStartOfMonth(year, month)
            val end = DateUtils.getEndOfMonth(year, month)

            val income = transactionRepository.getTotalByTypeAndDateRange(TransactionType.INCOME, start, end).first() ?: 0L
            val expense = transactionRepository.getTotalByTypeAndDateRange(TransactionType.EXPENSE, start, end).first() ?: 0L
            val incomeByCat = transactionRepository.getTotalsByCategoryInRange(TransactionType.INCOME, start, end).first()
            val expenseByCat = transactionRepository.getTotalsByCategoryInRange(TransactionType.EXPENSE, start, end).first()

            val label = DateUtils.formatMonthYear(year, month) + if (offset == 0) " (bulan ini)" else ""
            sb.appendLine("== $label ==")
            sb.appendLine("Pemasukan: Rp$income | Pengeluaran: Rp$expense | Saldo: Rp${income - expense}")
            if (incomeByCat.isNotEmpty()) {
                sb.appendLine("Pemasukan per kategori: " + incomeByCat.joinToString(", ") { "${it.categoryName} Rp${it.totalAmount}" })
            }
            if (expenseByCat.isNotEmpty()) {
                sb.appendLine("Pengeluaran per kategori: " + expenseByCat.joinToString(", ") { "${it.categoryName} Rp${it.totalAmount}" })
            }
            if (income == 0L && expense == 0L) sb.appendLine("(belum ada transaksi)")
            sb.appendLine()
        }

        // Anggaran bulan ini
        val budgets = budgetRepository.progressForMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH)).first()
        sb.appendLine("== Anggaran bulan ini ==")
        if (budgets.isEmpty()) {
            sb.appendLine("(pengguna belum mengatur anggaran)")
        } else {
            val daysLeft = now.getActualMaximum(Calendar.DAY_OF_MONTH) - now.get(Calendar.DAY_OF_MONTH) + 1
            sb.appendLine("Sisa hari bulan ini: $daysLeft")
            budgets.forEach {
                sb.appendLine("${it.category.name}: terpakai Rp${it.spent} dari Rp${it.limit} (${it.percent}%)")
            }
        }
        sb.appendLine()

        // Daftar transaksi bulan ini (tanpa catatan/note)
        val monthTx = transactionRepository.getByDateRange(
            DateUtils.getStartOfMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH)),
            DateUtils.getEndOfMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        ).first()
        sb.appendLine("== Transaksi bulan ini (${monthTx.size}) ==")
        val dateFormat = SimpleDateFormat("d MMM", DateUtils.localeID)
        monthTx.take(MAX_TRANSACTIONS).forEach { tx ->
            val type = if (tx.type == TransactionType.INCOME) "masuk" else "keluar"
            sb.appendLine("${dateFormat.format(Date(tx.date))} | $type | ${tx.categoryName} | Rp${tx.amount}")
        }
        if (monthTx.size > MAX_TRANSACTIONS) {
            sb.appendLine("(dan ${monthTx.size - MAX_TRANSACTIONS} transaksi lain)")
        }
        return sb.toString().trim()
    }

    private companion object {
        // Dibatasi supaya permintaan tetap kecil (batas token Groq per menit)
        const val MAX_TRANSACTIONS = 60
    }
}
