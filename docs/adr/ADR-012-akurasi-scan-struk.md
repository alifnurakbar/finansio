# ADR-012: Akurasi Scan Struk (Model Vision + Pemeriksaan TOTAL)

**Status:** Accepted
**Tanggal:** 2026-11-17
**Author:** Alif Nur Akbar

## Konteks
Hasil scan struk sering tidak sesuai dengan struk aslinya: kadang nominalnya lebih besar, kadang lebih kecil, bahkan pernah semuanya Rp 0. Contohnya struk TOKO ABENG (TOTAL Rp266.200) yang terbaca sebagai 5 item dengan nominal Rp 0 semua. Ada lima penyebab:

1. **Kolom terpisah.** Alur lama memakai `visionText.text` dari ML Kit, yang menyusun teks per *blok*. Kolom nama barang dan kolom harga jadi terpisah jauh, sehingga semua harga muncul setelah kata TOTAL dan BAYAR. AI tidak bisa memasangkan harga dengan barangnya.
2. **Arti "price" tidak jelas.** Prompt tidak menyebut apakah `price` itu harga satuan atau subtotal baris. Padahal aplikasi menyimpan `price × quantity`, sehingga subtotal yang terbaca sebagai `price` ikut dikalikan dan nominalnya jadi dua kali lipat atau lebih.
3. **Baris ringkasan ikut terbaca.** TOTAL, BAYAR, KEMBALI, dan PPN bisa terbaca sebagai item. Diskon dan pajak juga tidak diatur.
4. **Angka diubah diam-diam.** Jawaban AI dibaca langsung ke field `Long`, jadi `null` berubah menjadi 0 tanpa ada tanda apa pun. Angka bergaya Indonesia seperti `47.000` (tanpa tanda kutip) terbaca sebagai 47.
5. **Tidak ada pemeriksaan.** `total_amount` dari AI tidak pernah dibandingkan dengan jumlah semua item.

## Keputusan
1. **Hybrid vision + OCR.** Foto struk (diperkecil menjadi maksimal 1600 px, JPEG) dikirim ke model vision Groq `qwen/qwen3.8-27b` bersama teks OCR per baris sebagai bantuan. Bila model vision gagal (dihentikan, HTTP 4xx/5xx, atau tidak menemukan item), aplikasi otomatis kembali ke model teks `openai/gpt-oss-120b` dengan teks OCR per baris.
2. **OCR disusun per baris** berdasarkan posisi (`ReceiptLayout`), sehingga nama barang, jumlah, harga satuan, dan subtotal baris berada di baris yang sama.
3. **Skema JSON yang tegas.** AI mengisi `quantity`, `unit_price`, dan `line_total` secara terpisah, ditambah `subtotal`, `discount`, `tax`, `service_charge`, dan `total`. Prompt menjelaskan format angka Indonesia (titik = ribuan) dan daftar baris yang bukan item.
4. **Pemeriksaan di aplikasi** (`ReceiptReconciler`), bukan hanya mengandalkan AI:
   - Baris ringkasan dibuang dari daftar item.
   - Subtotal baris yang tercetak diutamakan. Harga satuan dan subtotal yang tertukar diperbaiki otomatis.
   - Pajak dan service charge ditambahkan, dan diskon struk dikurangkan, **secara proporsional** ke setiap item. Ini hanya dilakukan bila hasilnya membuat jumlah item sama dengan TOTAL struk, sehingga PPN yang sudah termasuk dalam harga tidak terhitung dua kali.
   - Angka dibaca dengan toleran (`RupiahParser`): "27.500", "27,500.00", "Rp 1.250.000", juga angka JSON `47.000`.
5. **Peringatan selisih.** Layar hasil scan menampilkan "✓ Cocok dengan TOTAL struk" atau jumlah selisihnya. Angka tidak diubah diam-diam agar cocok: pengguna yang mengoreksi item yang salah.

## Alternatif yang dipertimbangkan
| Alternatif | Alasan tidak dipilih |
|---|---|
| Hanya memperbaiki OCR dan prompt (tanpa vision) | Struk kusut, miring, atau tintanya pudar tetap salah baca. Teks OCR saja tidak bisa memperbaiki angka yang salah dibaca OCR. |
| Hanya model vision | Model vision di Groq masih Preview. Tanpa cadangan, fitur scan mati total bila model dihentikan (seperti Llama di ADR-002). |
| Menyesuaikan angka otomatis agar sama dengan TOTAL | Menyembunyikan salah baca. Lebih baik pengguna melihat selisihnya dan mengoreksi item yang benar-benar salah. |
| Menyimpan pajak/service sebagai transaksi terpisah | Pajak tidak punya kategori yang jelas dan mengacaukan statistik per kategori. |

## Konsekuensi
- Setiap scan mengirim foto struk ke Groq, sehingga butuh kuota token gambar dan koneksi internet. Foto tidak disimpan oleh aplikasi.
- Bila `qwen/qwen3.8-27b` dihentikan Groq, scan tetap berjalan lewat model teks. Ganti `GroqApiConstants.VISION_MODEL` ke model vision penggantinya.
- Item dengan jumlah pecahan (barang timbang), atau yang subtotalnya tidak habis dibagi jumlahnya setelah pajak disebar, disimpan sebagai 1 baris dengan nominal persis. Jumlahnya ditulis di nama item, misalnya "APEL (1,5x)".
- Logika pemeriksaan diuji dengan unit test `ReceiptScanLogicTest`, termasuk struk TOKO ABENG.
