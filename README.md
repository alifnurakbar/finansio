# Finansio — Aplikasi Pencatat Keuangan Pribadi Berbasis Android dengan Integrasi AI

Proyek skripsi **Alif Nur Akbar**.

Finansio adalah aplikasi Android untuk mencatat pemasukan dan pengeluaran pribadi. Selain pencatatan dan statistik, Finansio memakai kecerdasan buatan (AI) berbasis *Large Language Model* (LLM) untuk:

- mempercepat pencatatan (scan struk belanja dan input suara),
- menganalisis keuangan (AI Cashflow Insight dan Chat Asisten Keuangan),
- membantu mengendalikan pengeluaran (peringatan anggaran pintar).

---

## 📌 Asal Proyek & Kredit

Proyek ini **dikembangkan dari starter code** aplikasi *finance tracker* yang dibuat oleh **Satria (Kawan Kode)**. Starter code tersebut menjadi titik awal arsitektur dan fitur dasar aplikasi. Dokumen perancangan aslinya (PRD, design brief, dan ADR-001 sampai ADR-006) tetap disimpan sebagai referensi di [`docs/referensi-awal/`](docs/referensi-awal/) dan [`docs/adr/`](docs/adr/).

Pengembangan lanjutan, penyesuaian, dan penelitian untuk skripsi dilakukan oleh Alif Nur Akbar. Rinciannya ada di bagian **Kontribusi Saya** di bawah.

---

## 📥 Unduh Aplikasi

APK siap pasang tersedia di halaman **[Releases](https://github.com/alifnurakbar/finansio/releases/latest)**.

1. Unduh file `Finansio-v1.0.1.apk` di HP Android (minimal Android 7.0).
2. Buka file tersebut. Jika diminta, izinkan **"Instal aplikasi tidak dikenal"** untuk browser atau file manager yang dipakai.
3. Tekan **Instal**, lalu buka Finansio.

Fitur AI membutuhkan koneksi internet.

---

## 🚀 Fitur Aplikasi

### 📒 Pencatatan
| Fitur | Keterangan |
|---|---|
| Catat transaksi | Tambah, ubah, dan hapus pemasukan (*income*) dan pengeluaran (*outcome*) |
| 📷 Scan struk (AI) | Foto struk dibaca langsung oleh model vision (dibantu teks ML Kit yang disusun per baris) → satu atau beberapa transaksi lengkap dengan kategori, plus pengecekan jumlah item terhadap TOTAL struk |
| 🎤 Input suara (AI) | Ucapkan transaksi dalam Bahasa Indonesia, misalnya *"beli kopi 15 ribu dan bensin 50 ribu"*, lalu LLM memecahnya menjadi transaksi |
| Riwayat transaksi | Daftar semua transaksi dengan pencarian |
| Kategori | Tambah, ubah, dan hapus kategori beserta warnanya |

### 📊 Analisis
| Fitur | Keterangan |
|---|---|
| Beranda | Saldo bulan berjalan, grafik income/expense, dan transaksi terbaru |
| Statistik bulanan | Kartu saldo dengan rasio pengeluaran dan perubahan dibanding bulan lalu, grafik **Tren 6 Bulan**, dan **donat pengeluaran per kategori** yang bisa diketuk |
| Statistik mingguan | Grafik arus kas harian Senin–Minggu |
| ✨ AI Cashflow Insight | Analisis pola keuangan bulanan dalam bentuk paragraf |
| 🤖 Chat Asisten Keuangan AI | Tanya jawab bebas, misalnya *"Bulan ini aku paling boros di mana?"*, dijawab berdasarkan data pengguna |

### 👛 Pengendalian
| Fitur | Keterangan |
|---|---|
| Anggaran per kategori | Batas pengeluaran bulanan yang berlaku otomatis setiap bulan, dengan progress bar hijau (< 80%), kuning (80–99%), dan merah (≥ 100%) |
| ⚠️ Peringatan anggaran pintar (AI) | Saat sebuah kategori mencapai 80% atau 100%, LLM membuat saran singkat yang tampil sebagai banner di Beranda dan notifikasi HP |
| ⏰ Pengingat harian | Notifikasi untuk mencatat transaksi (bawaan pukul 20.00, bisa diubah), otomatis dilewati jika hari itu sudah mencatat |

### ⚙️ Data & Keamanan
| Fitur | Keterangan |
|---|---|
| 📄 Ekspor laporan | Laporan bulanan **PDF** (ringkasan, grafik kategori, anggaran, tabel transaksi) dan **CSV** untuk Excel/Google Sheets, dibagikan lewat menu Bagikan Android |
| ☁️ Backup & restore | Seluruh data disimpan ke satu file JSON (bisa ke memori HP atau Google Drive) dan dipulihkan dengan konfirmasi |
| 🔒 Kunci sidik jari | Aplikasi dikunci dengan sidik jari atau PIN/pola layar HP |
| 👤 Profil & Pengaturan | Nama pengguna, kelola kategori, dan semua pengaturan di atas |

---

## 📱 Navigasi Aplikasi

```
┌────────────────────────────────────────────┐
│  🏠       📊         (➕)       👛       👤  │
│ Home  Statistik    Catat   Anggaran  Profil│
└────────────────────────────────────────────┘
```

| Menu | Isi |
|---|---|
| 🏠 **Home** | Sapaan, banner peringatan anggaran, saldo, grafik, transaksi terbaru, tombol **✨ Tanya AI** |
| 📊 **Statistik** | Mode Minggu / Bulan, grafik, AI Cashflow Insight |
| ➕ **Catat** | Pilihan: input manual, scan struk, atau suara |
| 👛 **Anggaran** | Daftar anggaran per kategori dan totalnya |
| 👤 **Profil** | Nama, Kelola Kategori, Ekspor Laporan, Pengingat Harian, Kunci Sidik Jari, Backup/Restore, Tentang Aplikasi |

---

## 🛠️ Teknologi yang Digunakan

| Bagian | Teknologi |
|---|---|
| Bahasa | [Kotlin](https://kotlinlang.org/) |
| Antarmuka | [Jetpack Compose](https://developer.android.com/jetpack/compose), Material 3, Material Icons (gaya Rounded) |
| Arsitektur | MVVM (Model–View–ViewModel) + Repository |
| State & asinkron | StateFlow, Kotlin Coroutines |
| Database lokal | [Room](https://developer.android.com/training/data-storage/room) (versi skema 2, dengan migrasi) |
| Navigasi | Compose Navigation |
| Grafik | Grafik kustom dengan Compose `Canvas` (Statistik), [Compose Charts](https://github.com/ehsannarmani/ComposeCharts) (Beranda) |
| Jaringan | Retrofit + OkHttp |
| Pemindaian & OCR | ML Kit Document Scanner, ML Kit Text Recognition v2 |
| Pengenalan suara | Android `RecognizerIntent` (id-ID) |
| LLM | [Groq API](https://console.groq.com), model `openai/gpt-oss-120b` (teks) dan `qwen/qwen3.8-27b` (foto struk, status Preview) |
| Keamanan | AndroidX Biometric (sidik jari / kunci layar) |
| Laporan | Android `PdfDocument`, CSV, `FileProvider` |
| Notifikasi & penjadwalan | `NotificationCompat`, `AlarmManager` |
| Backup | Storage Access Framework, Gson (JSON) |

Target SDK: `minSdk 24` (Android 7.0), `targetSdk 36`.

---

## 🏗️ Arsitektur

```
 UI (Jetpack Compose)          ViewModel                 Repository / Manager            Sumber data
 ─────────────────────         ─────────────────         ──────────────────────          ─────────────────
 HomeScreen            ──►     HomeViewModel      ──►    TransactionRepository   ──►     Room (SQLite)
 StatisticsScreen      ──►     StatisticsViewModel──►    BudgetRepository        ──►       ├ transactions
 BudgetScreen          ──►     BudgetViewModel    ──►    CategoryRepository      ──►       ├ categories
 ChatScreen            ──►     ChatViewModel      ──►    FinanceContextBuilder           └ budgets
 ScanResultScreen      ──►     ScanResultViewModel──►    AiRepository            ──►     Groq API (LLM)
 ProfileScreen         ──────────────────────────►       SettingsRepository      ──►     SharedPreferences
                                                         ReportExporter / BackupManager ► File (PDF/CSV/JSON)
                                                         BudgetAlertManager, DailyReminder ► Notifikasi
```

- **UI** hanya menampilkan state dan meneruskan aksi pengguna ke ViewModel.
- **ViewModel** menyiapkan data untuk layar dalam bentuk `StateFlow`.
- **Repository/Manager** berisi logika bisnis dan akses data. Semua akses database berjalan di `Dispatchers.IO`.
- Dependensi dibuat satu kali di `FinansioApplication` (*manual dependency injection*), lalu dibagikan lewat `FinansioViewModelFactory`.

---

## 🗂️ Struktur Kode

```
app/src/main/java/com/finansio/app/
├── FinansioApplication.kt     # Membuat semua repository/manager, menjalankan peringatan & pengingat
├── MainActivity.kt            # Menu bawah 5 item, tombol Catat, kunci sidik jari
├── data/
│   ├── local/                 # Room
│   │   ├── AppDatabase.kt     #   Database v2 + migrasi 1→2 + kategori bawaan
│   │   ├── dao/               #   Query transaksi, kategori, anggaran
│   │   └── entity/            #   Tabel: transactions, categories, budgets
│   ├── remote/                # Retrofit + Groq API (service, DTO, konstanta)
│   ├── repository/            # TransactionRepository, CategoryRepository, BudgetRepository, AiRepository
│   ├── model/                 # Model hasil parsing suara dari AI
│   ├── receipt/               # Pembacaan angka rupiah, baris OCR, dan pengecekan TOTAL struk
│   ├── budget/                # BudgetAlertManager: peringatan anggaran 80%/100% + saran AI
│   ├── chat/                  # FinanceContextBuilder: ringkasan data untuk Chat AI
│   ├── export/                # ReportExporter: laporan PDF & CSV
│   ├── backup/                # BackupManager: backup & restore JSON
│   ├── reminder/              # DailyReminder: pengingat harian (AlarmManager)
│   └── settings/              # SettingsRepository: nama, pengingat, kunci
├── security/                  # AppLock (BiometricPrompt) dan LockScreen
├── ui/
│   ├── transaction/           # Beranda (list), form, dan riwayat transaksi
│   ├── statistics/            # Statistik + ModernCharts (grafik kolom & donat kustom)
│   ├── budget/                # Halaman Anggaran
│   ├── chat/                  # Chat Asisten Keuangan AI
│   ├── profile/               # Profil & Pengaturan, ekspor, backup/restore
│   ├── scanner/               # Alur scan struk/suara dan hasilnya
│   ├── category/              # Kelola kategori
│   ├── components/            # Komponen UI yang dipakai ulang
│   ├── navigation/            # Rute dan NavHost
│   └── theme/                 # Warna, tipografi, tema gelap
└── util/                      # Format rupiah dan tanggal
```

Keputusan desain teknis dicatat sebagai ADR (*Architecture Decision Record*) di [`docs/adr/`](docs/adr/).

---

## 🗄️ Database

| Versi | Perubahan |
|---|---|
| 1 | Tabel `categories` dan `transactions` (starter code) |
| 2 | Tabel `budgets` (`categoryId`, `monthlyLimit`) ditambahkan lewat `MIGRATION_1_2`, tanpa menghapus data lama |

Pengaturan ringan (nama, jam pengingat, status kunci, status peringatan anggaran) disimpan di `SharedPreferences`.

---

## 🔐 Privasi & Keamanan Data

- Semua data keuangan disimpan **lokal di HP** (Room). Tidak ada server Finansio.
- Yang dikirim ke Groq API hanya data yang perlu diproses AI:
  - **Scan struk**: **foto struk** (diperkecil, JPEG), teks hasil OCR, dan daftar nama kategori. Setiap scan butuh internet dan memakai kuota gambar Groq.
  - **Input suara**: teks ucapan dan daftar nama kategori.
  - **Chat AI & Insight**: ringkasan angka (total per bulan dan per kategori), status anggaran, serta transaksi bulan ini berupa tanggal, jenis, kategori, dan nominal. **Nama pengguna dan catatan transaksi tidak dikirim.**
  - **Peringatan anggaran**: nama kategori, nominal terpakai, batas, dan sisa hari.
- Riwayat Chat AI **tidak disimpan** dan hilang saat halaman chat ditutup.
- Isi permintaan ke Groq (termasuk foto struk) hanya dicatat ke logcat pada build **debug**. APK release tidak mencatatnya.
- API key Groq disimpan di `local.properties` (tidak ikut ke Git). Saat build, nilainya ditulis otomatis ke `GroqSecrets.kt`, yang juga masuk `.gitignore`.
- Kunci aplikasi memakai `BiometricPrompt` (sidik jari atau kunci layar HP). Aplikasi tidak menyimpan data biometrik apa pun.

### Izin aplikasi
| Izin | Dipakai untuk |
|---|---|
| `INTERNET` | Memanggil Groq API |
| `RECORD_AUDIO` | Input transaksi lewat suara |
| `POST_NOTIFICATIONS` | Peringatan anggaran dan pengingat harian (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Menjadwalkan ulang pengingat setelah HP dinyalakan ulang |

---

## ✍️ Kontribusi Saya

Bagian ini mencatat pengembangan yang saya (Alif Nur Akbar) lakukan di luar starter code awal. Riwayat lengkapnya bisa dilihat di *commit history* GitHub.

**Perbaikan**
- **Migrasi model LLM**: `llama-3.3-70b-versatile` dihentikan oleh Groq (HTTP 404 `model_not_found`), lalu diganti dengan `openai/gpt-oss-120b`. `max_tokens` dinaikkan karena model ini adalah *reasoning model*. Lihat [ADR-002](docs/adr/ADR-002-groq-llm-receipt-parsing.md).
- **Bug scan struk**: daftar kategori sebelumnya selalu kosong saat dikirim ke AI, sehingga semua item tersimpan tanpa kategori. Sekarang kategori dibaca langsung dari database, ada kategori cadangan, dan layar hasil scan menampilkan tanggal simpan.
- **Pesan error AI yang ramah pengguna** (HTTP 401/404/429, koneksi, timeout) dalam Bahasa Indonesia, dan batas waktu respons dinaikkan menjadi 60 detik.
- **Keamanan API key**: key tidak lagi ditulis di kode sumber.
- **Akurasi scan struk**: harga item sebelumnya sering Rp 0 atau dobel karena ML Kit membaca struk per kolom. Sekarang AI membaca foto struk langsung, teks OCR disusun ulang per baris, jawaban AI diperiksa ulang, dan layar hasil scan menunjukkan apakah jumlah item cocok dengan TOTAL struk. Lihat [ADR-012](docs/adr/ADR-012-akurasi-scan-struk.md).

**Fitur baru**
- **Anggaran per kategori** dengan migrasi database v1 → v2. Lihat [ADR-007](docs/adr/ADR-007-budget-per-category.md).
- **Peringatan anggaran pintar (AI)** di banner Beranda dan notifikasi. Lihat [ADR-008](docs/adr/ADR-008-smart-budget-alerts.md).
- **Chat Asisten Keuangan AI** dengan prinsip privasi data agregat. Lihat [ADR-009](docs/adr/ADR-009-ai-finance-chat.md).
- **Ekspor laporan PDF/CSV, pengingat harian, kunci sidik jari, serta backup & restore**. Lihat [ADR-010](docs/adr/ADR-010-export-reminder-lock-backup.md).
- **Desain ulang Statistik** dengan grafik kustom (kolom tren 6 bulan, donat interaktif, kartu saldo). Lihat [ADR-011](docs/adr/ADR-011-custom-statistics-charts.md).
- **Halaman Profil & Pengaturan**, navigasi bawah 5 menu, sapaan nama di Beranda, dan ikon Material Rounded yang seragam.

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

### 5. (Opsional) Membuat APK release
Tambahkan ke `local.properties`:
```properties
GROQ_API_KEY_RELEASE=key_groq_khusus_release
RELEASE_STORE_FILE=lokasi/keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```
Lalu jalankan `./gradlew assembleRelease`. APK release otomatis memakai `GROQ_API_KEY_RELEASE`, bukan key pengembangan, dan build akan ditolak jika key release belum diisi.

Saat pertama kali dibuka, isi nama Anda di **Profil**, lalu atur anggaran di menu **Anggaran**.

---

## 🐛 Troubleshooting

**1. Error "Using kotlin.sourceSets DSL to add Kotlin sources is not allowed..." saat build**
Disebabkan konflik versi KSP dengan pengaturan bawaan Kotlin Gradle Plugin. Proyek ini sudah menanganinya lewat `gradle.properties`:
```properties
android.disallowKotlinSourceSets=false
```

**2. Fitur AI gagal dengan pesan "API key Groq salah" (HTTP 401)**
API key belum diisi atau salah. Periksa `GROQ_API_KEY` di `local.properties`, lalu *rebuild* proyek.

**3. Fitur AI gagal dengan pesan "Batas pemakaian AI tercapai" (HTTP 429)**
Paket gratis Groq membatasi jumlah token per menit. Tunggu sekitar 1 menit lalu coba lagi.

**4. `GroqSecrets` atau `BuildConfig` bertanda merah di VS Code**
File tersebut dibuat otomatis oleh Gradle. Jalankan *build* sekali, lalu muat ulang editor. Di Android Studio cukup **Sync Project with Gradle Files**.

**5. Notifikasi tidak muncul**
Pastikan izin notifikasi Finansio aktif di Pengaturan HP. Pengingat harian dijadwalkan dengan alarm hemat baterai, jadi bisa tertunda beberapa menit.

**6. Kunci sidik jari tidak bisa diaktifkan**
Atur sidik jari atau kunci layar (PIN/pola) di Pengaturan HP terlebih dahulu.

---

## 📄 Lisensi & Atribusi

Starter code © Satria (Kawan Kode). Pengembangan lanjutan © 2026 Alif Nur Akbar.
