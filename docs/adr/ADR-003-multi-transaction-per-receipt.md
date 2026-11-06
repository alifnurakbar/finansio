# ADR 003: Multi-Transaksi per Struk

## Status
Accepted

## Konteks
Ketika memindai sebuah struk belanja, struk tersebut bisa berisi banyak item yang dibeli. Model database aplikasi saat ini (TransactionEntity) didesain untuk satu transaksi = satu catatan pengeluaran/pemasukan.

## Keputusan
Daripada menjumlahkan seluruh struk menjadi satu `TransactionEntity`, kita memutuskan untuk mengubah **setiap item pada struk menjadi satu `TransactionEntity` yang terpisah**.

## Implikasi
- Membutuhkan layer UI tambahan (`ScanResultScreen`) untuk me-review dan mengedit daftar item hasil scan sebelum disimpan ke database (Batch Insert).
- Memungkinkan kategorisasi per-item (misal: Sabun -> Kebutuhan Harian, Snack -> Makanan) yang jauh lebih detail dibanding satu kategori untuk satu struk penuh.
