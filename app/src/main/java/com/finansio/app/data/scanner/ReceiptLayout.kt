package com.finansio.app.data.scanner

import kotlin.math.abs

/** Satu baris teks hasil OCR beserta posisinya di gambar (piksel). */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerY: Double get() = (top + bottom) / 2.0
    val height: Int get() = (bottom - top).coerceAtLeast(1)
}

/**
 * Menyusun ulang hasil OCR menjadi baris-baris seperti yang terlihat di struk.
 *
 * ML Kit mengelompokkan teks per *blok*, sehingga kolom nama barang dan kolom harga
 * terpisah jauh (semua harga muncul setelah kata TOTAL). Akibatnya AI tidak tahu harga
 * mana milik barang mana. Di sini setiap potongan teks yang sejajar secara vertikal
 * digabung lagi menjadi satu baris, diurutkan dari kiri ke kanan.
 */
object ReceiptLayout {

    fun toText(lines: List<OcrLine>): String {
        val rows = mutableListOf<MutableList<OcrLine>>()
        lines.filter { it.text.isNotBlank() }
            .sortedBy { it.centerY }
            .forEach { line ->
                val row = rows.lastOrNull()
                if (row != null && belongsToRow(row, line)) row.add(line) else rows.add(mutableListOf(line))
            }
        return rows.joinToString("\n") { row ->
            row.sortedBy { it.left }.joinToString("   ") { it.text.trim() }
        }
    }

    private fun belongsToRow(row: List<OcrLine>, line: OcrLine): Boolean {
        // Potongan yang bertumpuk secara horizontal pasti berada di baris yang berbeda.
        if (row.any { it.left < line.right && line.left < it.right }) return false
        val rowCenter = row.map { it.centerY }.average()
        val rowHeight = row.map { it.height }.sorted()[row.size / 2]
        val tolerance = minOf(rowHeight, line.height) * 0.5
        return abs(line.centerY - rowCenter) <= tolerance
    }
}
