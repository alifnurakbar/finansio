package com.finansio.app.data.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptCheckerTest {

    private fun item(name: String, qty: Int?, unit: Long?, line: Long?) =
        RawReceiptItem(name, qty, unit, line, null)

    private fun receipt(
        items: List<RawReceiptItem>,
        total: Long?,
        tax: Long? = null,
        service: Long? = null,
        discount: Long? = null
    ) = RawReceipt("Toko Contoh", "2026-11-17", items, null, tax, service, discount, null, total)

    /**
     * Struk sintetis dengan bentuk yang sama seperti kasus aslinya: 5 barang, salah satunya
     * dibeli lebih dari satu, ditambah baris TOTAL/BAYAR/KEMBALI yang ikut terbaca sebagai item.
     */
    @Test
    fun `struk minimarket lima item cocok dengan TOTAL`() {
        val json = """
            {
              "merchant_name": "Toko Contoh",
              "date": "2026-11-17",
              "items": [
                {"name": "GULA PASIR 1KG", "quantity": 2, "unit_price": 17.500, "line_total": 35.000},
                {"name": "MINYAK GORENG 2L", "quantity": 1, "unit_price": 38900, "line_total": 38900},
                {"name": "BERAS 5KG", "quantity": 1, "unit_price": null, "line_total": "72.000"},
                {"name": "TELUR 1KG", "quantity": 1, "unit_price": 29800, "line_total": null},
                {"name": "SUSU UHT 1L", "quantity": 6, "unit_price": 90000, "line_total": 90000},
                {"name": "TOTAL", "quantity": 1, "unit_price": 265700, "line_total": 265700},
                {"name": "BAYAR", "quantity": 1, "unit_price": 300000, "line_total": 300000},
                {"name": "KEMBALI", "quantity": 1, "unit_price": 34300, "line_total": 34300}
              ],
              "total_amount": 265.700
            }
        """.trimIndent()

        val checked = ReceiptChecker.check(ReceiptJson.parse(json))

        assertEquals(listOf("GULA PASIR 1KG", "MINYAK GORENG 2L", "BERAS 5KG", "TELUR 1KG", "SUSU UHT 1L"), checked.items.map { it.name })
        assertEquals(listOf(35_000L, 38_900L, 72_000L, 29_800L, 90_000L), checked.items.map { it.lineTotal })
        val susu = checked.items.last()
        assertEquals(6, susu.quantity)
        assertEquals(15_000L, susu.unitPrice)
        assertEquals(265_700L, checked.receiptTotal)
        assertEquals(TotalCheck.Match, ReceiptChecker.compare(checked.items.sumOf { it.lineTotal }, checked.receiptTotal))
    }

    /**
     * Jawaban AI asli dari scan struk Toko Abeng di HP (17-11-2026). Struk ini mencetak tiap barang dalam
     * dua baris, dan AI menjadikan baris jumlah ("PCSX", "RTGx", ...) sebagai item tersendiri.
     */
    @Test
    fun `struk Toko Abeng dua baris digabung menjadi lima barang dengan TOTAL 266200`() {
        val json = """
            {"merchant_name":"TOKO ABENG","date":"2026-09-29","items":[
            {"name":"TOTOLE 200G","quantity":1,"unit_price":null,"line_total":null,"suggested_category":"Makanan"},
            {"name":"PCSX","quantity":2,"unit_price":27500,"line_total":55000,"suggested_category":"Makanan"},
            {"name":"GOOD DAY CAPPUCINO RTG","quantity":1,"unit_price":null,"line_total":null,"suggested_category":"Makanan"},
            {"name":"RTGx","quantity":1,"unit_price":21000,"line_total":21000,"suggested_category":"Makanan"},
            {"name":"MAXTEA TARIY","quantity":1,"unit_price":null,"line_total":null,"suggested_category":"Makanan"},
            {"name":"RTG","quantity":2,"unit_price":23500,"line_total":47000,"suggested_category":"Makanan"},
            {"name":"MAMA LEMON 610G","quantity":1,"unit_price":null,"line_total":null,"suggested_category":"Makanan"},
            {"name":"PCS X","quantity":4,"unit_price":8000,"line_total":32000,"suggested_category":"Makanan"},
            {"name":"MUANTEB 1KG","quantity":1,"unit_price":null,"line_total":null,"suggested_category":"Makanan"},
            {"name":"PCS X","quantity":4,"unit_price":27800,"line_total":111200,"suggested_category":"Makanan"}],
            "subtotal":null,"tax":null,"service_charge":null,"discount":null,"rounding":null,"total_amount":266200}
        """.trimIndent()

        val checked = ReceiptChecker.check(ReceiptJson.parse(json))

        assertEquals(
            listOf("TOTOLE 200G", "GOOD DAY CAPPUCINO RTG", "MAXTEA TARIY", "MAMA LEMON 610G", "MUANTEB 1KG"),
            checked.items.map { it.name }
        )
        assertEquals(listOf(2, 1, 2, 4, 4), checked.items.map { it.quantity })
        assertEquals(listOf(27_500L, 21_000L, 23_500L, 8_000L, 27_800L), checked.items.map { it.unitPrice })
        assertTrue(checked.items.none { it.priceMissing })
        assertEquals(TotalCheck.Match, ReceiptChecker.compare(checked.items.sumOf { it.lineTotal }, checked.receiptTotal))
    }

    @Test
    fun `baris jumlah dan satuan dikenali, nama barang tidak`() {
        listOf("PCSX", "2 PCSX", "1 RTGx", "RTG", "2RTG", "PCS X", "4 PCS  X", "X").forEach {
            assertTrue(it, ReceiptChecker.isQuantityLine(it))
        }
        listOf("MUANTEB 1KG", "TOTOLE 200G", "GOOD DAY CAPPUCINO RTG", "KOPI").forEach {
            assertTrue(it, !ReceiptChecker.isQuantityLine(it))
        }
    }

    @Test
    fun `subtotal yang ditulis sebagai harga satuan tidak menjadi dobel`() {
        val fixed = ReceiptChecker.fixLine(item("KOPI", 2, 27_500, 27_500))
        assertEquals(13_750L, fixed.unitPrice)
        assertEquals(27_500L, fixed.lineTotal)
    }

    @Test
    fun `harga satuan dan subtotal yang tertukar dibetulkan`() {
        val fixed = ReceiptChecker.fixLine(item("KOPI", 2, 27_500, 13_750))
        assertEquals(13_750L, fixed.unitPrice)
        assertEquals(27_500L, fixed.lineTotal)
    }

    @Test
    fun `jumlah barang yang salah baca dihitung dari subtotal`() {
        val fixed = ReceiptChecker.fixLine(item("KOPI", 1, 13_750, 27_500))
        assertEquals(2, fixed.quantity)
        assertEquals(27_500L, fixed.lineTotal)
    }

    @Test
    fun `harga satuan tidak bulat tetap menjaga subtotal`() {
        val fixed = ReceiptChecker.fixLine(item("APEL 3 BUAH", 3, null, 10_000))
        assertEquals(1, fixed.quantity)
        assertEquals(10_000L, fixed.unitPrice)
        assertEquals(10_000L, fixed.lineTotal)
    }

    @Test
    fun `harga kosong ditandai, tidak diam-diam menjadi nol`() {
        val fixed = ReceiptChecker.fixLine(item("ROTI", 1, null, null))
        assertTrue(fixed.priceMissing)
    }

    @Test
    fun `PPN disebar ke item bila membuat jumlah sama dengan TOTAL`() {
        val checked = ReceiptChecker.check(
            receipt(listOf(item("NASI GORENG", 1, 25_000, 25_000), item("ES TEH", 2, 5_000, 10_000)), total = 38_500, tax = 3_500)
        )
        assertEquals(listOf(27_500L, 11_000L), checked.items.map { it.lineTotal })
        assertEquals(5_500L, checked.items[1].unitPrice)
        assertEquals(listOf(Adjustment("Pajak", 3_500)), checked.spreadAdjustments)
    }

    @Test
    fun `PPN yang sudah termasuk harga tidak dihitung dua kali`() {
        val checked = ReceiptChecker.check(
            receipt(listOf(item("SABUN", 1, 11_100, 11_100)), total = 11_100, tax = 1_100)
        )
        assertEquals(11_100L, checked.items.single().lineTotal)
        assertTrue(checked.spreadAdjustments.isEmpty())
    }

    @Test
    fun `service dan diskon disebar bersamaan, totalnya tepat`() {
        val checked = ReceiptChecker.check(
            receipt(
                listOf(item("A", 1, 10_000, 10_000), item("B", 1, 10_000, 10_000), item("C", 1, 10_000, 10_000)),
                total = 30_500, service = 1_500, discount = 1_000
            )
        )
        assertEquals(30_500L, checked.items.sumOf { it.lineTotal })
        assertEquals(2, checked.spreadAdjustments.size)
    }

    @Test
    fun `baris PPN dan diskon yang terbaca sebagai item dipindah ke penyesuaian`() {
        val checked = ReceiptChecker.check(
            receipt(
                listOf(item("BAKSO", 1, 20_000, 20_000), item("PPN 10%", 1, 2_000, 2_000), item("DISKON MEMBER", 1, -1_000, -1_000)),
                total = 21_000
            )
        )
        assertEquals(listOf("BAKSO"), checked.items.map { it.name })
        assertEquals(21_000L, checked.items.single().lineTotal)
    }

    @Test
    fun `selisih ditunjukkan apa adanya bila tidak cocok`() {
        val checked = ReceiptChecker.check(
            receipt(listOf(item("A", 1, 10_000, 10_000), item("B", 1, 5_000, 5_000)), total = 20_000, tax = 1_000)
        )
        assertEquals(15_000L, checked.items.sumOf { it.lineTotal })
        assertTrue(checked.spreadAdjustments.isEmpty())
        assertEquals(TotalCheck.Mismatch(-5_000), ReceiptChecker.compare(15_000, checked.receiptTotal))
        assertEquals(TotalCheck.Unknown, ReceiptChecker.compare(15_000, null))
    }

    @Test
    fun `penyebaran selalu tepat walau ada sisa pembagian`() {
        val items = listOf(
            CheckedItem("A", 1, 3_333, 3_333, null),
            CheckedItem("B", 1, 3_333, 3_333, null),
            CheckedItem("C", 1, 3_334, 3_334, null),
            CheckedItem("D", 1, 0, 0, null, priceMissing = true)
        )
        val result = ReceiptChecker.distribute(items, 1_001)
        assertEquals(11_001L, result.sumOf { it.lineTotal })
        assertEquals(0L, result.last().lineTotal)
    }

    @Test
    fun `jawaban AI dengan blok think dan pagar kode tetap terbaca`() {
        val content = "<think>hitung dulu</think>\n```json\n{\"items\": [{\"name\": \"A\", \"price\": \"12.000\"}], \"total_amount\": 12000}\n```"
        val raw = ReceiptJson.parse(content)
        assertEquals(12_000L, raw.items.single().unitPrice)
        assertEquals(12_000L, raw.total)
    }
}
