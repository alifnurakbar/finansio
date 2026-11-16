package com.finansio.app.data.receipt

import kotlin.math.abs
import kotlin.math.max

/** Satu potongan teks hasil OCR beserta kotak posisinya di gambar (piksel). */
data class OcrFragment(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerY: Float get() = (top + bottom) / 2f
    val height: Int get() = bottom - top
}

/**
 * Menyusun ulang teks OCR per baris berdasarkan posisinya.
 *
 * ML Kit mengelompokkan teks per kolom, sehingga semua nama barang muncul lebih dulu dan
 * semua harga baru muncul di akhir (setelah TOTAL dan BAYAR). Fungsi ini mengumpulkan
 * potongan yang sejajar secara vertikal ke baris yang sama, lalu mengurutkannya dari kiri
 * ke kanan, sehingga nama barang dan harganya kembali berada di satu baris.
 */
object OcrRowBuilder {

    /** Potongan masuk ke baris yang sama bila jarak tengahnya maksimal sekian kali tinggi teks. */
    private const val SAME_ROW_TOLERANCE = 0.5f

    fun buildRows(fragments: List<OcrFragment>): String {
        val usable = fragments.filter { it.text.isNotBlank() && it.height > 0 }
        if (usable.isEmpty()) return ""

        val rows = mutableListOf<MutableList<OcrFragment>>()
        for (fragment in usable.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            if (row != null && belongsTo(fragment, row)) row.add(fragment)
            else rows.add(mutableListOf(fragment))
        }

        return rows.joinToString("\n") { row ->
            row.sortedBy { it.left }.joinToString("  ") { it.text.trim() }
        }
    }

    private fun belongsTo(fragment: OcrFragment, row: List<OcrFragment>): Boolean {
        val rowCenter = row.map { it.centerY }.average().toFloat()
        val rowHeight = row.map { it.height }.average().toFloat()
        val tolerance = max(rowHeight, fragment.height.toFloat()) * SAME_ROW_TOLERANCE
        return abs(fragment.centerY - rowCenter) <= tolerance
    }
}
