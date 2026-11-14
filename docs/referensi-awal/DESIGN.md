# DESIGN.md — Finance Tracker App

> Design brief untuk dieksekusi jadi desain visual aplikasi utuh. Disusun dari PRD workshop 2 hari ("Kawan Kode"). Arah visual mengikuti referensi screenshot yang diberikan: dark theme, modern fintech, aksen warna cerah. Bagian yang ditandai **(asumsi)** adalah keputusan desain yang aku ambil sendiri karena belum dibahas eksplisit — silakan dikoreksi sebelum dieksekusi.

---

## 1. Ringkasan Produk

Aplikasi pencatatan keuangan pribadi (income & expense tracker) dengan tiga cara input: manual, foto struk (AI OCR), dan suara (voice-to-text). Ada juga fitur insight keuangan berbasis AI. Target pengguna: orang Indonesia yang ingin mencatat cashflow harian dengan cepat.

**Nada produk (asumsi):** modern, percaya diri, "premium tapi santai" — seperti app fintech kekinian, bukan spreadsheet yang kaku.

---

## 2. Arah Visual

### Konsep: Dark Modern Fintech

Mengikuti referensi: background gelap nyaris hitam, card dengan sudut membulat besar, aksen warna cerah (lime/hijau neon, lavender, coral, kuning) yang dipakai secara selektif untuk kategori & status. Grafik jadi elemen sentral — wave line untuk tren saldo, donut/ring chart untuk breakdown kategori. Ini kontras tinggi (dark bg + elemen terang) yang terasa premium dan mudah dibaca angka besarnya.

### 2.1 Palet Warna

| Nama | Hex | Peran |
|---|---|---|
| Background | `#121214` | Background utama layar (hitam kehijauan sangat gelap) |
| Surface | `#1D1E22` | Card, sheet, elemen di atas background |
| Surface Raised | `#26272C` | Card sekunder / elemen di atas Surface (misal pill button di dalam card) |
| Text Primary | `#F5F5F3` | Teks utama (putih pudar, bukan pure white) |
| Text Secondary | `#8E8F94` | Label, caption, teks sekunder |
| Lime (primary accent) | `#C6F24E` | Warna brand utama — CTA, highlight positif, tab aktif |
| Lavender | `#B7B4F0` | Aksen sekunder — savings/investasi, elemen netral-positif |
| Coral | `#F2735C` | Expense, warning, aksen negatif |
| Yellow | `#F0D95B` | Aksen tersier — kategori lain, progress menengah |

**(asumsi)** Empat warna aksen (Lime/Lavender/Coral/Yellow) dipakai konsisten sebagai palet kategori — tiap kategori transaksi baru dapat salah satu dari 4 warna ini secara rotasi, bukan warna bebas tak terbatas, supaya visual tetap terkontrol.

### 2.2 Tipografi

| Peran | Font (arahan) | Keterangan |
|---|---|---|
| Display / Angka besar | Geometris, bold, sedikit rounded — misal **General Sans** atau **Sora** | Dipakai untuk saldo & nominal besar di Home/Statistik |
| Angka tabel/list | Sama seperti display tapi weight medium, tabular figures | Supaya nominal di list transaksi tetap rapi sejajar |
| Body / UI | **Inter** atau **Plus Jakarta Sans** | Label, deskripsi, form |

**(asumsi)** Tidak pakai monospace — beda dari draft sebelumnya. Angka tetap pakai tabular figures dari font sans yang sama, mengikuti gaya di screenshot referensi (angka besar bold, bukan gaya struk kasir).

### 2.3 Layout & Signature Element

**Signature:** kombinasi dua elemen data-viz yang jadi pusat visual app:
1. **Wave balance chart** di Home — garis tren saldo antar bulan, dua garis tipis (income vs expense) yang saling silang, dengan titik highlight nominal saat disentuh (persis pola di referensi)
2. **Cashflow ring** di Statistik — donut chart multi-segmen (tiap segmen = 1 kategori, warna dari 4 aksen di atas) dengan total nominal di tengah lingkaran

**Layout umum:**
- Card besar bersudut sangat membulat (radius ~24-28px), banyak padding di dalam
- Segmented pill selector (All/Income/Outcome/Savings-style) untuk filter — pill aktif diisi warna Lime penuh, teks jadi gelap
- Icon di dalam lingkaran kecil berwarna soft-tint dari warna kategorinya (misal icon expense dalam lingkaran coral transparan)
- Top bar minimal: tombol back bulat semi-transparan di kiri, judul di tengah, menu "..." di kanan

---

## 3. Struktur Navigasi

**Bottom navigation, 4 tab (ikon saja + 1 FAB tengah menonjol):**
1. **Beranda** (Home) — wave chart saldo + daftar transaksi terbaru
2. **Statistik** — cashflow ring + AI insight
3. **+ (FAB tengah, lingkaran Lime menonjol ke atas)** — bottom sheet pilihan: Input Manual / Scan Struk / Rekam Suara
4. **Kategori** — kelola kategori

**(asumsi)** Sama seperti draft sebelumnya — belum ada tab Profil/Settings karena belum disebutkan di PRD.

---

## 4. Daftar Layar & Alur

### 4.1 Beranda (Home)
- Top bar: sapaan singkat + ikon notifikasi (opsional)
- Segmented pill filter: **Semua / Income / Outcome** — pill aktif warna Lime
- **Total balance** — angka besar bold di atas, label kecil di bawahnya
- **Wave chart** tren saldo 6 bulan terakhir (garis income lavender, garis expense coral, atau income lime/expense coral — pilih 2 warna kontras), dengan bubble tooltip nominal saat titik tertentu di-highlight
- Dua card ringkas berdampingan: **Total Income** (icon lingkaran lime) & **Total Expense** (icon lingkaran coral)
- Banner insight singkat (opsional, pill panjang lavender): "Pengeluaran naik 12% dari bulan lalu" — mirip pola banner di referensi
- **Transactions History** — list dikelompokkan per tanggal, tiap baris: icon kategori bulat kecil, nama transaksi, kategori, nominal rata kanan (hijau untuk income, coral untuk expense)
- Empty state: card besar dengan ilustrasi garis minimal + CTA "Catat transaksi pertamamu"

### 4.2 Input Manual (Tambah/Edit Transaksi)
- Toggle Income/Outcome di atas, gaya pill (aktif = Lime untuk income, Coral untuk outcome)
- Input nominal — sangat besar, jadi fokus utama layar
- Dropdown/chip kategori (chip berwarna sesuai kategori, dari 4 palet aksen)
- Input catatan (opsional)
- Date picker (default: hari ini)
- Tombol simpan full-width, warna Lime, teks gelap, sudut sangat membulat (pill)

### 4.3 Kategori
- List kategori dikelompokkan Income/Outcome, tiap kategori pakai icon bulat bertint warna kategorinya
- Tambah/edit kategori: pilih nama, tipe, dan salah satu dari 4 warna aksen (bukan color picker bebas)
- Delete: konfirmasi dialog singkat — "Kategori dihapus, transaksi lama tetap tersimpan dengan nama kategori ini"

### 4.4 Statistik
- Segmented pill filter periode (Minggu/Bulan/Custom)
- **Cashflow ring** — donut multi-segmen di tengah layar, total nominal besar di pusat lingkaran, label kategori & persentase di sekitar/legend di bawah
- List breakdown per kategori di bawah ring (bar progress kecil per kategori, warna sesuai)
- Card **AI Cashflow Insight** — tombol pill "Buat Insight" (Lime), setelah ditekan → loading singkat → teks insight muncul di card Surface Raised

### 4.5 AI Receipt Scanner
1. **Layar kamera** — viewfinder full-screen dengan frame sudut (corner bracket ala scanner modern, bukan motif struk), tombol shutter bulat besar Lime di bawah
2. **Loading state** — garis scan tipis Lime bergerak naik-turun di atas frame + teks "Membaca struk..."
3. **Review hasil** — card Surface dengan field editable (nominal besar di atas, kategori chip, tanggal, catatan/merchant), tombol simpan Lime di bawah
4. **Error state** — pesan jelas di card Coral-tint + tombol "Coba foto ulang" atau "Isi manual saja"

### 4.6 Voice Input
1. **Layar rekam** — tombol mic bulat besar Lime di tengah, animasi waveform (batang-batang vertikal Lime) merespons volume suara, transkrip live muncul di bawah
2. **Loading state** — "Memproses ucapanmu..."
3. **Review hasil** — sama seperti Receipt Scanner
4. **Error state** — "Suara kurang jelas, coba ucapkan lagi" atau opsi isi manual

---

## 5. Komponen UI Utama

| Komponen | Catatan desain |
|---|---|
| Balance Card | Card Surface besar, angka bold, wave chart di dalamnya |
| Cashflow Ring | Donut chart multi-warna, dipakai di Statistik |
| Segmented Pill Filter | Pill grup, item aktif solid Lime dengan teks gelap |
| Category Chip/Icon | Lingkaran kecil bertint warna kategori (dari 4 aksen) |
| Transaction Row | Icon bulat + nama + kategori + nominal rata kanan berwarna |
| Insight Banner | Pill panjang lavender/lime-tint dengan panah kecil di kanan |
| FAB + Bottom Sheet | FAB Lime menonjol di tengah nav, buka sheet 3 opsi input |
| Loading State (AI) | Scan-line untuk kamera, waveform bar untuk suara — bukan spinner generik |

---

## 6. Copy & Microcopy (arahan nada)

- Aktif, jelas, tanpa istilah teknis. "Simpan transaksi", bukan "Submit data"
- Error tidak menyalahkan user: "Struk kurang jelas, coba foto lagi ya"
- Empty state sebagai ajakan bertindak: "Belum ada catatan bulan ini — yuk mulai catat"
- **(asumsi)** Semua microcopy dalam Bahasa Indonesia, angka nominal tetap format Rupiah meski gaya visual terinspirasi referensi berbahasa Inggris/format Dollar

---

## 7. Yang Perlu Dikonfirmasi Sebelum Eksekusi Penuh

- [ ] Apakah 4 warna aksen (Lime/Lavender/Coral/Yellow) sudah final, atau ada warna brand "Kawan Kode" yang harus masuk?
- [ ] Icon set: pakai icon set standar (misal Phosphor/Lucide, gaya line/rounded) atau custom?
- [ ] Apakah perlu light mode sebagai alternatif, atau dark-only seperti referensi?
- [ ] Apakah wave chart & cashflow ring dieksekusi sebagai chart statis (mockup) atau perlu interaktif (tooltip saat drag) di tahap desain ini?