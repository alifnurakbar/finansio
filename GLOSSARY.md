# Glosarium Finansio

Istilah yang dipakai di aplikasi, kode, dan dokumen proyek ini. Satu istilah = satu arti.

## Struk

**Struk**
Bukti belanja dari toko yang difoto pengguna untuk dicatat sebagai satu atau beberapa transaksi pengeluaran.
_Hindari_: nota, kuitansi, receipt (di teks untuk pengguna)

**Item**
Satu barang atau jasa yang dibeli dalam sebuah struk. Setiap item menjadi satu transaksi saat disimpan.
Baris ringkasan (TOTAL, BAYAR, KEMBALI, pajak, diskon) **bukan** item.

**Harga satuan**
Harga untuk satu buah item.
_Hindari_: harga (tanpa keterangan), price

**Jumlah**
Banyaknya buah item yang dibeli, misalnya 2 pada "2 x 13.750".
_Hindari_: qty (di teks untuk pengguna)

**Subtotal baris**
Harga satuan × jumlah untuk satu item, yaitu angka yang tercetak di kanan baris item.
Berbeda dari **Subtotal** struk (lihat di bawah).

**Subtotal**
Jumlah semua subtotal baris sebelum pajak, service charge, diskon, dan pembulatan.

**TOTAL struk**
Angka yang harus dibayar pembeli, seperti yang tercetak di struk. Ini patokan untuk memeriksa hasil scan.
Bukan uang yang diserahkan (**Bayar**) dan bukan **Kembalian**.

**Bayar**
Uang yang diserahkan pembeli ke kasir (tunai, debit, QRIS). Bisa lebih besar dari TOTAL struk.

**Kembalian**
Selisih Bayar dan TOTAL struk yang dikembalikan ke pembeli.

**Penyesuaian**
Angka di bawah subtotal yang mengubah TOTAL struk: pajak (PPN/PB1), service charge, diskon, dan pembulatan.
Penyesuaian yang sudah termasuk dalam harga item (misalnya "PPN termasuk") tidak mengubah TOTAL struk.

**Cocok dengan TOTAL**
Keadaan ketika jumlah subtotal baris semua item (setelah penyesuaian dibagi ke item) sama persis dengan TOTAL struk.

**Selisih**
Jumlah subtotal baris semua item dikurangi TOTAL struk, bila tidak cocok. Pengguna yang mengoreksi item, aplikasi tidak mengakali angkanya.

**Harga belum terbaca**
Item yang harganya tidak bisa dibaca dari struk. Item ini tidak boleh dianggap Rp 0; pengguna harus mengisinya.
