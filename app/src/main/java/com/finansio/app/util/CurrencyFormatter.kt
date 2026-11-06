package com.finansio.app.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun formatRupiah(amount: Long): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(amount).replace("Rp", "Rp ")
    }
}
