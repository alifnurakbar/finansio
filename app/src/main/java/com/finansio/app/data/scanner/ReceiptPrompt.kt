package com.finansio.app.data.scanner

/** Instruksi untuk AI pembaca struk. Dipakai baik saat AI melihat gambar maupun hanya teks OCR. */
object ReceiptPrompt {

    fun instructions(expenseCategories: String): String = """
        Kamu membaca STRUK BELANJA dari Indonesia. Salin angka PERSIS seperti yang tercetak. Jangan menebak atau mengarang.

        Cara membaca struk:
        1. Satu item biasanya terdiri dari nama barang, lalu baris "jumlah satuan x harga_satuan subtotal".
           Nama dan angkanya bisa berada di baris yang berbeda. Pasangkan setiap angka dengan barang di atasnya.
        2. Format angka Indonesia: TITIK adalah pemisah ribuan. "27.500" = 27500, "1.250.000" = 1250000.
        3. "quantity" = jumlah barang (angka sebelum PCS/RTG/BKS/BTL/KG/X). Barang timbang boleh desimal (1.5). Default 1.
        4. "unit_price" = harga satuan. "line_total" = angka paling kanan pada baris item (subtotal item itu).
           Bila hanya ada satu angka untuk sebuah item, isi "line_total" dengan angka itu dan "unit_price" null.
        5. Diskon yang tercetak tepat di bawah sebuah item adalah diskon item: kurangkan dari "line_total" item itu.
        6. JANGAN jadikan item: SUBTOTAL, TOTAL, GRAND TOTAL, BAYAR, TUNAI, CASH, KEMBALI, DEBIT, QRIS,
           PPN, PAJAK, DPP, SERVICE, DISKON/POTONGAN untuk seluruh struk, PEMBULATAN. Isi nilainya ke field ringkasan.
        7. "total" = TOTAL/GRAND TOTAL yang harus dibayar. Bukan BAYAR/TUNAI dan bukan KEMBALI.
        8. "date" = tanggal transaksi dengan format YYYY-MM-DD. Tanggal di struk Indonesia ditulis hari-bulan-tahun,
           dan tahun 2 digit "26" berarti 2026. Isi null bila tidak terbaca.
        9. Angka yang tidak terbaca diisi null, jangan diisi 0.
        10. Sebelum menjawab, cek ulang: jumlah semua line_total + tax + service_charge - discount harus sama dengan total.
            Jika tidak sama, baca ulang struknya.

        Kategori pengeluaran yang tersedia: [$expenseCategories]

        Jawab HANYA dengan JSON dengan key berikut (tanpa key lain):
        {
          "merchant_name": "nama toko atau null",
          "date": "YYYY-MM-DD atau null",
          "items": [
            {
              "name": "nama barang seperti tercetak",
              "quantity": 1,
              "unit_price": 0,
              "line_total": 0,
              "suggested_category": "nama kategori dari daftar di atas (sama persis) atau null"
            }
          ],
          "subtotal": null,
          "discount": null,
          "tax": null,
          "service_charge": null,
          "total": 0
        }
        Semua nominal ditulis sebagai angka bulat rupiah tanpa titik dan koma.
    """.trimIndent()

    fun visionUserText(expenseCategories: String, ocrLayoutText: String): String = buildString {
        append(instructions(expenseCategories))
        if (ocrLayoutText.isNotBlank()) {
            append("\n\nSebagai bantuan, ini teks hasil OCR per baris. OCR bisa salah baca, jadi utamakan yang terlihat di GAMBAR:\n---\n")
            append(ocrLayoutText)
            append("\n---")
        }
    }

    fun textOnlyUserText(ocrLayoutText: String): String =
        "Teks struk hasil OCR, disusun per baris sesuai posisinya di struk (kolom dipisah spasi):\n---\n$ocrLayoutText\n---"
}
