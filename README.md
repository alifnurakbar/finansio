# Finansio — Aplikasi Pencatat Keuangan Pribadi Berbasis Android dengan Integrasi AI

Proyek skripsi **Alif Nur Akbar**.

Finansio adalah aplikasi Android untuk mencatat pemasukan dan pengeluaran pribadi. Aplikasi ini menampilkan statistik keuangan dan memakai kecerdasan buatan (AI) untuk mempercepat pencatatan, yaitu memindai struk belanja, mencatat transaksi lewat suara, dan memberi ringkasan (insight) arus kas.

---

## 📌 Asal Proyek & Kredit

Proyek ini **dikembangkan dari starter code** aplikasi *finance tracker* yang dibuat oleh **Satria (Kawan Kode)**. Starter code tersebut menjadi titik awal arsitektur dan fitur dasar aplikasi. Dokumen perancangan aslinya (PRD, design brief, dan ADR) tetap disimpan sebagai referensi di [`docs/referensi-awal/`](docs/referensi-awal/).

Pengembangan lanjutan, penyesuaian, dan penelitian untuk skripsi dilakukan oleh Alif Nur Akbar. Rinciannya ada di bagian [Kontribusi Saya](#-kontribusi-saya).

---

## 🚀 Fitur Aplikasi

### Fitur dasar
- **Catat transaksi**: tambah, ubah, dan hapus pemasukan (*income*) dan pengeluaran (*outcome*).
- **Manajemen kategori**: tambah, ubah, dan hapus kategori beserta warnanya.
- **Riwayat transaksi**: daftar transaksi dengan filter per bulan.
- **Statistik visual**: *pie chart* dan *line chart* pengeluaran memakai Compose Charts.
- **Penyimpanan offline**: semua data tersimpan di perangkat dengan Room Database.
- **Tema gelap**: antarmuka modern berbasis Jetpack Compose dan Material 3.

### Fitur AI
- **AI Receipt Scanner**: memotret struk dengan ML Kit Document Scanner, membaca teksnya dengan ML Kit Text Recognition, lalu mengubahnya menjadi satu atau beberapa transaksi dengan model bahasa (LLM) melalui Groq API.
- **Voice to Transaction**: mencatat transaksi lewat suara (Bahasa Indonesia) dengan Android Speech Recognizer, lalu kalimatnya diurai oleh LLM menjadi data transaksi.
- **AI Cashflow Insight**: ringkasan dan saran tentang pola keuangan bulanan, dibuat oleh LLM dari data statistik pengguna.

---

## 🛠️ Teknologi yang Digunakan

| Bagian | Teknologi |
|---|---|
| Bahasa | [Kotlin](https://kotlinlang.org/) |
| Antarmuka | [Jetpack Compose](https://developer.android.com/jetpack/compose), Material 3 |
| Arsitektur | MVVM (Model–View–ViewModel) + Repository |
| State & asinkron | StateFlow, Kotlin Coroutines |
| Database lokal | [Room](https://developer.android.com/training/data-storage/room) |
| Navigasi | Compose Navigation |
| Grafik | [Compose Charts](https://github.com/ehsannarmani/ComposeCharts) |
| Jaringan | Retrofit |
| Pemindaian & OCR | ML Kit Document Scanner, ML Kit Text Recognition v2 |
| Pengenalan suara | Android `RecognizerIntent` (id-ID) |
| Keamanan | AndroidX Biometric (sidik jari / kunci layar) |
| Laporan | Android `PdfDocument`, CSV |
| Notifikasi | `AlarmManager`, `NotificationCompat` |
| LLM | [Groq API](https://console.groq.com), model `openai/gpt-oss-120b` |

Target SDK: `minSdk 24`, `targetSdk 36`.

---

## 🗂️ Struktur Kode

```
app/src/main/java/com/finansio/app/
├── data/
│   ├── local/        # Room: database, DAO, entity
│   ├── model/        # Model hasil parsing struk
│   ├── remote/       # Retrofit + Groq API (service, DTO, konstanta)
│   └── repository/   # Transaction, Category, dan AI repository
├── ui/
│   ├── transaction/  # Beranda, form, dan riwayat transaksi
│   ├── category/     # Manajemen kategori
│   ├── statistics/   # Statistik + AI Cashflow Insight
│   ├── scanner/      # Alur scan struk dan hasilnya
│   ├── components/   # Komponen UI yang dipakai ulang
│   ├── navigation/   # Navigasi antarlayar
│   └── theme/        # Warna, tipografi, tema
└── util/             # Format mata uang dan tanggal
```

Keputusan desain teknis dicatat dalam bentuk ADR (*Architecture Decision Record*) di [`docs/adr/`](docs/adr/).

---

## ✍️ Kontribusi Saya

Bagian ini mencatat pengembangan yang saya (Alif Nur Akbar) lakukan di luar starter code awal.

- **Migrasi model LLM**: model `llama-3.3-70b-versatile` dihentikan oleh Groq (HTTP 404 `model_not_found`). Saya menggantinya dengan `openai/gpt-oss-120b` dan menaikkan `max_tokens` ke 4096 karena model ini adalah *reasoning model*. Lihat [ADR-002](docs/adr/ADR-002-groq-llm-receipt-parsing.md).
- **Perbaikan bug scan struk**: daftar kategori sebelumnya selalu kosong saat dikirim ke AI, sehingga semua item tersimpan tanpa kategori. Sekarang kategori dibaca langsung dari database, ada kategori cadangan, dan layar hasil scan menampilkan tanggal simpan.
- **Perbaikan grafik statistik**: grafik lingkaran kini diperbarui saat bulan/data berubah, ditambah ringkasan Income, Outcome, dan Saldo bulanan.
- **Pesan error AI yang ramah pengguna** (HTTP 401/404/429, koneksi, timeout) dalam Bahasa Indonesia.
- **Anggaran per kategori** dengan progress bar (hijau/kuning/merah). Database ditingkatkan dari versi 1 ke 2 lewat migrasi tanpa menghapus data.
- **Peringatan anggaran pintar (AI)**: saat pengeluaran kategori mencapai 80% dan 100%, aplikasi meminta saran singkat dari LLM, lalu menampilkannya sebagai banner di Home dan notifikasi HP.
- **Chat Asisten Keuangan AI**: tanya jawab bebas berdasarkan data keuangan pengguna. Hanya data agregat yang dikirim ke AI (tanpa nama dan catatan transaksi), dan riwayat chat tidak disimpan.
- **Ekspor laporan bulanan** ke PDF (ringkasan, grafik batang kategori, anggaran, tabel transaksi) dan CSV untuk Excel/Google Sheets.
- **Pengingat harian** dengan jam yang bisa diatur, dilewati otomatis jika hari itu sudah ada transaksi.
- **Kunci aplikasi** dengan sidik jari atau kunci layar HP (androidx.biometric).
- **Backup & restore** seluruh data ke file JSON (bisa disimpan ke Google Drive).
- **Halaman Profil & Pengaturan** serta navigasi bawah 5 menu dengan ikon Material Rounded yang seragam.

---

## 💻 Prasyarat

1. **Android Studio** versi terbaru ([unduh](https://developer.android.com/studio)).
2. **JDK 17** atau lebih baru (biasanya sudah bawaan Android Studio).
3. **Android SDK** sesuai `compileSdk` proyek.
4. **API key Groq**, bisa dibuat gratis di [console.groq.com](https://console.groq.com).

---

## ⚙️ Cara Menjalankan

### 1. Clone repository
```bash
git clone https://github.com/alifnurakbar/finansio.git
cd finansio
```

### 2. Buka di Android Studio
Pilih **Open**, arahkan ke folder `finansio`, lalu tunggu **Gradle Sync** selesai.
Kalau sync gagal, pastikan JDK 17+ dipakai di `Settings > Build, Execution, Deployment > Build Tools > Gradle`.

### 3. Isi API key Groq
Tambahkan baris berikut ke file `local.properties` di root proyek. File ini tidak ikut ter-commit, jadi API key tetap aman.
```properties
GROQ_API_KEY=isi_api_key_groq_anda
```

### 4. Build & Run
Jalankan di emulator atau HP Android (USB Debugging aktif) dengan tombol **Run 'app'** atau `Shift + F10`.

---

## 🐛 Troubleshooting

**1. Error "Using kotlin.sourceSets DSL to add Kotlin sources is not allowed..." saat build**
Disebabkan konflik versi KSP dengan pengaturan bawaan Kotlin Gradle Plugin. Proyek ini sudah menanganinya lewat `gradle.properties`:
```properties
android.disallowKotlinSourceSets=false
```

**2. Label grafik berwarna hitam dan tidak terbaca**
Sudah ditangani dengan menyesuaikan *label properties* grafik agar mengikuti tema gelap Material 3.

**3. Fitur AI gagal dengan HTTP 401**
API key Groq belum diisi atau salah. Periksa `GROQ_API_KEY` di `local.properties`, lalu *rebuild* proyek.

---

## 📄 Lisensi & Atribusi

Starter code © Satria (Kawan Kode). Pengembangan lanjutan © 2026 Alif Nur Akbar.
