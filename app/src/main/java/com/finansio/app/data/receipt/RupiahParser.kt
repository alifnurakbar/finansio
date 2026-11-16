package com.finansio.app.data.receipt

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Membaca angka rupiah dari jawaban AI atau teks struk.
 *
 * Masalah yang diatasi: AI kadang menulis `47.000` sebagai angka JSON. Parser JSON biasa
 * membacanya sebagai 47,0 sehingga harganya menjadi Rp 47. Di sini titik yang diikuti
 * tepat tiga digit dianggap pemisah ribuan (format Indonesia), jadi `47.000` = 47000.
 */
object RupiahParser {

    // 1.250.000 atau 1.250.000,50 (format Indonesia)
    private val indonesian = Regex("""^\d{1,3}(\.\d{3})+(,\d{1,2})?$""")
    // 1,250,000 atau 1,250,000.50 (format Inggris)
    private val english = Regex("""^\d{1,3}(,\d{3})+(\.\d{1,2})?$""")
    // 27500, 27500.00, 27500,5
    private val plain = Regex("""^\d+([.,]\d{1,2})?$""")

    /** Mengubah teks seperti "Rp 27.500", "27500", "-5.000" atau "(5.000)" menjadi rupiah utuh. Null jika tidak terbaca. */
    fun parse(raw: String?): Long? {
        if (raw == null) return null
        var s = raw.trim()
            .replace(Regex("(?i)rp\\.?|idr"), "")
            .replace(" ", "")
            .replace(" ", "")
        if (s.isEmpty()) return null

        var negative = false
        if (s.startsWith("(") && s.endsWith(")")) {
            negative = true
            s = s.substring(1, s.length - 1)
        }
        if (s.startsWith("-")) {
            negative = true
            s = s.substring(1)
        } else if (s.endsWith("-")) { // beberapa struk mencetak diskon sebagai "5.000-"
            negative = true
            s = s.dropLast(1)
        }
        if (s.isEmpty()) return null

        val normalized = when {
            indonesian.matches(s) -> s.replace(".", "").replace(",", ".")
            english.matches(s) -> s.replace(",", "")
            plain.matches(s) -> s.replace(",", ".")
            else -> return null
        }
        val value = BigDecimal(normalized).setScale(0, RoundingMode.HALF_UP).toLong()
        return if (negative) -value else value
    }

    /**
     * Membaca angka dari elemen JSON. Untuk angka JSON, Gson menyimpan teks aslinya,
     * sehingga `47.000` masih bisa dibedakan dari `47`.
     */
    fun parse(element: JsonElement?): Long? {
        if (element == null || element.isJsonNull || !element.isJsonPrimitive) return null
        val primitive: JsonPrimitive = element.asJsonPrimitive
        return when {
            primitive.isNumber || primitive.isString -> parse(primitive.asString)
            else -> null
        }
    }
}
