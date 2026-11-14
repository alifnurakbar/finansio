# ADR-007: Anggaran per Kategori

**Status:** Accepted
**Tanggal:** 2026-11-14
**Author:** Alif Nur Akbar

## Konteks
Pengguna bisa melihat ke mana uangnya pergi, tetapi belum bisa menetapkan batas pengeluaran. Fitur anggaran juga menjadi dasar untuk peringatan anggaran pintar (ADR-008).

## Keputusan
- Tabel baru `budgets` dengan kolom `categoryId` (primary key) dan `monthlyLimit`. Satu kategori pengeluaran hanya punya satu anggaran.
- Anggaran **berlaku otomatis setiap bulan** sampai diubah, sehingga pengguna tidak perlu mengisinya ulang tiap bulan.
- Progres dihitung saat dibaca: anggaran digabung dengan total pengeluaran per `categoryId` pada bulan yang dipilih (`BudgetRepository.progressForMonth`).
- Warna status: hijau < 80%, kuning 80–99%, merah ≥ 100%. Warna selalu disertai ikon dan angka persen, tidak hanya warna.
- Database naik dari versi 1 ke 2 lewat `MIGRATION_1_2` yang hanya menjalankan `CREATE TABLE`, sehingga data transaksi dan kategori yang ada tetap utuh.

## Alternatif yang dipertimbangkan
| Alternatif | Alasan tidak dipilih |
|---|---|
| Anggaran berbeda per bulan | Pengguna harus mengisi ulang setiap bulan; terlalu merepotkan untuk kebutuhan pribadi |
| `fallbackToDestructiveMigration()` | Menghapus seluruh data pengguna saat versi database naik |
| Simpan progres di tabel | Bisa tidak sinkron dengan transaksi; menghitung saat dibaca lebih sederhana dan selalu akurat |

## Konsekuensi
- Perubahan skema berikutnya wajib menambahkan migrasi baru (2 → 3, dan seterusnya).
- Transaksi tanpa `categoryId` (data lama) tidak dihitung ke anggaran mana pun.
