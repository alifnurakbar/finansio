# ADR-012: Akurasi Scan Struk

**Status:** Accepted
**Tanggal:** 2026-11-17
**Author:** Alif Nur Akbar

## Konteks
Hasil scan struk sering salah: banyak item berharga Rp 0, ada yang dobel, dan jumlahnya tidak sama dengan TOTAL struk. Penyebabnya:

1. **ML Kit membaca struk per kolom, bukan per baris.** Teks mentahnya berisi semua nama barang lebih dulu, lalu semua harga baru muncul di akhir (setelah TOTAL dan BAYAR). LLM tidak bisa memasangkan harga dengan barangnya.
2. **Harga kosong diam-diam menjadi Rp 0.** Gson mengisi `price` dengan 0 bila AI tidak memberi nilai.
3. **Harga dikali jumlah.** Saat menyimpan, aplikasi menghitung `price × quantity`. Bila AI menaruh subtotal baris di `price`, hasilnya dobel.
4. **Format angka Indonesia.** Bila AI menulis `47.000` sebagai angka JSON, Gson membacanya 47.
5. **Baris ringkasan ikut dianggap item** (TOTAL, BAYAR, KEMBALI, PPN).

## Keputusan

### 1. AI membaca foto struk langsung
Foto struk (diperkecil ke sekitar 2 megapiksel, JPEG) dikirim ke model vision **`qwen/qwen3.8-27b`** di Groq bersama teks OCR. Foto adalah sumber utama; teks OCR hanya bantuan. Pada 2026-11-17 model ini terdaftar di akun Groq proyek, menerima gambar, dan mendukung mode JSON (diuji dengan struk sintetis: 5 item cocok dengan TOTAL).

Model ini berstatus **Preview** di Groq dan bisa dihentikan sewaktu-waktu. Karena itu, bila panggilan model vision gagal (kecuali API key salah atau tidak ada internet) atau jawabannya bukan JSON yang valid, scan **otomatis kembali** ke model teks `openai/gpt-oss-120b` dengan teks OCR saja.

### 2. Teks OCR disusun ulang per baris
Baris teks ML Kit dikelompokkan berdasarkan posisi vertikalnya (`OcrRowBuilder`), lalu diurutkan dari kiri ke kanan. Nama barang dan harganya kembali berada di baris yang sama. Ini membantu model vision dan menjadi satu-satunya masukan bagi model teks cadangan.

### 3. Format jawaban AI yang lebih tegas
AI diminta mengisi `unit_price`, `quantity`, dan `line_total` secara terpisah, serta field ringkasan `subtotal`, `tax`, `service_charge`, `discount`, `rounding`, dan `total_amount`. Prompt menjelaskan bahwa titik adalah pemisah ribuan, baris ringkasan bukan item, dan harga yang tidak terbaca diisi `null`, bukan 0.

### 4. Aplikasi memeriksa ulang jawaban AI (`ReceiptChecker`)
- Angka dibaca dengan `RupiahParser`, bukan langsung oleh Gson, sehingga `47.000` = 47000.
- Baris ringkasan yang terlanjur menjadi item dibuang. Bila baris itu pajak, service charge, diskon, atau pembulatan, angkanya dipindah ke penyesuaian.
- Harga satuan, jumlah, dan subtotal baris dibuat konsisten. Subtotal baris lebih dipercaya karena angka itulah yang dijumlahkan menjadi TOTAL struk. Kasus yang diperbaiki: harga satuan dan subtotal tertukar, subtotal ditulis sebagai harga satuan, dan jumlah yang salah baca.
- Penyesuaian dibagi ke item sebanding subtotal barisnya, **hanya bila** hasilnya tepat sama dengan TOTAL struk. Jadi PPN yang sudah termasuk harga tidak terhitung dua kali.
- Item tanpa harga ditandai **Harga belum terbaca**, tidak menjadi Rp 0.

### 5. Layar hasil scan menunjukkan pengecekan TOTAL
Di atas tombol Simpan tampil "✓ Cocok dengan TOTAL struk", atau besar selisihnya bila tidak cocok. Pengecekan dihitung ulang setiap kali pengguna mengubah atau menghapus item. **Angka tidak pernah diakali supaya cocok**; pengguna yang mengoreksi item yang salah.

## Pembaruan setelah uji di HP (2026-11-17)
Uji pertama di HP (struk Toko Abeng asli) menemukan dua masalah:

1. **Model vision ditolak Groq (HTTP 429).** Kuota gratis `qwen/qwen3.8-27b` adalah 1000 token output dan 7000 token input per menit. Dengan `max_tokens` 4096, Groq memperkirakan output terlalu besar dan menolak permintaan sebelum diproses, sehingga scan selalu jatuh ke model teks. `max_tokens` untuk model vision diturunkan ke 900 dan AI diminta menjawab JSON ringkas. Karena kuota input, kira-kira hanya satu scan foto per menit; scan berikutnya dalam menit yang sama memakai model teks.
2. **Struk dua baris.** Struk Toko Abeng mencetak nama barang di satu baris, lalu `2 PCSX 27.500 55.000` di baris berikutnya. Teks OCR per baris sudah benar, tetapi model teks menjadikan baris jumlah ("PCSX", "RTGx") sebagai item tersendiri dan nama barangnya tanpa harga. Perbaikannya: prompt menjelaskan format dua baris dengan contoh, dan `ReceiptChecker` menggabungkan baris jumlah ke nama barang di atasnya bila nama itu belum punya harga. Dengan teks OCR asli dari HP, model teks menghasilkan 5 item cocok dengan TOTAL Rp 266.200 pada 3 dari 3 percobaan.

## Alternatif yang dipertimbangkan
- **Hanya memperbaiki prompt dan susunan OCR, tanpa foto.** Lebih murah dan tidak memakai kuota gambar, tetapi tetap bergantung pada OCR yang bisa salah baca angka. Tetap dipakai sebagai jalur cadangan.
- **Parser aturan (regex) tanpa LLM.** Ditolak dengan alasan yang sama seperti ADR-002: format struk Indonesia terlalu beragam.
- **Menyesuaikan harga otomatis supaya selalu cocok dengan TOTAL.** Ditolak karena menyembunyikan kesalahan baca; pengguna bisa menyimpan angka yang salah tanpa sadar.

## Konsekuensi
- Setiap scan mengirim **foto struk** ke Groq, sehingga memakai kuota gambar Groq dan selalu butuh internet. Bagian privasi di README sudah diperbarui.
- Ketergantungan pada model Preview. Jalur cadangan menjaga scan tetap jalan, tetapi akurasinya kembali bergantung pada OCR.
- Logika angka dan pengecekan TOTAL ada di kode Kotlin murni (`data/receipt/`) dan dicakup unit test (`app/src/test/.../data/receipt/`).
- Bila subtotal baris tidak habis dibagi jumlahnya (termasuk setelah penyesuaian dibagi), item disimpan sebagai 1 × subtotal baris supaya total tetap tepat (jumlah aslinya tetap tercatat di catatan transaksi).
