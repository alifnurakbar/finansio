# ADR 005: Voice-to-Transaction menggunakan System SpeechRecognizer Intent + Groq LLM

## Status
Accepted

## Konteks
Aplikasi membutuhkan fitur voice input agar user bisa mencatat transaksi hanya dengan mengucapkan kalimat seperti "beli kopi lima belas ribu". Fitur ini perlu mengkonversi suara menjadi teks, lalu memparse teks tersebut menjadi structured data (JSON) yang bisa disimpan sebagai `TransactionEntity`.

## Keputusan

### 1. Speech-to-Text: System Intent (`ACTION_RECOGNIZE_SPEECH`)
Kami memilih menggunakan **System Intent** (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) yang meluncurkan UI bawaan Google, bukan custom `SpeechRecognizer` API.

**Alasan:**
- Hanya membutuhkan ~20 baris kode — sangat cocok untuk konteks workshop pemula.
- Tidak perlu mengelola lifecycle `SpeechRecognizer` secara manual (create, destroy, error recovery).
- Google menangani UI feedback (waveform, progress) secara otomatis.
- Mendukung locale `id-ID` (Bahasa Indonesia) dengan konfigurasi `EXTRA_LANGUAGE`.
- Tradeoff: tidak bisa custom UI waveform seperti di DESIGN.md section 4.6, tapi simplicity lebih penting untuk workshop.

### 2. Text Parsing: Groq LLM (reuse infrastruktur existing)
Setelah mendapat teks dari SpeechRecognizer, teks dikirim ke Groq API menggunakan method baru `parseVoiceText()` di `AiRepository` yang sudah ada. Prompt dirancang untuk:
- Mengextract: nama item, harga, quantity, tipe (INCOME/EXPENSE), dan suggested category.
- Mendukung multi-item per ucapan (misal "beli kopi 15 ribu dan makan siang 25 ribu" → 2 item).
- Default ke EXPENSE, tapi mendeteksi INCOME dari konteks (misal "gaji masuk 5 juta").

### 3. Review UI: Reuse `ScanResultScreen`
Hasil parsing ditampilkan menggunakan `ScanResultScreen` yang sudah ada (dari fitur receipt scanner). Alasan:
- Flow identik: parse → review items → edit inline → save all.
- Menghindari duplikasi kode UI yang signifikan.
- Konsistensi pengalaman user antara receipt scan dan voice input.

### 4. Entry Point: AddTransactionBottomSheet
Voice input diakses sebagai opsi ke-3 di `AddTransactionBottomSheet` (di samping "Input Manual" dan "Scan Struk"), sesuai DESIGN.md section 3.

## Konsekuensi
- Perlu menambahkan permission `RECORD_AUDIO` di AndroidManifest.xml.
- Perlu menambahkan `<queries>` untuk `RecognitionService` di AndroidManifest.xml (Android 11+ package visibility).
- `AiRepository` bertambah satu method (`parseVoiceText`) dengan prompt template yang berbeda dari `parseReceiptText`.
- `ScanResultViewModel` perlu method baru untuk menerima voice text dan memulai parsing flow.
- `AddTransactionBottomSheet` perlu callback tambahan `onVoiceInputClick`.
- Bergantung pada Google Speech Services di device — perlu graceful error handling jika tidak tersedia.
