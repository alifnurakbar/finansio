package com.finansio.app.data.scanner

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

/**
 * Membaca jawaban JSON AI menjadi [RawReceipt] dengan toleran.
 *
 * Sebelumnya jawaban dibaca langsung ke field `Long`, sehingga harga yang dikirim sebagai teks
 * ("27.500") atau `null` berubah diam-diam menjadi 0 atau membuat parsing gagal.
 * Di sini setiap angka boleh berupa angka maupun teks, dan dibaca dengan [RupiahParser].
 */
object ReceiptJsonReader {

    fun read(content: String): RawReceipt {
        val json = JsonParser.parseString(extractJsonObject(content)).asJsonObject
        val items = json.arrayOrEmpty("items").mapNotNull { element ->
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val name = obj.string("name") ?: return@mapNotNull null
            RawReceiptItem(
                name = name,
                quantity = obj.quantity("quantity"),
                unitPrice = obj.amount("unit_price") ?: obj.amount("price"),
                lineTotal = obj.amount("line_total"),
                suggestedCategory = obj.string("suggested_category")
            )
        }
        return RawReceipt(
            merchantName = json.string("merchant_name"),
            date = json.string("date"),
            items = items,
            subtotal = json.amount("subtotal"),
            discount = json.amount("discount"),
            tax = json.amount("tax"),
            serviceCharge = json.amount("service_charge"),
            total = json.amount("total") ?: json.amount("total_amount")
        )
    }

    /** Mengambil objek JSON pertama, membuang blok <think>…</think> atau ```json bila ada. */
    fun extractJsonObject(content: String): String {
        val withoutThinking = content.replace(Regex("(?s)<think>.*?</think>"), "")
        val start = withoutThinking.indexOf('{')
        val end = withoutThinking.lastIndexOf('}')
        if (start < 0 || end <= start) throw JsonSyntaxException("Jawaban AI tidak berisi JSON")
        return withoutThinking.substring(start, end + 1)
    }

    private fun JsonObject.value(key: String): JsonElement? = get(key)?.takeIf { !it.isJsonNull }

    private fun JsonObject.string(key: String): String? =
        value(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    private fun JsonObject.amount(key: String): Long? {
        val v = value(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
        if (!v.isNumber) return RupiahParser.parse(v.asString)
        // AI kadang menulis angka JSON gaya Indonesia: 47.000 (tanpa kutip) yang oleh JSON
        // dibaca 47.0. Teks aslinya tetap tersedia, jadi pola titik-ribuan dikenali dari sana.
        val literal = v.asString
        return if (THOUSANDS_LITERAL.matches(literal)) RupiahParser.parse(literal)
        else RupiahParser.fromJsonNumber(v.asDouble)
    }

    private val THOUSANDS_LITERAL = Regex("-?\\d{1,3}(\\.\\d{3})+")

    private fun JsonObject.quantity(key: String): Double? {
        val v = value(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
        return if (v.isNumber) v.asDouble else RupiahParser.parseQuantity(v.asString)
    }

    private fun JsonObject.arrayOrEmpty(key: String): List<JsonElement> =
        value(key)?.takeIf { it.isJsonArray }?.asJsonArray?.toList() ?: emptyList()
}
