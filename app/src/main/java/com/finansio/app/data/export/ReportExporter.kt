package com.finansio.app.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.data.repository.BudgetRepository
import com.finansio.app.data.repository.TransactionRepository
import com.finansio.app.util.CurrencyFormatter
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

enum class ExportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv")
}

/** Membuat laporan bulanan (PDF atau CSV) lalu menyiapkan Intent "Bagikan" Android. */
class ReportExporter(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) {
    private data class ReportData(
        val monthLabel: String,
        val transactions: List<TransactionEntity>,
        val income: Long,
        val expense: Long,
        val expenseByCategory: List<Pair<String, Long>>,
        val budgets: List<com.finansio.app.data.repository.BudgetProgress>
    )

    suspend fun createShareIntent(year: Int, month: Int, format: ExportFormat, userName: String): Intent =
        withContext(Dispatchers.IO) {
            val data = loadData(year, month)
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // hapus laporan lama
            val fileName = "Laporan-Finansio-%04d-%02d.%s".format(year, month + 1, format.extension)
            val file = File(dir, fileName)
            when (format) {
                ExportFormat.CSV -> writeCsv(file, data)
                ExportFormat.PDF -> writePdf(file, data, userName)
            }
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan ${data.monthLabel}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(send, "Bagikan laporan").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private suspend fun loadData(year: Int, month: Int): ReportData {
        val start = DateUtils.getStartOfMonth(year, month)
        val end = DateUtils.getEndOfMonth(year, month)
        val transactions = transactionRepository.getByDateRange(start, end).first().sortedBy { it.date }
        return ReportData(
            monthLabel = DateUtils.formatMonthYear(year, month),
            transactions = transactions,
            income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            expenseByCategory = transactions.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryName }
                .map { (name, list) -> name to list.sumOf { it.amount } }
                .sortedByDescending { it.second },
            budgets = budgetRepository.progressForMonth(year, month).first()
        )
    }

    // ---------------- CSV ----------------

    private fun writeCsv(file: File, data: ReportData) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", DateUtils.localeID)
        fun esc(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
        file.bufferedWriter(Charsets.UTF_8).use { out ->
            out.write("﻿") // BOM supaya Excel membaca huruf dengan benar
            out.write("Tanggal,Jenis,Kategori,Nominal,Catatan\n")
            data.transactions.forEach { tx ->
                val type = if (tx.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
                out.write(
                    listOf(
                        dateFormat.format(Date(tx.date)), type, esc(tx.categoryName),
                        tx.amount.toString(), esc(tx.note ?: "")
                    ).joinToString(",") + "\n"
                )
            }
        }
    }

    // ---------------- PDF ----------------

    private fun writePdf(file: File, data: ReportData, userName: String) {
        val pdf = PdfDocument()
        val writer = PdfWriter(pdf)
        val c = writer

        // Judul
        c.text("Laporan Keuangan", 22f, bold = true)
        c.text(data.monthLabel, 14f, color = GRAY)
        val generated = SimpleDateFormat("d MMMM yyyy, HH:mm", DateUtils.localeID).format(Date())
        c.text(
            (if (userName.isNotBlank()) "Milik: $userName  •  " else "") + "Dibuat: $generated",
            9f, color = GRAY
        )
        c.space(16f)

        // Ringkasan
        c.summaryBoxes(
            listOf(
                Triple("Pemasukan", CurrencyFormatter.formatRupiah(data.income), GREEN),
                Triple("Pengeluaran", CurrencyFormatter.formatRupiah(data.expense), RED),
                Triple("Saldo", CurrencyFormatter.formatRupiah(data.income - data.expense), DARK)
            )
        )
        c.space(20f)

        // Pengeluaran per kategori
        c.text("Pengeluaran per Kategori", 13f, bold = true)
        c.space(6f)
        if (data.expenseByCategory.isEmpty()) {
            c.text("Belum ada pengeluaran.", 10f, color = GRAY)
        } else {
            data.expenseByCategory.forEach { (name, total) ->
                val percent = if (data.expense > 0) total * 100f / data.expense else 0f
                c.barRow(name, CurrencyFormatter.formatRupiah(total), "%.0f%%".format(percent), percent / 100f)
            }
        }
        c.space(16f)

        // Anggaran
        if (data.budgets.isNotEmpty()) {
            c.text("Anggaran", 13f, bold = true)
            c.space(6f)
            data.budgets.forEach { b ->
                val color = when {
                    b.percent >= 100 -> RED
                    b.percent >= 80 -> AMBER
                    else -> GREEN
                }
                c.barRow(
                    b.category.name,
                    "${CurrencyFormatter.formatRupiah(b.spent)} / ${CurrencyFormatter.formatRupiah(b.limit)}",
                    "${b.percent}%",
                    (b.percent / 100f).coerceAtMost(1f),
                    color
                )
            }
            c.space(16f)
        }

        // Daftar transaksi
        c.text("Daftar Transaksi (${data.transactions.size})", 13f, bold = true)
        c.space(6f)
        if (data.transactions.isEmpty()) {
            c.text("Belum ada transaksi di bulan ini.", 10f, color = GRAY)
        } else {
            val dateFormat = SimpleDateFormat("dd MMM", DateUtils.localeID)
            c.tableHeader()
            data.transactions.forEach { tx ->
                c.tableRow(
                    date = dateFormat.format(Date(tx.date)),
                    category = tx.categoryName,
                    note = tx.note ?: "-",
                    amount = (if (tx.type == TransactionType.INCOME) "+" else "-") +
                        CurrencyFormatter.formatRupiah(tx.amount),
                    isIncome = tx.type == TransactionType.INCOME
                )
            }
        }

        writer.finish()
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
    }

    /** Penulis PDF sederhana dengan pindah halaman otomatis. Ukuran A4 (595 x 842 point). */
    private class PdfWriter(private val pdf: PdfDocument) {
        private val pageWidth = 595
        private val pageHeight = 842
        private val margin = 40f
        private val contentWidth = pageWidth - 2 * margin
        private var pageNumber = 0
        private lateinit var page: PdfDocument.Page
        private var y = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        init { newPage() }

        private val canvas get() = page.canvas

        /** Kembalikan Paint ke keadaan awal. Perataan teks diatur eksplisit karena
         *  Paint.reset() tidak selalu mengembalikannya ke kiri di semua HP. */
        private fun resetPaint() {
            paint.reset()
            paint.isAntiAlias = true
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.DEFAULT
        }

        private fun newPage() {
            if (pageNumber > 0) finishPage()
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            y = margin
        }

        private fun finishPage() {
            resetPaint()
            paint.textSize = 8f; paint.color = GRAY
            canvas.drawText("Finansio  •  Halaman $pageNumber", margin, pageHeight - 20f, paint)
            pdf.finishPage(page)
        }

        fun finish() = finishPage()

        private fun ensure(height: Float) {
            if (y + height > pageHeight - 50f) newPage()
        }

        fun space(h: Float) { y += h }

        fun text(value: String, size: Float, bold: Boolean = false, color: Int = DARK) {
            ensure(size + 6f)
            resetPaint()
            paint.textSize = size
            paint.color = color
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            y += size
            canvas.drawText(value, margin, y, paint)
            y += size * 0.45f
        }

        fun summaryBoxes(items: List<Triple<String, String, Int>>) {
            val gap = 10f
            val w = (contentWidth - gap * (items.size - 1)) / items.size
            val h = 56f
            ensure(h)
            items.forEachIndexed { i, (label, value, color) ->
                val left = margin + i * (w + gap)
                resetPaint()
                paint.color = LIGHT_BG
                canvas.drawRoundRect(RectF(left, y, left + w, y + h), 10f, 10f, paint)
                paint.textSize = 9f; paint.color = GRAY
                canvas.drawText(label, left + 12f, y + 20f, paint)
                paint.textSize = 13f; paint.color = color; paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(value, left + 12f, y + 40f, paint)
            }
            y += h
        }

        fun barRow(label: String, value: String, percent: String, fraction: Float, color: Int = RED) {
            val h = 30f
            ensure(h)
            resetPaint()
            paint.textSize = 10f; paint.color = DARK
            canvas.drawText(label, margin, y + 11f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("$value  ($percent)", margin + contentWidth, y + 11f, paint)
            paint.textAlign = Paint.Align.LEFT
            val barTop = y + 16f
            paint.color = LIGHT_BG
            canvas.drawRoundRect(RectF(margin, barTop, margin + contentWidth, barTop + 7f), 4f, 4f, paint)
            paint.color = color
            canvas.drawRoundRect(
                RectF(margin, barTop, margin + contentWidth * fraction.coerceIn(0f, 1f), barTop + 7f), 4f, 4f, paint
            )
            y += h
        }

        // Kolom tabel: Tanggal | Kategori | Catatan | Nominal
        private val colDate = margin
        private val colCategory = margin + 55f
        private val colNote = margin + 160f
        private val colAmountRight = margin + contentWidth

        fun tableHeader() {
            ensure(22f)
            resetPaint()
            paint.color = LIGHT_BG
            canvas.drawRect(margin, y, margin + contentWidth, y + 20f, paint)
            paint.textSize = 9f; paint.color = GRAY; paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Tanggal", colDate + 4f, y + 14f, paint)
            canvas.drawText("Kategori", colCategory, y + 14f, paint)
            canvas.drawText("Catatan", colNote, y + 14f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Nominal", colAmountRight - 4f, y + 14f, paint)
            y += 22f
        }

        fun tableRow(date: String, category: String, note: String, amount: String, isIncome: Boolean) {
            if (y + 18f > pageHeight - 50f) { newPage(); tableHeader() }
            resetPaint()
            paint.textSize = 9f; paint.color = DARK
            canvas.drawText(date, colDate + 4f, y + 12f, paint)
            canvas.drawText(ellipsize(category, colNote - colCategory - 8f), colCategory, y + 12f, paint)
            canvas.drawText(ellipsize(note, colAmountRight - 100f - colNote), colNote, y + 12f, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = if (isIncome) GREEN else RED
            canvas.drawText(amount, colAmountRight - 4f, y + 12f, paint)
            paint.textAlign = Paint.Align.LEFT
            paint.color = LINE
            canvas.drawLine(margin, y + 18f, margin + contentWidth, y + 18f, paint)
            y += 18f
        }

        private fun ellipsize(text: String, maxWidth: Float): String {
            if (paint.measureText(text) <= maxWidth) return text
            var end = text.length
            while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
            return text.substring(0, end) + "…"
        }
    }

    private companion object {
        val DARK = Color.rgb(30, 30, 34)
        val GRAY = Color.rgb(120, 121, 128)
        val GREEN = Color.rgb(46, 125, 50)
        val RED = Color.rgb(211, 72, 54)
        val AMBER = Color.rgb(230, 160, 0)
        val LIGHT_BG = Color.rgb(242, 242, 245)
        val LINE = Color.rgb(228, 228, 232)
    }
}
