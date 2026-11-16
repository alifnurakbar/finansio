package com.finansio.app.data.scanner

import kotlin.math.abs
import kotlin.math.roundToLong

/** Item struk seperti yang dibaca AI, sebelum diperiksa. Semua angka boleh kosong. */
data class RawReceiptItem(
    val name: String,
    val quantity: Double?,
    val unitPrice: Long?,
    val lineTotal: Long?,
    val suggestedCategory: String? = null
)

/** Isi struk seperti yang dibaca AI, sebelum diperiksa. */
data class RawReceipt(
    val merchantName: String?,
    val date: String?,
    val items: List<RawReceiptItem>,
    val subtotal: Long? = null,
    val discount: Long? = null,
    val tax: Long? = null,
    val serviceCharge: Long? = null,
    val total: Long? = null
)

/** Item struk yang sudah diperiksa: nominal yang disimpan = [unitPrice] × [quantity]. */
data class ReceiptLine(
    val name: String,
    val unitPrice: Long,
    val quantity: Int,
    val suggestedCategory: String? = null
) {
    val amount: Long get() = unitPrice * quantity
}

/** Struk yang sudah diperiksa. [printedTotal] = TOTAL yang tercetak, null bila tidak terbaca. */
data class ReconciledReceipt(
    val merchantName: String?,
    val date: String?,
    val lines: List<ReceiptLine>,
    val printedTotal: Long?
) {
    val linesTotal: Long get() = lines.sumOf { it.amount }
    val matchesPrintedTotal: Boolean get() = printedTotal == null || printedTotal == linesTotal
}

/**
 * Memeriksa dan merapikan hasil baca AI supaya angka yang disimpan sama dengan struk:
 * 1. Membuang baris ringkasan (TOTAL, BAYAR, KEMBALI, PPN, ...) yang ikut terbaca sebagai item.
 * 2. Mengutamakan subtotal baris yang tercetak, dan memperbaiki harga satuan/subtotal yang tertukar.
 * 3. Menyebar pajak, service charge, dan diskon struk ke item secara proporsional,
 *    hanya bila hasilnya memang membuat jumlah item sama dengan TOTAL struk.
 */
object ReceiptReconciler {

    private val NON_ITEM = Regex(
        "^\\s*(sub\\s*-?total|grand\\s*total|total|jumlah|bayar|tunai|cash|kembali(an)?|change|" +
            "ppn|pajak|tax|dpp|service(\\s*charge)?|layanan|diskon|discount|potongan|" +
            "debit|kredit|credit|qris|pembulatan|rounding)(\\b|\\s|:|$)",
        RegexOption.IGNORE_CASE
    )

    private data class Working(val name: String, val quantity: Double, var lineTotal: Long, val category: String?)

    fun reconcile(raw: RawReceipt): ReconciledReceipt {
        val working = raw.items
            .filter { it.name.isNotBlank() && !NON_ITEM.containsMatchIn(it.name) }
            .map { normalize(it) }

        val itemsSum = working.sumOf { it.lineTotal }
        val adjustment = (raw.tax ?: 0) + (raw.serviceCharge ?: 0) - abs(raw.discount ?: 0)
        val total = raw.total?.takeIf { it > 0 }

        // Pajak/diskon hanya disebar bila memang menjelaskan selisihnya. Banyak struk ritel
        // mencetak PPN yang sudah termasuk dalam harga, dan itu tidak boleh ditambahkan lagi.
        val shouldAdjust = adjustment != 0L && itemsSum > 0 &&
            (total == null || (itemsSum != total && itemsSum + adjustment == total))
        if (shouldAdjust) distribute(working, adjustment)

        return ReconciledReceipt(
            merchantName = raw.merchantName?.takeIf { it.isNotBlank() },
            date = raw.date?.takeIf { it.isNotBlank() },
            lines = working.map { toLine(it) },
            printedTotal = total
        )
    }

    private fun normalize(item: RawReceiptItem): Working {
        val qty = item.quantity?.takeIf { it > 0 } ?: 1.0
        var unit = item.unitPrice?.takeIf { it > 0 }
        var line = item.lineTotal?.takeIf { it > 0 }

        val wholeQty = qty == Math.floor(qty)
        if (unit != null && line != null && wholeQty) {
            val q = qty.toLong()
            // AI menukar kolom: harga satuan diisi subtotal dan sebaliknya.
            if (unit * q != line && line * q == unit) {
                val swap = unit; unit = line; line = swap
            }
        }
        // Subtotal baris yang tercetak lebih bisa dipercaya daripada hasil perkalian.
        val lineTotal = line ?: unit?.let { (it * qty).roundToLong() } ?: 0L
        return Working(item.name.trim(), qty, lineTotal, item.suggestedCategory)
    }

    /** Membagi [adjustment] ke item sebanding subtotalnya (metode sisa terbesar, tanpa pembulatan hilang). */
    private fun distribute(items: List<Working>, adjustment: Long) {
        val base = items.sumOf { it.lineTotal }
        if (base <= 0) return
        val sign = if (adjustment < 0) -1L else 1L
        val amount = abs(adjustment)
        val shares = items.map { amount * it.lineTotal / base }
        var remainder = amount - shares.sum()
        val order = items.indices.sortedByDescending { (amount * items[it].lineTotal) % base }
        val finalShares = shares.toMutableList()
        for (i in order) {
            if (remainder <= 0) break
            finalShares[i] += 1; remainder -= 1
        }
        items.forEachIndexed { i, item ->
            item.lineTotal = (item.lineTotal + sign * finalShares[i]).coerceAtLeast(0)
        }
    }

    private fun toLine(w: Working): ReceiptLine {
        val wholeQty = w.quantity == Math.floor(w.quantity) && w.quantity >= 1
        val q = w.quantity.toInt()
        return if (wholeQty && w.lineTotal % q == 0L) {
            ReceiptLine(w.name, w.lineTotal / q, q, w.category)
        } else {
            // Barang timbang (1,5 KG) atau subtotal yang tidak habis dibagi: simpan sebagai 1 baris
            // dengan nominal persis, jumlahnya ditulis di nama supaya tidak hilang.
            val qtyLabel = if (wholeQty) q.toString() else w.quantity.toString().replace('.', ',')
            ReceiptLine("${w.name} (${qtyLabel}x)", w.lineTotal, 1, w.category)
        }
    }
}
