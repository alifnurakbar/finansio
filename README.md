# Finansio - Financial Tracker (Workshop Starter Code)

Selamat datang di *repository* **Finansio**, aplikasi pelacak keuangan berbasis Android yang akan kita gunakan selama sesi *workshop*!

Aplikasi ini dibangun menggunakan *modern Android development stack* dan dirancang untuk membantu pengguna mencatat pemasukan (Income) dan pengeluaran (Outcome), serta melihat visualisasi statistik pengeluaran mereka.

## 🚀 Fitur yang Sudah Tersedia
Aplikasi ini sudah dilengkapi dengan kerangka dasar (*starter code*) yang sudah matang:
- **Catat Transaksi**: Menambah, mengedit, dan menghapus catatan pemasukan/pengeluaran.
- **Kategori Manajemen**: Menambah, mengedit, dan menghapus kategori khusus (beserta warna).
- **Statistik Visual**: Menampilkan grafik visual (*Pie Chart* dan *Line Chart*) menggunakan *Compose Charts*.
- **Database Lokal**: Menggunakan *Room Database* untuk penyimpanan data *offline*.
- **Dark Theme**: UI modern berbasis Compose dengan tema gelap yang elegan.

Di sesi *workshop*, kita akan membedah arsitekturnya dan di Hari ke-2 kita akan berfokus pada integrasi fitur pintar (**AI Cashflow Insight**).

---

## 🛠️ Tech Stack
- **Bahasa**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Arsitektur**: MVVM (Model-View-ViewModel)
- **State Management**: StateFlow / Coroutines
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Navigasi**: Compose Navigation
- **Grafik/Chart**: [Compose Charts](https://github.com/ehsannarmani/ComposeCharts)

---

## 💻 Prasyarat (Prerequisites)
Sebelum memulai, pastikan perangkat komputermu sudah terpasang:
1. **Android Studio**: Sangat disarankan menggunakan versi terbaru (minimal *Koala* atau *Ladybug*). Unduh di [sini](https://developer.android.com/studio).
2. **Java Development Kit (JDK)**: Minimal JDK 17 (biasanya sudah bawaan *bundled* dari Android Studio terbaru).
3. **Android SDK**: API Level 34 atau 35.

---

## ⚙️ Cara Setup & Menjalankan Project (Untuk Peserta)

Silakan ikuti langkah-langkah berikut untuk menjalankan aplikasi di komputermu:

### 1. Fork Repository (Wajib)
Agar kamu bisa menyimpan hasil kodinganmu sendiri nanti, **Fork** *repository* ini terlebih dahulu ke akun GitHub pribadimu menggunakan tombol *Fork* di kanan atas halaman GitHub ini.

### 2. Clone Repository
Buka Terminal / *Command Prompt* / *Git Bash*, lalu *clone repository* yang sudah kamu *fork*:
```bash
git clone https://github.com/USERNAME_KAMU/finansio.git
cd finansio
```

### 3. Buka di Android Studio
1. Buka aplikasi **Android Studio**.
2. Pilih menu **Open**.
3. Cari dan pilih folder `finansio` hasil *clone* tadi.
4. Tunggu beberapa saat sampai **Gradle Sync** selesai secara otomatis. 
   *(Jika Gradle Sync gagal, pastikan koneksi internet stabil dan cek konfigurasi JDK di `Settings > Build, Execution, Deployment > Build Tools > Gradle` pastikan menggunakan JDK 17+).*

### 4. Isi API Key Groq
Tambahkan baris berikut ke file `local.properties` di root project (file ini tidak ikut ter-commit). Key bisa dibuat di [console.groq.com](https://console.groq.com):
```properties
GROQ_API_KEY=key_groq_kamu
```

### 5. Build & Run
1. Buat **Android Emulator (AVD)** baru melalui *Device Manager* di Android Studio, atau sambungkan HP Android aslimu menggunakan kabel USB (*pastikan USB Debugging aktif*).
2. Tekan tombol **Play (Run 'app')** 🟢 di menu atas Android Studio atau gunakan *shortcut* `Shift + F10`.
3. Aplikasi **Finansio** akan ter- *install* dan terbuka di emulator atau HP kamu!

---

## 🐛 Troubleshooting Umum

**1. Error "Using kotlin.sourceSets DSL to add Kotlin sources is not allowed..." saat build**
Ini biasanya muncul karena konflik versi KSP dan pengaturan default Kotlin Gradle Plugin. Jangan khawatir, pada *starter code* ini sudah diselesaikan dengan menambahkan baris berikut di `gradle.properties`:
```properties
android.disallowKotlinSourceSets=false
```

**2. Teks label pada Chart (Grafik) berwarna hitam tidak terbaca**
Ini juga sudah diselesaikan pada *starter code* dengan menyesuaikan *Label Properties* agar mengikuti konfigurasi *Dark Theme* pada Material3.

---

## 🎓 Selamat Belajar!
Jangan ragu untuk mengeksplorasi kode-kode yang ada di dalam *package* `com.finansio.app.ui` dan `com.finansio.app.data` sebelum sesi *workshop* dimulai. *Happy coding!*
