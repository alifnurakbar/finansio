package com.finansio.app.data.receipt

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RupiahParserTest {

    @Test
    fun `titik diikuti tiga digit adalah pemisah ribuan`() {
        assertEquals(47_000L, RupiahParser.parse("47.000"))
        assertEquals(1_250_000L, RupiahParser.parse("1.250.000"))
        assertEquals(27_500L, RupiahParser.parse("Rp 27.500"))
        assertEquals(1_250_000L, RupiahParser.parse("1.250.000,00"))
    }

    @Test
    fun `format Inggris dan desimal tetap terbaca`() {
        assertEquals(27_500L, RupiahParser.parse("27,500"))
        assertEquals(27_500L, RupiahParser.parse("27500.00"))
        assertEquals(27_500L, RupiahParser.parse("27500"))
    }

    @Test
    fun `angka JSON 47 titik 000 tidak menjadi 47`() {
        val obj = JsonParser.parseString("""{"price": 47.000, "plain": 47}""").asJsonObject
        assertEquals(47_000L, RupiahParser.parse(obj.get("price")))
        assertEquals(47L, RupiahParser.parse(obj.get("plain")))
    }

    @Test
    fun `angka negatif untuk diskon`() {
        assertEquals(-5_000L, RupiahParser.parse("-5.000"))
        assertEquals(-5_000L, RupiahParser.parse("5.000-"))
        assertEquals(-5_000L, RupiahParser.parse("(5.000)"))
    }

    @Test
    fun `teks yang bukan angka menjadi null, bukan nol`() {
        assertNull(RupiahParser.parse("abc"))
        assertNull(RupiahParser.parse(""))
        assertNull(RupiahParser.parse(null as String?))
        assertNull(RupiahParser.parse(JsonParser.parseString("""{"a": null}""").asJsonObject.get("a")))
    }
}
