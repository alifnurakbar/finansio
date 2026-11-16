package com.finansio.app.data.receipt

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrRowBuilderTest {

    /** Baris setinggi 20 px; nama di kiri (x=10), qty di tengah (x=200), harga di kanan (x=320). */
    private fun left(text: String, row: Int) = OcrFragment(text, 10, row * 30, 180, row * 30 + 20)
    private fun mid(text: String, row: Int) = OcrFragment(text, 200, row * 30 + 2, 280, row * 30 + 22)
    private fun right(text: String, row: Int) = OcrFragment(text, 320, row * 30 - 2, 400, row * 30 + 18)

    @Test
    fun `harga yang dibaca per kolom kembali ke baris barangnya`() {
        // Urutan seperti keluaran ML Kit: kolom nama dulu, lalu kolom angka setelah TOTAL dan BAYAR.
        val fragments = listOf(
            left("GULA PASIR 1KG", 0),
            left("MINYAK GORENG 2L", 1),
            left("TOTAL", 2),
            left("BAYAR", 3),
            mid("2 x 17.500", 0),
            right("35.000", 0),
            right("38.900", 1),
            right("73.900", 2),
            right("100.000", 3)
        )

        val expected = """
            GULA PASIR 1KG  2 x 17.500  35.000
            MINYAK GORENG 2L  38.900
            TOTAL  73.900
            BAYAR  100.000
        """.trimIndent()
        assertEquals(expected, OcrRowBuilder.buildRows(fragments))
    }

    @Test
    fun `teks kosong diabaikan`() {
        assertEquals("", OcrRowBuilder.buildRows(listOf(OcrFragment("  ", 0, 0, 10, 10))))
    }
}
