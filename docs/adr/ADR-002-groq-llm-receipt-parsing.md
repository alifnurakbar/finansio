# ADR 002: Penggunaan Groq LLM untuk Receipt Parsing

## Status
Accepted

## Konteks
Setelah mendapatkan teks mentah (raw text) dari ML Kit Text Recognition, kita perlu mengubah teks yang tidak beraturan tersebut menjadi structured data (JSON) berisi nama merchant, tanggal, total, dan daftar item. Menulis regular expression untuk semua format struk sangat sulit dan tidak scalable.

## Keputusan
Menggunakan LLM via Groq API dengan model `llama-3.1-70b-versatile` dan mode JSON.

## Alasan
- Groq menawarkan latency sangat rendah (LPU) yang ideal untuk use-case mobile.
- Llama 3.1 70B sangat handal dalam zero-shot extraction dan format JSON constraint.
- Daripada rule-based parsing, AI approach jauh lebih fleksibel untuk berbagai macam struk di Indonesia.

## Pembaruan (2026-11-07)
Model Llama 3.x sudah dihentikan di Groq dan API mengembalikan HTTP 404 `model_not_found`. Model diganti ke `openai/gpt-oss-120b`, yang masih mendukung mode JSON dan memberi hasil paling akurat di antara model yang tersedia. Karena ini reasoning model, `max_tokens` dinaikkan ke 4096 supaya jawabannya tidak terpotong.

## Pembaruan (2026-11-17)
Scan struk kini memakai model vision `qwen/qwen3.8-27b` yang membaca foto struk langsung, dengan `openai/gpt-oss-120b` sebagai cadangan teks. Lihat [ADR-012](ADR-012-akurasi-scan-struk.md).
