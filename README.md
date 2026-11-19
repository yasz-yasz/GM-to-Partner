# GM to Partner — Kalkulator Penukaran GarudaMiles ke Maskapai Partner

Aplikasi Android offline untuk menghitung kebutuhan **GarudaMiles** (Economy & Business Class) pada penukaran tiket (*award ticket*) maskapai partner **SkyTeam** maupun **Non-SkyTeam Partner** sesuai tabel jarak **TPM (Ticketed Point Mileage) Group A–I** dari website resmi Garuda Indonesia.

## Fitur Utama
- **Kalkulator Rute 1 Leg hingga 3 Leg (Single & Mix Partner):** Masukkan bandara asal dan tujuan (misalnya `CGK → IST`), aplikasi otomatis menyusun rekomendasi rute direct, 1 transit (*single partner* seperti Saudia `CGK → JED → IST`, Emirates `CGK → DXB → IST`, Qatar `CGK → DOH → IST`), maupun kombinasi **Mix Partner** antar maskapai rekanan.
- **Database Offline Global:** Mencakup **22 maskapai partner GarudaMiles**, **578 bandara global**, **3,137 rute direct & fifth freedom** (6,274 segmen penerbangan dua arah), termasuk **34 rute Fifth Freedom** aktif.
- **Perhitungan TPM Per Segmen:** Mengikuti aturan resmi GarudaMiles (dihitung terpisah per segmen penerbangan berdasarkan jarak *Great Circle* dalam mil).
- **Opsi Fleksibel:**
  - Pilih kabin **Economy** atau **Business**, serta mode **One-Way (OW)** atau **Round-Trip (PP ×2)**.
  - Filter jumlah transit (Direct, Maks 1 Transit, Maks 2 Transit) dan jenis kombinasi (Semua, Single Partner, Mix Partner).
  - Toggle tarif **Group D Business**: `47.500 miles (Aktual Sistem)` vs `77.500 miles (Sesuai Web Typo)`.
  - Toggle opsional rute **Singapore Airlines (Non-Codeshare)** dan **Aeroflot (Suspended)**.
- **Explorer Fifth Freedom & Direktori 22 Maskapai:** Cari rute *sweet spot* fifth freedom atau telusuri seluruh jaringan rute masing-masing maskapai partner.
- **Tabel Acuan TPM Lengkap:** Referensi cepat Group A–I lengkap beserta ketentuan penukaran GarudaMiles.

## Cara Build APK Otomatis di GitHub Actions
1. Ekstrak seluruh isi file ZIP ini dan upload ke root repository GitHub Anda.
2. Buka tab **Actions** pada repository GitHub.
3. Workflow **Build GM to Partner APK** akan berjalan otomatis setiap kali ada *push* ke branch `main`/`master` (atau klik **Run workflow** secara manual).
4. Setelah selesai, unduh APK dari bagian **Artifacts → GM-to-Partner-Debug-APK**.
