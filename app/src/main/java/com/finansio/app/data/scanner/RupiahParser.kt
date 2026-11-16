package com.finansio.app.data.scanner

import kotlin.math.roundToLong

/**
 * Mengubah angka nominal seperti yang tercetak di struk Indonesia menjadi rupiah utuh.
 *
 * Contoh: "27.500" -> 27500, "Rp 1.250.000" -> 1250000, "27,500.00" -> 27500,
 * "27.500,00" -> 27500, "55.000,-" -> 55000, "-33.800" -> -33800.
 *
 * Di struk Indonesia titik hampir selalu pemisah ribuan. Pemisah hanya dianggap desimal
 * bila diikuti tepat 1–2 digit (misal ",00").
 */
object RupiahParser {

    fun parse(raw: String?): Long? {
        if (raw == null) return null
        val text = raw.trim()
        if (text.isEmpty()) return null

        val negative = text.startsWith("-") || text.startsWith("(") ||
            text.replace(" ", "").startsWith("Rp-", ignoreCase = true)

        // OCR kadang membaca angka 0 sebagai huruf O di tengah angka (misal "27.5O0").
        val fixedOcr = text.replace(Regex("(?<=[\\d.,])[Oo](?=[\\d.,]|$)"), "0")
        val cleaned = fixedOcr.filter { it.isDigit() || it == '.' || it == ',' }
        if (cleaned.none { it.isDigit() }) return null

        val lastDot = cleaned.lastIndexOf('.')
        val lastComma = cleaned.lastIndexOf(',')
        val sepIndex = maxOf(lastDot, lastComma)

        var integerPart = cleaned
        var fraction = ""
        if (sepIndex >= 0) {
            val tail = cleaned.substring(sepIndex + 1)
            val sepChar = cleaned[sepIndex]
            val usesBoth = lastDot >= 0 && lastComma >= 0
            val sepCount = cleaned.count { it == sepChar }
            val isDecimal = tail.length in 1..2 && tail.all { it.isDigit() } && (usesBoth || sepCount == 1)
            if (isDecimal) {
                integerPart = cleaned.substring(0, sepIndex)
                fraction = tail
            }
        }

        val digits = integerPart.filter { it.isDigit() }
        var value = if (digits.isEmpty()) 0L else digits.toLongOrNull() ?: return null
        if (fraction.isNotEmpty() && fraction.padEnd(2, '0').take(2).toInt() >= 50) value += 1
        return if (negative) -value else value
    }

    /**
     * Nominal yang dikirim AI sebagai angka JSON. Angka pecahan kecil seperti 27.5 hampir pasti
     * hasil salah baca "27.500" (titik ribuan dianggap desimal), jadi dikalikan 1000.
     */
    fun fromJsonNumber(value: Double): Long {
        val isFractional = value != Math.floor(value)
        return if (isFractional && kotlin.math.abs(value) < 1000) (value * 1000).roundToLong()
        else value.roundToLong()
    }

    /** Jumlah barang, misal "2 PCS" -> 2.0, "1,5 KG" -> 1.5. */
    fun parseQuantity(raw: String?): Double? {
        if (raw == null) return null
        val match = Regex("\\d+(?:[.,]\\d+)?").find(raw) ?: return null
        return match.value.replace(',', '.').toDoubleOrNull()
    }
}
