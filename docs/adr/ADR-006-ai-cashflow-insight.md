# ADR-006: AI Cashflow Insight

**Status:** Accepted  
**Date:** 2026-08-08  
**Author:** Satria (Kawan Kode)

---

## Context

Halaman Statistik sudah menampilkan chart pengeluaran per kategori dan ringkasan income/expense bulanan, tetapi user harus menganalisis sendiri pola keuangannya. Fitur ini memanfaatkan Groq API yang sudah terintegrasi di fitur sebelumnya (Receipt Scanner & Voice-to-Transaction) untuk mengotomatiskan analisis tersebut menjadi teks insight berbahasa natural.

---

## Decision

Menambahkan fitur **AI Cashflow Insight** yang dapat di-trigger on-demand oleh user melalui tombol di halaman Statistik. Groq LLM akan menerima ringkasan data keuangan bulan yang sedang dipilih dan menghasilkan 3-5 kalimat insight dalam Bahasa Indonesia.

---

## Detailed Decisions

### 1. Trigger: On-Demand via Button
- Insight hanya di-generate saat user menekan tombol "Buat Insight".
- Tidak auto-generate saat screen dibuka agar jumlah API call terkontrol.

### 2. Data Konteks yang Dikirim ke Groq
Sumber data: transaksi dari bulan yang sedang dipilih user (sesuai MonthFilterViewModel) dan bulan sebelumnya untuk perbandingan.
- Nama bulan (label)
- Total income bulan ini dan bulan lalu
- Total expense bulan ini dan bulan lalu
- Persentase perubahan pengeluaran dibanding bulan lalu
- Saldo (income - expense)
- Saving rate (persentase sisa dana dari income)
- Top 3 kategori pengeluaran terbesar beserta persentasenya
- Breakdown seluruh expense per kategori

### 3. Format Output Groq
- Response berupa plain text bebas (bukan JSON) tanpa responseFormat = json_object.
- Panjang: 5-8 kalimat komprehensif, dalam Bahasa Indonesia.
- Struktur instruksi ke AI mencakup: Ringkasan bulan ini, evaluasi kategori terbesar, perbandingan dengan bulan lalu, evaluasi saving rate, dan 2 saran konkret.

### 4. State Management
- Sealed class InsightUiState: Idle | Loading | Success(text) | Error(message)
- State dikelola di StatisticsViewModel via MutableStateFlow.
- Tidak ada caching - setiap klik tombol = fresh API call.

### 5. Tampilan Hasil
- Teks insight ditampilkan langsung di dalam card "AI Cashflow Insight" yang sudah ada.
- Tombol berubah label menjadi "Perbarui Insight" setelah state Success pertama.

### 6. Tombol Enabled State
- Tombol selalu enabled. Groq handle kasus data kosong sendiri.

### 7. Reuse AiRepository
- Method baru generateCashflowInsight(prompt: String): Result<String> ditambahkan ke AiRepository.
- Tidak menggunakan responseFormat agar Groq bisa menulis teks natural.
- Meningkatkan maxTokens menjadi 1500 agar output bisa lebih panjang dan menyeluruh.

### 8. Injeksi AiRepository ke StatisticsViewModel
- AiRepository ditambahkan sebagai parameter constructor ketiga StatisticsViewModel.
- FinansioViewModelFactory di-update untuk meng-inject aiRepository.

---

## Consequences

**Positif:**
- UX sederhana, sesuai target workshop pemula
- Tidak ada state yang perlu di-persist
- Memanfaatkan infrastruktur Groq yang sudah ada

**Negatif / Trade-off:**
- Setiap klik = 1 API call ke Groq (potensi rate limit)
- Tidak ada history insight - teks hilang saat user keluar dari screen

---

## Alternatives Considered

| Alternatif | Alasan Ditolak |
|---|---|
| Auto-generate saat buka halaman | Memboroskan API call, memperlambat screen load |
| Cache insight ke Room | Over-engineering untuk scope workshop |
| Tampilkan di dialog/modal | Kurang seamless untuk konten teks singkat |
| Bahasa Inggris | Target pengguna berbahasa Indonesia |
