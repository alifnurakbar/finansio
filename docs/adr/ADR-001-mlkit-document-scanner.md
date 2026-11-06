# ADR 001: Penggunaan ML Kit Document Scanner API

## Status
Accepted

## Konteks
Fitur AI Receipt Scanner membutuhkan cara untuk mengambil gambar struk dan mengekstrak teksnya.

## Keputusan
Kita menggunakan kombinasi:
1. **ML Kit Document Scanner API**: Meng-handle UI kamera, deteksi tepi dokumen, auto-crop, dan enhancement gambar. Menghasilkan output JPEG berkualitas tinggi.
2. **ML Kit Text Recognition v2**: Mengambil output JPEG dari Scanner API dan mengekstrak teks raw.

## Alasan
- Mengurangi kompleksitas membuat custom camera UI.
- Hasil crop otomatis dari Document Scanner meningkatkan akurasi OCR.
- Kedua API berjalan on-device sehingga aman dan cepat.
- Sepenuhnya gratis dan didukung oleh Google Play Services.
