# ADR-011: Grafik Statistik Kustom

**Status:** Accepted
**Tanggal:** 2026-11-14
**Author:** Alif Nur Akbar

## Konteks
Grafik bawaan library Compose Charts pada halaman Statistik terlihat kaku, sulit disesuaikan dengan tema gelap, dan grafik lingkarannya tidak menggambar ulang saat data berubah. Halaman ini juga belum menampilkan pemasukan bulanan maupun tren antarbulan.

## Keputusan
Halaman Statistik digambar ulang dengan komponen kustom berbasis Compose `Canvas` (`ModernCharts.kt`):
- **Kartu saldo**: saldo bulan ini, batang rasio Outcome/Income, dan perubahan pengeluaran dibanding bulan lalu.
- **Tren 6 Bulan** (`GroupedColumnChart`): kolom Income vs Outcome berdampingan, ujung atas membulat 4dp dengan dasar rata, animasi tumbuh, sumbu ringkas (500rb, 2,5jt), dan gridline tipis. Ketuk sebuah periode untuk menyorot dan melihat angka pastinya.
- **Donat per kategori** (`DonutChart`): cincin tebal dengan celah 2dp antar-segmen, animasi putar, total di tengah, dan ketuk untuk menyorot. Disertai legenda berisi nama, nominal, persen, dan batang tipis.
- **Mode Minggu**: kolom arus kas harian Senin–Minggu, dengan hari ini disorot.

Prinsip visual mengikuti panduan visualisasi data: satu sumbu, legenda selalu ada untuk dua seri atau lebih, teks memakai warna teks (bukan warna seri), dan warna tidak menjadi satu-satunya penanda (ada nama kategori, angka, dan sorotan).

## Hasil validasi palet (validator `validate_palette.js`, mode gelap, latar `#1D1E22`)
| Palet | Hasil |
|---|---|
| Income `#C6F24E` vs Outcome `#F2735C` | Lolos buta warna (ΔE 20,1), penglihatan normal (ΔE 32,2), dan kontras. Hanya *lightness band* yang gagal (warna lebih terang dari standar), dan dipertahankan sebagai warna khas aplikasi. |
| Warna bawaan kategori pengeluaran | **Gagal**: Makanan (lime) vs Hiburan (kuning) hampir sama bagi buta warna (ΔE 2,0). Mengganti Hiburan menjadi biru `#3987E5` akan lolos (ΔE 15,7 / 19,8). |

Warna kategori adalah data milik pengguna (bisa diubah di **Kelola Kategori**), sehingga **sengaja tidak diubah otomatis**. Identitas kategori tetap terbaca lewat legenda bernama.

## Konsekuensi
- Compose Charts tetap dipakai untuk grafik di Beranda.
- Grafik kustom lebih mudah disesuaikan, tetapi perawatannya menjadi tanggung jawab proyek ini.
