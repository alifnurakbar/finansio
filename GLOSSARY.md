# Glosarium Finansio

Istilah yang dipakai di aplikasi, kode, dan dokumentasi Finansio.

## Scan struk

**Struk**
Bukti belanja tercetak dari toko yang difoto pengguna. Satu struk dicatat sebagai beberapa transaksi pengeluaran, satu untuk setiap item struk.
_Hindari_: nota, bon, receipt (dalam teks untuk pengguna)

**Item struk**
Satu barang yang dibeli di sebuah struk, beserta jumlah dan harganya. Baris ringkasan (TOTAL, BAYAR, KEMBALI, PPN, diskon struk) **bukan** item struk.

**Jumlah (quantity)**
Banyaknya barang pada satu item struk, misalnya "2 PCS". Untuk barang timbang boleh berupa pecahan, misalnya "1,5 KG".

**Harga satuan**
Harga satu unit barang seperti yang tercetak di struk.
_Hindari_: "harga" saja, karena bisa tertukar dengan subtotal baris

**Subtotal baris**
Nominal paling kanan pada baris item struk, yaitu harga satuan × jumlah dikurangi diskon item. Bila berbeda dengan hasil perkalian, angka yang tercetak inilah yang dipercaya.

**Diskon item**
Potongan harga yang tercetak tepat di bawah satu item dan hanya berlaku untuk item itu.

**Diskon struk**
Potongan harga untuk seluruh belanja, dicetak di bagian ringkasan struk.

**TOTAL struk**
Nominal yang harus dibayar, seperti yang tercetak di struk. Ini **bukan** jumlah uang yang diberikan pembeli (BAYAR/TUNAI) dan **bukan** uang kembalian (KEMBALI).

**Selisih struk**
Beda antara jumlah semua item hasil scan dan TOTAL struk. Bila tidak nol, pasti ada item yang salah baca, dan pengguna diminta memeriksanya sebelum menyimpan.
