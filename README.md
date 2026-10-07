# TebakDunia

Game tebak lokasi dunia untuk Android, dibuat dengan Kotlin, Google Maps SDK, dan Street View.

## Cara menjalankan
1. Buka folder ini di Android Studio.
2. Buat API key di Google Cloud Console, aktifkan **Maps SDK for Android** (billing harus aktif).
3. Buat file `local.properties` di root proyek, isi: `MAPS_API_KEY=KEY_KAMU`
4. Jalankan di HP atau emulator.

## Fitur
- Street View fullscreen, nama jalan disembunyikan
- 5 ronde, titik acak dari kota-kota dunia
- Peta tebakan, garis jarak, skor maks 5000 per ronde
- Layar skor akhir dan main lagi

> `local.properties` sudah ada di `.gitignore`, jadi API key tidak ikut ter-upload.
