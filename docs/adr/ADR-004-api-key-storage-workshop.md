# ADR 004: Penyimpanan API Key (Konteks Workshop)

## Status
Accepted

## Konteks
Groq API membutuhkan API Key untuk autentikasi. Dalam aplikasi production, API Key seharusnya disimpan di backend server untuk keamanan.

## Keputusan
Untuk keperluan **Workshop**, API Key disimpan di `local.properties` sebagai `GROQ_API_KEY` dan dibaca lewat `BuildConfig`. Setiap peserta mengisi key mereka sendiri sebelum menjalankan aplikasi. Keputusan awalnya hardcoded di `GroqApiConstants.kt`, tetapi diubah agar key tidak ikut ter-push ke GitHub.

## Alasan
- Target audiens workshop adalah pemula, sehingga tidak realistis untuk meminta mereka men-setup backend terpisah atau environment variables/secrets Gradle yang rumit.
- Ini memangkas waktu setup dan fokus pada pembelajaran integrasi AI-nya.

> **Peringatan**: Keputusan ini tidak boleh diterapkan untuk production release, karena API Key bisa di-reverse engineer dari APK.
