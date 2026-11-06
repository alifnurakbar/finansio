# PRD: Finance Tracker App — Workshop 2 Hari
**Pembicara:** Satria (Kawan Kode)
**Format:** 2 hari, 1 sesi/hari, 90 menit/sesi
**Audiens:** Pemula — baru mengenal Jetpack Compose & Room
**Scope:** Starter code (70%) + fitur workshop (30%)

> Catatan: dokumen ini draft — bagian yang masih asumsi/perlu konfirmasi ditandai jelas di tiap section. Estimasi waktu di rencana sesi masih perkiraan kasar.

---

## 1. Latar Belakang & Tujuan

Workshop ini mengajak peserta pemula melanjutkan sebuah base app pencatatan keuangan (starter code, 70% jadi) dengan menambahkan fitur AI (OCR struk, voice input, AI insight) — topik yang jarang dibahas di workshop Android level dasar.

**Tujuan Workshop:**
- Peserta memahami cara kerja & alur integrasi ML Kit untuk OCR struk belanja (walau tidak coding langsung)
- Peserta memahami cara kerja & alur integrasi Speech-to-Text untuk input suara
- Peserta memahami cara memanggil LLM API (Groq) dan pola prompt untuk parsing respons ke kebutuhan aplikasi nyata
- Peserta memahami pola integrasi "on-device ML + cloud AI" dalam arsitektur MVVM, sehingga bisa mereplikasi sendiri dari kode yang dibagikan setelah sesi

*(Asumsi awal berdasarkan fitur yang kamu sebutkan — tolong dikoreksi kalau ada tujuan lain. Kata kerja "memahami" dipakai karena format sesi satu arah, bukan hands-on.)*

---

## 2. Target Audiens & Format

- Level: **Pemula**, sudah kenal Kotlin dasar, baru mulai/baru kenal Jetpack Compose dan Room
- Asumsi: belum pernah integrasi ML Kit, Speech Recognizer, atau REST API ke LLM
- **Format:** mostly satu arah (presentasi/demo dari pembicara) — **peserta tidak live coding**. Implikasinya: fokus materi ke penjelasan konsep, alur, dan penjelasan kode (bukan peserta mengetik bareng), sehingga starter code + kode fitur AI perlu sudah siap penuh untuk didemokan

---

## 3. Scope Keseluruhan

| Bagian | Isi | Status |
|---|---|---|
| Base App (70%) | CRUD Income & Expense, CRUD Kategori, Statistik/Analytics | **Perlu dirancang & dibangun** sebagai starter code sebelum hari-H |
| Fitur Workshop (30%) | AI Receipt Scanner, Voice Recognition Input, AI Cashflow Insight | Dibangun bersama peserta saat workshop |

---

## 4. Rancangan Teknis — Base App (70%)

### 4.1 Struktur Project

```
app/
 └── src/main/java/com/kawankode/financetracker/
      ├── FinanceTrackerApp.kt          // Application class (Hilt entry point, jika pakai Hilt)
      ├── MainActivity.kt
      │
      ├── data/
      │    ├── local/
      │    │    ├── entity/
      │    │    │    ├── TransactionEntity.kt
      │    │    │    └── CategoryEntity.kt
      │    │    ├── dao/
      │    │    │    ├── TransactionDao.kt
      │    │    │    └── CategoryDao.kt
      │    │    └── AppDatabase.kt
      │    ├── repository/
      │    │    ├── TransactionRepository.kt
      │    │    └── CategoryRepository.kt
      │    └── model/                    // domain model (kalau dipisah dari entity)
      │         ├── Transaction.kt
      │         └── Category.kt
      │
      ├── ui/
      │    ├── transaction/
      │    │    ├── list/                // Home / daftar transaksi
      │    │    ├── form/                // Tambah/edit transaksi
      │    │    └── TransactionViewModel.kt
      │    ├── category/
      │    │    ├── list/
      │    │    ├── form/
      │    │    └── CategoryViewModel.kt
      │    ├── statistics/
      │    │    ├── StatisticsScreen.kt
      │    │    └── StatisticsViewModel.kt
      │    ├── navigation/
      │    │    └── AppNavGraph.kt
      │    └── theme/                    // Compose theme (Color, Type, Theme)
      │
      └── util/
           ├── DateUtils.kt
           └── CurrencyFormatter.kt
```

*(Ini rancangan awal — bisa disesuaikan lagi kalau kamu punya konvensi struktur project sendiri yang biasa dipakai.)*

### 4.2 Dependencies (Base App)

| Kategori | Library | Catatan |
|---|---|---|
| UI | Jetpack Compose (BOM terbaru) | Cek versi stabil terbaru sebelum dipakai — jangan ambil dari ingatanku, cek langsung di dokumentasi resmi Android |
| Database | Room (runtime, ktx, compiler/KSP) | |
| Navigation | Navigation Compose | |
| DI | Manual DI (ViewModel Factory sederhana) | Diputuskan: skip Hilt, biar materi tidak terlalu berat untuk pemula |
| Networking | Retrofit + converter (Moshi/Gson) | Diputuskan: Retrofit |
| Chart | ComposeCharts | Diputuskan |
| Coroutines | kotlinx-coroutines-android | Untuk operasi Room yang async |
| Async stream | Kotlin Flow | Room mendukung `Flow<List<T>>` untuk observe data real-time |

> Aku belum mencantumkan versi spesifik di sini karena versi library gampang berubah — sebaiknya dicek langsung ke halaman resmi masing-masing sebelum dipakai di starter code.

### 4.3 Data Model (Room)

**CategoryEntity**
| Field | Tipe | Keterangan |
|---|---|---|
| id | Int (PK, autoGenerate) | |
| name | String | |
| type | Enum/String (`INCOME` / `EXPENSE`) | Kategori terpisah untuk income & expense, atau satu tabel dengan flag tipe |
| iconRes / color | Int/String (opsional) | Untuk tampilan visual di list & chart |

**TransactionEntity**
| Field | Tipe | Keterangan |
|---|---|---|
| id | Int (PK, autoGenerate) | |
| amount | Double/Long | Disarankan simpan dalam bentuk integer (misal: rupiah tanpa desimal) untuk hindari isu floating point |
| type | Enum/String (`INCOME` / `EXPENSE`) | |
| categoryId | Int? | **Diputuskan:** tetap disimpan untuk referensi ke `CategoryEntity` (misal kalau nanti perlu fitur edit yang link balik ke kategori aktif) |
| categoryName | String | **Diputuskan:** disimpan juga sebagai snapshot nama kategori saat transaksi dibuat, supaya delete/rename kategori tidak mengubah tampilan transaksi lama |
| note | String? | Catatan opsional |
| date | Long (timestamp) | |

**Catatan relasi:** `TransactionEntity` menyimpan **keduanya** — `categoryId` (referensi ke `CategoryEntity`, tanpa FK constraint/cascade yang ketat) dan `categoryName` (snapshot string saat transaksi dibuat). `categoryName` yang jadi sumber utama untuk ditampilkan di UI & agregasi statistik, supaya tampilan transaksi lama tidak berubah kalau kategori di-rename atau dihapus. `categoryId` tetap disimpan untuk keperluan lain (misal kalau nanti mau fitur "edit transaksi, pilih ulang dari kategori yang masih aktif") — tapi kalau kategorinya sudah dihapus, `categoryId` ini jadi tidak valid lagi (dangling reference) dan aplikasi cukup mengandalkan `categoryName` saja di kondisi itu.

*(Ini rancangan dasar — field bisa ditambah sesuai kebutuhan, misal `paymentMethod` kalau relevan.)*

### 4.4 Fitur Detail — Base App

**CRUD Income & Expense**
- List transaksi (bisa difilter: semua/income/expense, per periode)
- Form tambah/edit: input nominal, pilih tipe, pilih kategori (dropdown dari CategoryEntity), catatan, tanggal
- Delete dengan konfirmasi (swipe-to-delete atau tombol + dialog)
- Observasi data pakai `Flow` dari Room agar UI update otomatis

**CRUD Kategori**
- List kategori (terpisah income/expense atau digabung dengan filter)
- Tambah/edit/hapus kategori
- **Diputuskan:** delete kategori tidak memengaruhi transaksi yang sudah ada — `TransactionEntity` tetap punya `categoryId` (jadi dangling/tidak valid setelah kategori dihapus) tapi `categoryName` (snapshot) tetap tersimpan dan dipakai untuk tampilan. Kategori boleh dihapus kapan saja tanpa perlu cek transaksi terkait, tidak perlu FK constraint dengan cascade/restrict.

**Statistik/Analytics — chart/grafik**
- Ringkasan total income/expense/saldo per periode (misal bulan berjalan)
- Chart per kategori (bar chart) — untuk expense breakdown per kategori dalam periode tertentu, pakai **ComposeCharts**
- Query agregasi: `SUM(amount) GROUP BY categoryId` untuk periode tertentu (bisa pakai Room `@Query` custom)

---

## 5. Rancangan Fitur — Workshop (30%)

### 5.1 AI Receipt Scanner (Hari 1)
**User story:** Sebagai pengguna, saya ingin memfoto struk belanja agar aplikasi otomatis mengisi data transaksi.

**Alur:**
1. User ambil foto struk (kamera/galeri)
2. ML Kit Text Recognition ekstrak teks mentah dari gambar
3. Teks mentah dikirim ke Groq API dengan prompt terstruktur (minta output JSON: nominal, tanggal, item/merchant)
4. Hasil parsing ditampilkan di form input (reuse form dari base app), user koreksi sebelum simpan
5. Simpan ke Room seperti transaksi biasa

**Kebutuhan fungsional:**
- Izin kamera & penyimpanan
- Loading state saat OCR + API call berjalan
- Error handling: teks tidak terbaca, API gagal, hasil parsing tidak lengkap

**Di luar scope (asumsi):** multi-item parsing per struk, deteksi kategori otomatis dari nama merchant

### 5.2 Voice Recognition Input (Hari 2)
**User story:** Sebagai pengguna, saya ingin mengucapkan transaksi ("beli kopi lima belas ribu") agar otomatis tercatat.

**Alur (pola sama dengan Receipt Scanner):**
1. User tekan tombol mic, ucapkan transaksi
2. `SpeechRecognizer` Android konversi suara → teks
3. Teks dikirim ke Groq API dengan prompt terstruktur → JSON (nominal, deskripsi, tipe income/expense)
4. Hasil ditampilkan di form untuk konfirmasi
5. Simpan ke Room

**Kebutuhan fungsional:**
- Izin mikrofon
- Cek dukungan locale `id-ID` di `SpeechRecognizer` — sebaiknya ditest di device peserta sebelum hari-H, karena dukungan bisa beda-beda tergantung vendor/OS
- Error handling: suara tidak jelas, hasil ambigu

### 5.3 AI Cashflow Insight (Hari 2)
**User story:** Sebagai pengguna, saya ingin mendapat insight singkat tentang kebiasaan keuangan saya tanpa harus menganalisis sendiri.

**Alur:**
1. Ambil data transaksi user dari Room (periode tertentu)
2. Susun jadi prompt terstruktur (summary per kategori, total income/expense)
3. Kirim ke Groq API, minta insight bahasa natural (2-3 kalimat)
4. Tampilkan di halaman statistik, sebagai teks hasil LLM saja (tanpa elemen visual tambahan)

**Diputuskan:** insight di-generate **on-demand**, lewat tombol terpisah (bukan otomatis saat buka halaman statistik) — supaya jumlah API call terkontrol dan user yang punya kendali kapan insight di-generate.

---

## 6. Arsitektur Teknis — Fitur AI

- **Networking ke Groq API:** **Diputuskan — Retrofit** (+ converter Moshi/Gson untuk parsing JSON)
- **Layer baru:** `AiRepository`/`RemoteDataSource` untuk request-response ke Groq, plus DTO untuk request/response
- **Prompt handling:** sebaiknya prompt template disiapkan di starter code (constant/helper function) agar peserta tinggal isi variabel, bukan menulis prompt dari nol saat workshop
- **API key:** **Diputuskan** — peserta bikin sendiri (bukan dibagikan panitia). Perlu instruksi pra-workshop supaya peserta sudah punya API key sebelum hari-H, biar tidak makan waktu sesi
- **Starter code untuk networking layer AI:** **Diputuskan** — semua bagian networking layer (Retrofit setup, service interface, DTO, repository) ditulis lengkap di starter code, tidak ada bagian yang sengaja disembunyikan/dihapus. Kamu yang akan menyeleksi sendiri nanti bagian mana yang dipakai untuk materi workshop

---

## 7. Rencana Sesi (Draft Kasar — 90 menit/sesi, format presentasi/demo satu arah)

> Karena peserta tidak live coding, alokasi waktu di sini lebih ke "penjelasan + demo" daripada "peserta menulis kode". Estimasi waktu masih perkiraan kasar.

### Hari 1 — AI Receipt Scanner
| Waktu (estimasi) | Aktivitas |
|---|---|
| 0–15 menit | Intro konsep: on-device ML vs cloud AI, overview arsitektur fitur |
| 15–40 menit | Penjelasan & demo alur ML Kit — dari ambil foto sampai ekstrak teks |
| 40–65 menit | Penjelasan & demo koneksi ke Groq API — prompt engineering untuk parsing struk, tunjukkan request/response nyata |
| 65–85 menit | Penjelasan & demo alur hasil parsing → form input → simpan ke Room |
| 85–90 menit | Q&A / wrap up |

### Hari 2 — Voice Recognition + AI Insight
| Waktu (estimasi) | Aktivitas |
|---|---|
| 0–10 menit | Recap hari 1, intro voice recognition |
| 10–35 menit | Penjelasan & demo `SpeechRecognizer` — capture suara → teks |
| 35–50 menit | Penjelasan & demo kirim teks ke Groq API (reuse pattern dari hari 1) |
| 50–75 menit | Penjelasan & demo AI Cashflow Insight: agregasi data → prompt → tampilkan insight |
| 75–90 menit | Q&A / wrap up / closing |

> Karena tidak ada live coding, dua fitur (voice + insight) dalam satu sesi 90 menit lebih realistis dibanding sebelumnya — tapi tetap perlu dicek apakah waktu penjelasan + demo pas, terutama kalau mau ada jeda tanya-jawab di tengah.

---

## 8. Asumsi & Risiko

| Asumsi/Risiko | Catatan |
|---|---|
| Peserta harus punya API key Groq sendiri sebelum hari-H | Perlu instruksi pra-workshop yang jelas (cara daftar, dsb) supaya tidak makan waktu sesi |
| Koneksi internet venue stabil untuk demo + API call live | Sebaiknya ada fallback (rekaman demo/screenshot hasil) kalau internet venue bermasalah saat presentasi |
| Networking layer AI sudah lengkap di starter code (Retrofit dll) — tidak ada bagian yang sengaja disembunyikan | Kamu yang akan menyeleksi sendiri nanti bagian mana yang relevan ditampilkan/dijelaskan saat sesi |
| Dukungan bahasa Indonesia di SpeechRecognizer konsisten di semua device peserta | Perlu ditest di beberapa device sebelum hari-H |
| Library chart untuk Compose | Belum dipilih spesifik — ekosistemnya cepat berubah, cek pilihan terbaru & paling simple mendekati hari-H |

---

---

## 9. Instruksi Pra-Workshop: Membuat API Key Groq

> Berdasarkan info dari beberapa sumber publik (dokumentasi Groq & panduan pihak ketiga) — sebaiknya dicek ulang ke [console.groq.com](https://console.groq.com) mendekati hari-H, karena alur signup/UI provider bisa berubah sewaktu-waktu.

Kirim instruksi ini ke peserta sebelum hari-H, supaya tidak makan waktu sesi:

1. Buka **console.groq.com**, buat akun (bisa pakai email, Google, atau GitHub) — gratis, tidak perlu kartu kredit
2. Verifikasi email kalau diminta
3. Setelah masuk ke dashboard, cari menu **"API Keys"**
4. Klik **"Create API Key"**, beri nama (misal: `finance-tracker-workshop`)
5. **Copy API key yang muncul saat itu juga** — kunci hanya ditampilkan sekali, kalau lupa copy harus generate ulang
6. Simpan key di tempat aman (jangan di-commit ke Git, jangan share ke orang lain)

**Catatan untuk kamu sebagai penyelenggara:**
- Free tier Groq (berdasarkan info yang aku temukan) ada rate limit (jumlah request per menit & token harian) — perlu diinfokan ke peserta supaya tidak kaget kalau kena limit saat demo/testing sendiri
- Rate limit dan detail free tier ini bisa berubah — cek langsung ke halaman pricing/limits Groq sebelum hari-H untuk angka pastinya, jangan sampaikan ke peserta angka yang sudah tidak akurat

## 10. Yang Masih Perlu Diputuskan

- [ ] Fallback kalau internet venue bermasalah saat sesi (mengingat semua demo bergantung koneksi live ke Groq API)
- [ ] Detail slide/materi presentasi tiap 15-25 menit blok waktu di rencana sesi (bagian 7) — PRD ini baru garis besar
- [ ] Apakah perlu handout/repo kode terpisah yang dibagikan setelah sesi, supaya peserta bisa coba sendiri di rumah
