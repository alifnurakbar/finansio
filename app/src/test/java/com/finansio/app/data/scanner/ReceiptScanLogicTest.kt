package com.finansio.app.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uji logika scan struk memakai struk nyata "TOKO ABENG" (TOTAL 266.200). */
class ReceiptScanLogicTest {

    // ---------- RupiahParser ----------

    @Test
    fun parsesIndonesianAmounts() {
        assertEquals(27500L, RupiahParser.parse("27.500"))
        assertEquals(1250000L, RupiahParser.parse("Rp 1.250.000"))
        assertEquals(27500L, RupiahParser.parse("27,500.00"))
        assertEquals(27500L, RupiahParser.parse("27.500,00"))
        assertEquals(55000L, RupiahParser.parse("55.000,-"))
        assertEquals(-33800L, RupiahParser.parse("-33.800"))
        assertEquals(21000L, RupiahParser.parse("21.0O0"))
        assertEquals(8000L, RupiahParser.parse("8000"))
        assertNull(RupiahParser.parse("TOTAL"))
        assertNull(RupiahParser.parse(null))
    }

    @Test
    fun fractionalJsonNumberIsTreatedAsThousands() {
        assertEquals(27500L, RupiahParser.fromJsonNumber(27.5))
        assertEquals(266200L, RupiahParser.fromJsonNumber(266200.0))
    }

    @Test
    fun parsesQuantities() {
        assertEquals(2.0, RupiahParser.parseQuantity("2 PCS")!!, 0.0)
        assertEquals(1.5, RupiahParser.parseQuantity("1,5 KG")!!, 0.0)
        assertNull(RupiahParser.parseQuantity("PCS"))
    }

    // ---------- ReceiptLayout ----------

    @Test
    fun layoutPutsNamesAndPricesOnTheSameRow() {
        // Meniru ML Kit: kolom kiri, tengah, dan kanan datang sebagai blok terpisah.
        val lines = listOf(
            OcrLine("TOTOLE 200G", 10, 100, 200, 120),
            OcrLine("2 PCS x", 40, 130, 140, 150),
            OcrLine("TOTAL", 150, 200, 230, 222),
            OcrLine("27.500", 260, 132, 340, 152),
            OcrLine("55.000", 420, 131, 500, 151),
            OcrLine("266.200", 410, 201, 500, 223)
        )
        val text = ReceiptLayout.toText(lines)
        assertEquals("TOTOLE 200G\n2 PCS x   27.500   55.000\nTOTAL   266.200", text)
    }

    @Test
    fun layoutKeepsStackedTextOnSeparateRows() {
        val lines = listOf(
            OcrLine("MAMA LEMON 610G", 10, 100, 200, 120),
            OcrLine("4 PCS", 12, 112, 80, 132) // bertumpuk horizontal -> baris lain
        )
        assertEquals("MAMA LEMON 610G\n4 PCS", ReceiptLayout.toText(lines))
    }

    // ---------- ReceiptReconciler ----------

    private fun abengItems(transform: (RawReceiptItem) -> RawReceiptItem = { it }) = listOf(
        RawReceiptItem("TOTOLE 200G", 2.0, 27500, 55000),
        RawReceiptItem("GOOD DAY CAPPUCINO RTG", 1.0, 21000, 21000),
        RawReceiptItem("MAXTEA TARIK", 2.0, 23500, 47000),
        RawReceiptItem("MAMA LEMON 610G", 4.0, 8000, 32000),
        RawReceiptItem("MUANTEB 1KG", 4.0, 27800, 111200)
    ).map(transform)

    @Test
    fun tokoAbengMatchesPrintedTotal() {
        val receipt = ReceiptReconciler.reconcile(
            RawReceipt("TOKO ABENG", "2026-09-29", abengItems(), total = 266200)
        )
        assertEquals(5, receipt.lines.size)
        assertEquals(266200L, receipt.linesTotal)
        assertTrue(receipt.matchesPrintedTotal)
        assertEquals(27500L, receipt.lines[0].unitPrice)
        assertEquals(2, receipt.lines[0].quantity)
    }

    @Test
    fun summaryRowsAreNotItems() {
        val items = abengItems() + listOf(
            RawReceiptItem("TOTAL", 1.0, 266200, 266200),
            RawReceiptItem("BAYAR", 1.0, 300000, 300000),
            RawReceiptItem("KEMBALI", 1.0, null, 33800),
            RawReceiptItem("PPN 11%", 1.0, null, 1000)
        )
        val receipt = ReceiptReconciler.reconcile(RawReceipt("TOKO ABENG", null, items, total = 266200))
        assertEquals(5, receipt.lines.size)
        assertEquals(266200L, receipt.linesTotal)
    }

    @Test
    fun lineTotalUsedAsUnitPriceIsNotDoubled() {
        // Kesalahan lama: AI mengisi price = subtotal baris, lalu app mengalikan lagi dengan qty.
        val items = listOf(RawReceiptItem("TOTOLE 200G", 2.0, 55000, 27500)) // kolom tertukar
        val receipt = ReceiptReconciler.reconcile(RawReceipt(null, null, items, total = 55000))
        assertEquals(27500L, receipt.lines[0].unitPrice)
        assertEquals(55000L, receipt.linesTotal)
    }

    @Test
    fun printedLineTotalWinsOverMultiplication() {
        val items = listOf(RawReceiptItem("MUANTEB 1KG", 4.0, 27000, 111200)) // harga satuan salah baca
        val receipt = ReceiptReconciler.reconcile(RawReceipt(null, null, items, total = 111200))
        assertEquals(111200L, receipt.linesTotal)
        assertEquals(27800L, receipt.lines[0].unitPrice)
    }

    @Test
    fun missingPricesStayZeroAndAreFlagged() {
        val items = abengItems { it.copy(unitPrice = null, lineTotal = null) }
        val receipt = ReceiptReconciler.reconcile(RawReceipt(null, null, items, total = 266200))
        assertEquals(0L, receipt.linesTotal)
        assertFalse(receipt.matchesPrintedTotal)
    }

    @Test
    fun taxAndServiceAreSpreadWhenTheyExplainTheTotal() {
        val items = listOf(
            RawReceiptItem("NASI GORENG", 1.0, 30000, 30000),
            RawReceiptItem("ES TEH", 2.0, 5000, 10000)
        )
        // 40.000 + PPN 4.000 + service 2.000 = 46.000
        val receipt = ReceiptReconciler.reconcile(
            RawReceipt(null, null, items, tax = 4000, serviceCharge = 2000, total = 46000)
        )
        assertEquals(46000L, receipt.linesTotal)
        assertTrue(receipt.matchesPrintedTotal)
        assertEquals(34500L, receipt.lines[0].amount)
        assertEquals(11500L, receipt.lines[1].amount)
    }

    @Test
    fun includedTaxIsNotAddedTwice() {
        // Struk ritel: harga sudah termasuk PPN, PPN hanya dicetak sebagai informasi.
        val items = listOf(RawReceiptItem("SABUN", 1.0, 11100, 11100))
        val receipt = ReceiptReconciler.reconcile(RawReceipt(null, null, items, tax = 1100, total = 11100))
        assertEquals(11100L, receipt.linesTotal)
    }

    @Test
    fun receiptDiscountIsSubtractedProportionally() {
        val items = listOf(
            RawReceiptItem("A", 1.0, 10000, 10000),
            RawReceiptItem("B", 1.0, 20000, 20000)
        )
        val receipt = ReceiptReconciler.reconcile(RawReceipt(null, null, items, discount = 3000, total = 27000))
        assertEquals(27000L, receipt.linesTotal)
        assertEquals(9000L, receipt.lines[0].amount)
        assertEquals(18000L, receipt.lines[1].amount)
    }

    @Test
    fun weighedItemKeepsExactAmount() {
        val items = listOf(RawReceiptItem("APEL", 1.5, 20000, 30000))
        val receipt = ReceiptReconciler.reconcile(RawReceipt(null, null, items, total = 30000))
        assertEquals("APEL (1,5x)", receipt.lines[0].name)
        assertEquals(30000L, receipt.lines[0].amount)
    }

    // ---------- ReceiptJsonReader ----------

    @Test
    fun readsAiJsonWithStringAmountsAndThinkingBlock() {
        val content = """
            <think>membaca struk...</think>
            ```json
            {"merchant_name":"TOKO ABENG","date":"2026-09-29",
             "items":[
               {"name":"TOTOLE 200G","quantity":"2 PCS","unit_price":"27.500","line_total":"55.000","suggested_category":"Belanja"},
               {"name":"GOOD DAY CAPPUCINO RTG","quantity":1,"unit_price":21000,"line_total":21000},
               {"name":"MAXTEA TARIK","quantity":2,"unit_price":23.5,"line_total":47.000},
               {"name":"MAMA LEMON 610G","quantity":4,"unit_price":8000,"line_total":32000},
               {"name":"MUANTEB 1KG","quantity":4,"unit_price":"27.800","line_total":"111.200"}
             ],
             "subtotal":null,"discount":null,"tax":null,"service_charge":null,"total":"266.200"}
            ```
        """.trimIndent()
        val receipt = ReceiptReconciler.reconcile(ReceiptJsonReader.read(content))
        assertEquals("TOKO ABENG", receipt.merchantName)
        assertEquals(266200L, receipt.printedTotal)
        assertEquals(266200L, receipt.linesTotal)
        assertTrue(receipt.matchesPrintedTotal)
        assertEquals("Belanja", receipt.lines[0].suggestedCategory)
    }

    @Test
    fun readsLegacyKeys() {
        val raw = ReceiptJsonReader.read("""{"items":[{"name":"X","price":5000,"quantity":2}],"total_amount":10000}""")
        assertEquals(5000L, raw.items[0].unitPrice)
        assertEquals(10000L, raw.total)
    }
}
