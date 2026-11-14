package com.finansio.app.util

import java.text.NumberFormat
import java.util.Locale

/** Format angka menjadi Rupiah, misalnya 15000 menjadi "Rp 15.000". */
object CurrencyFormatter {
    fun formatRupiah(amount: Long): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(amount).replace("Rp", "Rp ")
    }
}
