package com.finansio.app.data.receipt

/** Satu item struk yang angkanya sudah diperiksa: [unitPrice] × [quantity] selalu sama dengan [lineTotal]. */
data class CheckedItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Long,
    val lineTotal: Long,
    val suggestedCategory: String?,
    /** True bila AI tidak menemukan harga item ini. Pengguna harus mengisinya sendiri. */
    val priceMissing: Boolean = false
)

/** Hasil pemeriksaan struk: item yang sudah dibetulkan, TOTAL tercetak, dan penyesuaian yang disebar. */
data class CheckedReceipt(
    val merchantName: String?,
    val date: String?,
    val items: List<CheckedItem>,
    /** Angka TOTAL yang tercetak di struk (yang harus dibayar). Null bila tidak terbaca. */
    val receiptTotal: Long?,
    /** Penyesuaian (pajak, service, diskon, pembulatan) yang sudah disebar ke item. Kosong bila tidak ada. */
    val spreadAdjustments: List<Adjustment>
)

data class Adjustment(val label: String, val amount: Long)

/** Perbandingan jumlah item dengan TOTAL struk. */
sealed class TotalCheck {
    /** TOTAL struk tidak terbaca, jadi tidak bisa dibandingkan. */
    object Unknown : TotalCheck()
    object Match : TotalCheck()
    /** [difference] = jumlah item − TOTAL struk. Positif berarti item kelebihan. */
    data class Mismatch(val difference: Long) : TotalCheck()
}

/**
 * Memeriksa ulang jawaban AI sebelum ditampilkan.
 *
 * Yang dilakukan:
 * 1. Baris ringkasan (TOTAL, BAYAR, KEMBALI, PPN, ...) yang terlanjur dianggap item dibuang.
 *    Bila baris itu pajak/service/diskon/pembulatan dan field ringkasannya kosong, angkanya dipindah ke sana.
 * 2. Harga satuan, jumlah, dan subtotal tiap item dibuat konsisten (lihat [fixLine]).
 * 3. Pajak, service, diskon, dan pembulatan disebar ke item HANYA bila hasilnya tepat sama dengan TOTAL.
 *    Jadi PPN yang sudah termasuk harga tidak terhitung dua kali.
 *
 * Angka tidak pernah diakali supaya cocok. Bila tetap tidak cocok, selisihnya ditunjukkan ke pengguna.
 */
object ReceiptChecker {

    private val paymentWords = Regex(
        """^(SUB\s*-?\s*TOTAL|GRAND\s*TOTAL|TOTAL|JUMLAH|BAYAR|PEMBAYARAN|TUNAI|CASH|KEMBALI|KEMBALIAN|CHANGE|DEBIT|KREDIT|CREDIT|QRIS|OVO|GOPAY|DANA|SHOPEEPAY|EDC|DPP)\b"""
    )
    private val taxWords = Regex("""^(PPN|PAJAK|TAX|PB\s*1|VAT)\b""")
    private val serviceWords = Regex("""^(SERVICE|SERVIS|SVC|BIAYA\s+LAYANAN)\b""")
    private val discountWords = Regex("""^(DISKON|DISC|DISCOUNT|POTONGAN|PROMO|HEMAT|VOUCHER)\b""")
    private val roundingWords = Regex("""^(PEMBULATAN|ROUNDING|ROUND)\b""")

    fun check(raw: RawReceipt): CheckedReceipt {
        var tax = raw.tax
        var service = raw.serviceCharge
        var discount = raw.discount?.let { kotlin.math.abs(it) }
        var rounding = raw.rounding

        val goods = mutableListOf<RawReceiptItem>()
        for (item in mergeQuantityLines(raw.items)) {
            val key = item.name.uppercase().trimStart { !it.isLetterOrDigit() }
            val amount = item.lineTotal ?: item.unitPrice
            when {
                paymentWords.containsMatchIn(key) -> Unit
                taxWords.containsMatchIn(key) -> if (tax == null) tax = amount
                serviceWords.containsMatchIn(key) -> if (service == null) service = amount
                discountWords.containsMatchIn(key) -> if (amount != null) discount = (discount ?: 0) + kotlin.math.abs(amount)
                roundingWords.containsMatchIn(key) -> if (rounding == null) rounding = amount
                // Baris bernilai negatif tanpa kata kunci (misalnya potongan per item) dihitung sebagai diskon.
                amount != null && amount < 0 -> discount = (discount ?: 0) + -amount
                else -> goods.add(item)
            }
        }

        var items = goods.map { fixLine(it) }

        val adjustments = listOfNotNull(
            tax?.takeIf { it != 0L }?.let { Adjustment("Pajak", it) },
            service?.takeIf { it != 0L }?.let { Adjustment("Service charge", it) },
            discount?.takeIf { it != 0L }?.let { Adjustment("Diskon", -it) },
            rounding?.takeIf { it != 0L }?.let { Adjustment("Pembulatan", it) }
        )

        var spread = emptyList<Adjustment>()
        val total = raw.total
        if (total != null && items.isNotEmpty()) {
            val itemsSum = items.sumOf { it.lineTotal }
            if (itemsSum != total) {
                val chosen = subsetsLargestFirst(adjustments).firstOrNull { subset ->
                    itemsSum + subset.sumOf { it.amount } == total
                }
                if (chosen != null) {
                    items = distribute(items, chosen.sumOf { it.amount })
                    spread = chosen
                }
            }
        }

        return CheckedReceipt(
            merchantName = raw.merchantName,
            date = raw.date,
            items = items,
            receiptTotal = total,
            spreadAdjustments = spread
        )
    }

    /**
     * Baris yang isinya hanya jumlah dan satuan, misalnya "2 PCSX", "1 RTGx", "PCS X", "RTG".
     * Di struk dua baris, baris ini berisi harga untuk nama barang di atasnya.
     */
    private val quantityLine = Regex(
        """^\d*\s*(PCS|PC|RTG|RTGS|BKS|BH|BUAH|BTL|BOTOL|KG|GR|LSN|DUS|BOX|PAK|PACK|PCK|SCT|SACHET|KTK|CUP|BJ|BIJI|LBR|ROLL|RL|UNIT|EA|ML|LTR|BAL|IKT|RIM|PSG)?\s*[X×@*]?\s*\d*$""",
        RegexOption.IGNORE_CASE
    )

    internal fun isQuantityLine(name: String): Boolean = name.isNotBlank() && quantityLine.matches(name.trim())

    /**
     * Struk dua baris (nama di atas, "2 PCS X 27.500 55.000" di bawah) kadang dibaca AI sebagai dua item:
     * nama tanpa harga, lalu item bernama "PCSX" yang berisi harganya. Gabungkan keduanya menjadi satu item
     * dengan nama barang yang benar. Hanya dilakukan bila item di atasnya memang belum punya harga.
     */
    internal fun mergeQuantityLines(items: List<RawReceiptItem>): List<RawReceiptItem> {
        val merged = mutableListOf<RawReceiptItem>()
        for (item in items) {
            val previous = merged.lastOrNull()
            val previousHasNoPrice = previous != null && previous.unitPrice == null && previous.lineTotal == null &&
                !isQuantityLine(previous.name)
            if (isQuantityLine(item.name) && previousHasNoPrice) {
                merged[merged.lastIndex] = item.copy(
                    name = previous!!.name,
                    suggestedCategory = previous.suggestedCategory ?: item.suggestedCategory
                )
            } else {
                merged.add(item)
            }
        }
        return merged
    }

    /** Bandingkan jumlah item dengan TOTAL struk. Dipanggil ulang setiap kali pengguna mengubah item. */
    fun compare(itemsTotal: Long, receiptTotal: Long?): TotalCheck = when {
        receiptTotal == null -> TotalCheck.Unknown
        itemsTotal == receiptTotal -> TotalCheck.Match
        else -> TotalCheck.Mismatch(itemsTotal - receiptTotal)
    }

    /**
     * Membuat harga satuan (u), jumlah (q), dan subtotal baris (L) saling konsisten.
     * Subtotal baris dipercaya lebih dulu, karena angka itulah yang dijumlahkan menjadi TOTAL struk.
     */
    internal fun fixLine(item: RawReceiptItem): CheckedItem {
        var q = item.quantity ?: 1
        var u = item.unitPrice?.takeIf { it > 0 }
        var l = item.lineTotal?.takeIf { it > 0 }

        if (u == null && l == null) {
            return CheckedItem(item.name, q, 0, 0, item.suggestedCategory, priceMissing = true)
        }
        if (u != null && l != null && u * q != l) {
            when {
                // Tertukar: AI menaruh subtotal di harga satuan dan harga satuan di subtotal.
                q > 1 && u == l * q -> { val t = u; u = l; l = t }
                // Subtotal ditulis sebagai harga satuan (penyebab angka dobel): subtotal sudah benar.
                q > 1 && u == l -> u = null
                // Jumlah salah baca, misalnya 2 × 13.750 = 27.500 tetapi quantity ditulis 1.
                l > u && l % u == 0L -> q = (l / u).toInt()
                // Selain itu percayai subtotal baris.
                else -> u = null
            }
        }
        if (l == null) l = u!! * q
        if (u == null) {
            if (l % q == 0L) u = l / q
            else { u = l; q = 1 } // harga satuan tidak bulat: simpan sebagai 1 baris agar total tetap tepat
        }
        return CheckedItem(item.name, q, u, l, item.suggestedCategory)
    }

    /** Semua kombinasi penyesuaian, yang paling lengkap dicoba lebih dulu. */
    private fun subsetsLargestFirst(all: List<Adjustment>): List<List<Adjustment>> {
        val n = all.size
        return (1 until (1 shl n))
            .map { mask -> all.filterIndexed { i, _ -> mask and (1 shl i) != 0 } }
            .sortedByDescending { it.size }
    }

    /**
     * Menyebar [amount] ke item sebanding dengan subtotalnya. Sisa pembagian diberikan ke item
     * dengan pecahan terbesar, sehingga jumlah akhirnya tepat.
     */
    internal fun distribute(items: List<CheckedItem>, amount: Long): List<CheckedItem> {
        val base = items.sumOf { it.lineTotal }
        if (amount == 0L || base <= 0L) return items

        val shares = items.map { it.lineTotal * amount / base } // dibulatkan ke arah nol
        var remainder = amount - shares.sum()
        val step = if (remainder > 0) 1L else -1L
        val order = items.indices.filter { items[it].lineTotal > 0 }.sortedByDescending { i ->
            kotlin.math.abs(items[i].lineTotal * amount % base)
        }
        val finalShares = shares.toMutableList()
        var k = 0
        while (remainder != 0L && order.isNotEmpty()) {
            finalShares[order[k % order.size]] += step
            remainder -= step
            k++
        }

        return items.mapIndexed { i, item ->
            if (item.priceMissing) return@mapIndexed item
            val newTotal = item.lineTotal + finalShares[i]
            if (newTotal % item.quantity == 0L) {
                item.copy(unitPrice = newTotal / item.quantity, lineTotal = newTotal)
            } else {
                item.copy(quantity = 1, unitPrice = newTotal, lineTotal = newTotal)
            }
        }
    }
}
