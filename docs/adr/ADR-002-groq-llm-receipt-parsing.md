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
