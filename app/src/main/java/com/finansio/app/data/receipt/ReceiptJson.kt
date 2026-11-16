package com.finansio.app.data.receipt

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

/** Satu baris barang di struk, persis seperti yang dibaca AI (belum diperiksa). */
data class RawReceiptItem(
    val name: String,
    val quantity: Int?,
    val unitPrice: Long?,
    val lineTotal: Long?,
    val suggestedCategory: String?
)

/** Isi struk seperti yang dibaca AI, termasuk angka ringkasan di bagian bawah struk. */
data class RawReceipt(
    val merchantName: String?,
    val date: String?,
    val items: List<RawReceiptItem>,
    val subtotal: Long?,
    val tax: Long?,
    val serviceCharge: Long?,
    val discount: Long?,
    val rounding: Long?,
    val total: Long?
)

/**
 * Mengubah jawaban JSON dari AI menjadi [RawReceipt].
 *
 * Angka tidak dibaca langsung oleh Gson ke Long, karena `47.000` akan terbaca 47.
 * Semua angka melewati [RupiahParser].
 */
object ReceiptJson {

    fun parse(content: String): RawReceipt {
        val obj = JsonParser.parseString(extractJsonObject(content)).asJsonObject
        val items = obj.getArray("items").mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val item = element.asJsonObject
            val name = item.getString("name")?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            RawReceiptItem(
                name = name,
                quantity = parseQuantity(item.get("quantity")),
                unitPrice = RupiahParser.parse(item.get("unit_price") ?: item.get("price")),
                lineTotal = RupiahParser.parse(item.get("line_total")),
                suggestedCategory = item.getString("suggested_category")
            )
        }
        return RawReceipt(
            merchantName = obj.getString("merchant_name"),
            date = obj.getString("date"),
            items = items,
            subtotal = RupiahParser.parse(obj.get("subtotal")),
            tax = RupiahParser.parse(obj.get("tax")),
            serviceCharge = RupiahParser.parse(obj.get("service_charge")),
            discount = RupiahParser.parse(obj.get("discount")),
            rounding = RupiahParser.parse(obj.get("rounding")),
            total = RupiahParser.parse(obj.get("total_amount"))
        )
    }

    /** Ambil objek JSON dari jawaban AI, membuang blok <think> atau pagar ``` bila ada. */
    internal fun extractJsonObject(content: String): String {
        val cleaned = content.replace(Regex("(?s)<think>.*?</think>"), "")
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) throw JsonSyntaxException("Jawaban AI tidak berisi JSON")
        return cleaned.substring(start, end + 1)
    }

    /** Jumlah barang harus bilangan bulat positif. Jumlah pecahan (misalnya 0,5 kg) dianggap tidak diketahui. */
    private fun parseQuantity(element: JsonElement?): Int? {
        if (element == null || element.isJsonNull || !element.isJsonPrimitive) return null
        val text = element.asString.trim().replace(",", ".")
        val value = text.toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null
        return try {
            value.intValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }

    private fun JsonObject.getString(key: String): String? {
        val element = get(key) ?: return null
        if (element.isJsonNull || !element.isJsonPrimitive) return null
        return element.asString.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    private fun JsonObject.getArray(key: String): List<JsonElement> {
        val element = get(key) ?: return emptyList()
        return if (element.isJsonArray) element.asJsonArray.toList() else emptyList()
    }
}
