# Guide: Menyusun PRD & Design Brief Bareng Claude

> Dokumen ini merangkum bagaimana proses menyusun PRD dan design brief (DESIGN.md) untuk sebuah codebase berlangsung — mulai dari ide di kepala sampai jadi dua dokumen yang siap dieksekusi ke tahap development/desain. Tujuannya bukan supaya kamu meniru persis studi kasusnya, tapi supaya kamu paham **pola diskusi** yang bikin hasilnya berguna, bukan sekadar teks generik.

---

## Kenapa Bukan Cuma "Bikinkan PRD"?

Prompt seperti *"bikinkan PRD aplikasi finance tracker"* akan menghasilkan dokumen generik — Claude akan menebak-nebak konteks yang sebenarnya cuma kamu yang tahu (kenapa aplikasi ini dibuat, batasan yang kamu punya, bagian mana yang sudah ada di kepalamu). Hasilnya kelihatan lengkap, tapi banyak asumsi yang salah dan ujungnya kamu revisi ulang dari nol.

Pola yang lebih efektif: **kasih konteks di awal, biarkan Claude bertanya balik untuk hal yang ambigu, lalu koreksi bertahap** — bukan sekali jadi.

---

## Prinsip #1: Kasih Konteks Konkret di Awal

Saat mulai menyiapkan codebase untuk aplikasi finance tracker ini, prompt pembukanya bukan cuma "buatkan PRD", tapi menyertakan:
- Stack yang mau dipakai (Jetpack Compose + Room)
- Scope yang sudah jelas di kepala (fitur dasar apa saja vs fitur AI yang lebih eksperimental)
- Tools/teknologi tambahan yang sudah ditentukan (MLKit, Groq API)

**Semakin spesifik konteks di prompt pertama, semakin sedikit asumsi yang salah tebak.** Kalau kamu sendiri belum yakin dengan detail tertentu, tidak apa — itu gunanya prinsip berikutnya.

---

## Prinsip #2: Minta Claude Bertanya Dulu untuk Hal yang Ambigu

Contoh nyata dari sesi ini — di akhir prompt pertama ada instruksi:

> "Tanya-tanya ke aku lebih lanjut untuk lebih detailnya ya"

Ini penting untuk task besar/terbuka seperti PRD. Tanpa instruksi ini, Claude cenderung langsung menebak dan mengeksekusi. Dengan instruksi ini, Claude nanya dulu hal-hal yang akan mengubah struktur dokumen — misalnya seberapa detail rancangan teknisnya harus ditulis, dan bagaimana bentuk fitur statistik yang dibayangkan — sebelum menulis draft pertama.

**Kapan pola ini dipakai lagi di sesi ini:** setiap kali mau mengeksekusi task besar berikutnya (DESIGN.md, breakdown materi presentasi, guide ini sendiri), selalu diminta "tanya dulu sebelum eksekusi".

---

## Prinsip #3: Koreksi Itu Wajar — Makin Spesifik, Makin Cepat Benar

Draft pertama PRD di sesi ini salah asumsi: bagian dasar aplikasi (base features) dikira sudah jadi, padahal maksudnya baru mau dirancang bareng dari nol. Koreksinya singkat:

> "sorry maksudnya 70% tersedia itu bukan berarti tersedia, tetapi aku pengen kamu ikut membuat rancangannya. paham ga?"

Yang bikin koreksi ini efektif: **langsung ke akar masalah** (kesalahpahaman scope), bukan cuma "ini salah, ulangi". Claude bisa langsung paham bagian mana dari dokumen yang perlu direvisi total, bukan mulai dari nol.

---

## Prinsip #4: Putuskan Satu-Satu, Bukan Dump Semua Sekaligus

Setelah draft PRD ada, keputusan teknis (networking library, dependency injection, chart library, dst) tidak diputuskan sekaligus di awal — tapi muncul bertahap lewat pertanyaan pilihan ganda dari Claude, dijawab satu per satu:

```
* networking: retrofit
* manual di
* pakai library chart yang paling simple dan mudah dimengerti saja
* API key peserta bikin sendiri
* delete kategori tidak memengaruhi transaksi...
```

Bahkan setelah dijawab, ada koreksi lanjutan ("TransactionEntity tetep hold categoryId dan categoryName ya") — ini normal, dokumen hidup itu direvisi terus sampai kamu yakin dengan keputusan teknisnya, bukan ditulis sekali lalu final.

---

## Prinsip #5: Dokumen Turunan, Bukan Mulai dari Nol Lagi

DESIGN.md tidak diminta terpisah dari nol — instruksinya:

> "Dari PRD tersebut, tolong buatkan DESIGN.md..."

Dengan merujuk ke PRD yang sudah ada, Claude tidak perlu menebak ulang scope fitur atau keputusan teknis yang sudah dibuat — tinggal diterjemahkan ke bahasa desain. **Kalau kamu sudah punya satu dokumen solid, manfaatkan sebagai basis untuk dokumen berikutnya**, jangan mulai percakapan baru dari kosong.

---

## Prinsip #6: Referensi Visual Konkret > Deskripsi Panjang

Untuk urusan desain, instruksi teks ("buat yang modern") jauh lebih ambigu dibanding referensi visual. Di sesi ini, arah desain berubah total dari "konsep struk digital" ke "dark modern fintech" hanya lewat:

> [screenshot referensi] + "jangan pakai konsep struk digital, buat modern design, sesuai sama screenshot yang aku kirim"

Satu gambar referensi menghasilkan revisi total palet warna, tipografi, dan komponen — jauh lebih cepat & presisi dibanding coba jelaskan "modern" pakai kata-kata.

---

## Prinsip #7: Untuk Task Panjang, Minta Outline Dulu

Sebelum konten materi presentasi yang panjang ditulis penuh, diminta outline dulu:

> "coba kamu kasih outlinenya dulu"

Ini menghemat banyak waktu — kalau strukturnya salah (misal urutan topik atau pembagian materi tidak pas), lebih murah diperbaiki di level outline daripada setelah konten penuh ditulis semua. Baru setelah outline disetujui (dengan tambahan konteks lanjutan), konten penuh di-generate.

---

## Prinsip #8: Iterasi Lanjutan Setelah "Selesai"

Dokumen yang sudah "jadi" bukan berarti berhenti di situ. Setelah satu dokumen jadi, masih wajar ada permintaan lanjutan — misalnya perbaikan visual/styling, atau pertanyaan strategi non-teknis (misal soal bagaimana mendistribusikan kodenya nanti) yang ujungnya memengaruhi keputusan-keputusan sebelumnya.

**Poin pentingnya:** proses ini tidak linear satu arah dari 0 ke selesai — tapi bolak-balik antar dokumen, dan itu normal.

---

## Ringkasan Alur (Studi Kasus Proyek Ini)

1. Jelaskan ide + konteks (stack, scope fitur) → minta Claude nanya dulu
2. Jawab pertanyaan → dapat draft PRD pertama
3. Koreksi kesalahpahaman scope (bagian dasar aplikasi belum jadi, baru mau dirancang)
4. Putuskan detail teknis satu-satu lewat pertanyaan pilihan ganda
5. Minta dokumen turunan (DESIGN.md) berdasarkan PRD yang sudah solid
6. Revisi arah desain total pakai referensi visual (screenshot)
7. Minta outline dulu untuk materi panjang → revisi outline → baru generate konten penuh
8. Polish lanjutan & diskusi keputusan strategi non-teknis yang berdampak ke dokumen sebelumnya

---

## Coba Sendiri

Kalau kamu mau praktik pola yang sama untuk codebase-mu sendiri, mulai dari:
1. Tulis konteks proyekmu sejelas mungkin (tujuan, stack, batasan yang sudah kamu tahu)
2. Tambahkan: *"tanya-tanya ke aku dulu sebelum eksekusi"*
3. Jawab pertanyaannya, lalu koreksi draft yang keluar sespesifik mungkin
4. Kalau butuh dokumen baru yang berkaitan, minta Claude membangunnya dari dokumen yang sudah ada — jangan mulai dari nol