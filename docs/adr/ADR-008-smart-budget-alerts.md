# ADR-008: Peringatan Anggaran Pintar (AI)

**Status:** Accepted
**Tanggal:** 2026-11-14
**Author:** Alif Nur Akbar

## Konteks
Progress bar anggaran hanya berguna jika pengguna membuka halaman Anggaran. Pengguna perlu diberi tahu secara proaktif saat pengeluaran mendekati batas, disertai saran yang relevan, bukan sekadar angka.

## Keputusan
- `BudgetAlertManager` (dijalankan dari `FinansioApplication`) memantau progres anggaran **bulan berjalan** lewat `Flow`, sehingga setiap transaksi baru langsung diperiksa.
- Ambang peringatan: **80%** (kuning) dan **100%** (merah). Setiap level hanya diperingatkan **sekali per kategori per bulan**. Status ini disimpan di `SharedPreferences`.
- Saat ambang terlewati, aplikasi meminta saran singkat (maksimal 2 kalimat) dari LLM melalui `AiRepository.generateBudgetAdvice`, berisi nama kategori, nominal terpakai, batas, persen, dan sisa hari.
- Jika AI gagal atau lebih dari 30 detik, dipakai **teks cadangan** tanpa AI, sehingga peringatan tetap muncul saat offline.
- Peringatan tampil sebagai **banner di Beranda** (bisa ditutup) dan **notifikasi HP** (kanal "Peringatan Anggaran").

## Konsekuensi
- Membutuhkan izin `POST_NOTIFICATIONS` di Android 13+. Jika ditolak, banner di Beranda tetap tampil.
- Peringatan hanya untuk bulan berjalan; bulan lain cukup ditampilkan sebagai progress bar.
