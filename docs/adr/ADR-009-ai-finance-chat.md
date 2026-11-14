# ADR-009: Chat Asisten Keuangan AI

**Status:** Accepted
**Tanggal:** 2026-11-14
**Author:** Alif Nur Akbar

## Konteks
AI Cashflow Insight (ADR-006) hanya menghasilkan satu paragraf dengan format tetap. Pengguna ingin bertanya bebas tentang keuangannya, misalnya kategori paling boros, perbandingan antarbulan, atau apakah anggaran aman.

## Keputusan
- Halaman chat baru (`ChatScreen`) dibuka dari tombol **✨ Tanya AI** di Beranda, dengan 4 contoh pertanyaan siap ketuk.
- Setiap pertanyaan, `FinanceContextBuilder` menyusun ulang ringkasan data terbaru:
  - total pemasukan/pengeluaran dan rincian per kategori untuk **3 bulan terakhir**,
  - status anggaran bulan ini dan sisa hari,
  - transaksi bulan ini (maksimal 60) berupa **tanggal, jenis, kategori, dan nominal**.
- **Privasi:** nama pengguna dan catatan (*note*) transaksi **tidak pernah dikirim** ke AI. Keterangan ini juga ditampilkan di halaman chat.
- Prompt sistem membatasi jawaban: hanya berdasarkan data, Bahasa Indonesia santai, ringkas, tidak mengarang angka, dan menolak topik di luar keuangan.
- Riwayat chat hanya disimpan di `ChatViewModel` yang terikat ke halaman, sehingga **hilang saat halaman ditutup**. Hanya 10 pesan terakhir yang dikirim sebagai konteks.
- Jawaban dirender dengan format sederhana (teks **tebal** dan daftar •).

## Alternatif yang dipertimbangkan
| Alternatif | Alasan tidak dipilih |
|---|---|
| Kirim seluruh riwayat transaksi | Melanggar prinsip data minimal dan cepat melewati batas token Groq (±8.000 token/menit pada paket gratis) |
| Simpan riwayat chat di database | Menambah data sensitif yang perlu dilindungi; tidak dibutuhkan untuk kasus pemakaian ini |

## Konsekuensi
- Memerlukan koneksi internet.
- Jika batas token Groq tercapai, pengguna mendapat pesan HTTP 429 yang ramah dan tombol "Coba lagi".
