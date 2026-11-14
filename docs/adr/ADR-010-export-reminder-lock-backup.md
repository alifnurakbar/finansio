# ADR-010: Ekspor Laporan, Pengingat Harian, Kunci Sidik Jari, dan Backup

**Status:** Accepted
**Tanggal:** 2026-11-14
**Author:** Alif Nur Akbar

## Konteks
Agar aplikasi lengkap dipakai sehari-hari, dibutuhkan fitur pendukung: membagikan laporan, kebiasaan mencatat, keamanan data, dan cadangan data. Semua fitur ini sebisa mungkin memakai API bawaan Android supaya tidak menambah banyak library.

## Keputusan

### 1. Ekspor laporan (`ReportExporter`)
- **PDF** dibuat dengan `android.graphics.pdf.PdfDocument` (tanpa library), ukuran A4, latar putih untuk dicetak: ringkasan, batang pengeluaran per kategori, anggaran, dan tabel transaksi dengan pindah halaman otomatis.
- **CSV** UTF-8 dengan BOM agar terbaca benar di Excel/Google Sheets.
- File ditulis ke `cache/exports` dan dibagikan lewat `FileProvider` + menu Bagikan Android. Hanya folder itu yang dibuka untuk aplikasi lain.

### 2. Pengingat harian (`DailyReminder`)
- `AlarmManager.setAndAllowWhileIdle` dijadwalkan ulang setiap hari. Tidak memerlukan izin *exact alarm* (boleh tertunda beberapa menit demi baterai).
- Bawaan aktif pukul 20.00, dan jam bisa diubah. Pengingat dilewati jika hari itu sudah ada transaksi.
- `BootReceiver` menjadwalkan ulang setelah HP dinyalakan ulang.

### 3. Kunci aplikasi (`AppLock`)
- `androidx.biometric` dengan `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`: sidik jari, atau PIN/pola layar HP sebagai cadangan. Tidak ada PIN khusus aplikasi yang perlu diingat.
- Mengaktifkan atau mematikan kunci memerlukan verifikasi terlebih dahulu.
- Aplikasi terkunci saat dibuka, atau setelah ditinggal **lebih dari 30 detik**. Batas ini dipilih supaya aplikasi tidak terkunci saat pengguna membuka kamera scan atau menu Bagikan sebentar.
- Jika kunci layar HP dihapus, kunci aplikasi otomatis dimatikan supaya pengguna tidak terkunci selamanya.

### 4. Backup & restore (`BackupManager`)
- Seluruh kategori, transaksi, anggaran, dan nama pengguna disimpan ke **satu file JSON** (Gson) melalui *Storage Access Framework*. Pengguna bebas memilih memori HP atau Google Drive tanpa login khusus di aplikasi.
- Restore membaca dan memeriksa file terlebih dahulu (penanda `app`, versi format), menampilkan ringkasan isi, lalu meminta konfirmasi.
- Restore berjalan dalam **satu transaksi database** (`runInTransaction`): jika gagal di tengah jalan, data lama tidak berubah.

## Konsekuensi
- Menambah dependensi `androidx.biometric:biometric:1.1.0`, dan `MainActivity` kini turunan `FragmentActivity`.
- Menambah izin `RECEIVE_BOOT_COMPLETED` dan `POST_NOTIFICATIONS`.
- Format backup punya `formatVersion`; perubahan struktur data berikutnya perlu menaikkan versi ini.
