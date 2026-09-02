# ⚡ VoltGuard

Aplikasi Android (Kotlin + Jetpack Compose) untuk **memantau tegangan & daya yang masuk**
saat HP kamu di-charge. Tampilan modern, berjalan real-time, dengan alert jika tegangan/suhu
di luar ambang yang kamu tentukan.

![status](https://img.shields.io/badge/status-release%20ready-2ea44f)
![stack](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF)
![compose](https://img.shields.io/badge/Compose-Material%203-6750A4)

---

## 📱 Fitur

- **Gauge tegangan real-time** (arc 240°) — menampilkan tegangan sel (atau tegangan input
  adapter/VIN bila ROM menyediakan), dengan warna status: 🟢 baik, 🟡 waspada, 🔴 bahaya.
- **Detail lengkap daya**: tegangan (mV/V), arus input & isi, daya (Watt), suhu, level,
  status pengisian, tipe sumber (AC / USB / nirkabel), teknologi baterai.
- **Riwayat tersimpan** (Room DB) + grafik garis — data direkam otomatis tiap ±10 detik
  dan bertahan setelah aplikasi ditutup.
- **Alert & notifikasi**: foreground service terus memantau di latar belakang, memberi
  notifikasi (dan getar opsional) bila tegangan/suhu melewati ambang.
- **Pengaturan ambang** bisa disesuaikan lewat UI (slider) — tersimpan di DataStore.
- UI **dark modern**, Material 3, fully responsive, minSdk 24 (Android 7.0+).

## 🧠 Catatan penting soal data

Android publik **tidak** selalu membuka *tegangan output adapter (VIN/charger input)*
untuk semua perangkat. VoltGuard membaca apa yang disediakan `BatteryManager`:

| Data                       | Ketersediaan                          |
| -------------------------- | ------------------------------------- |
| Tegangan sel               | ✅ selalu                              |
| Arus isi / input           | ✅ hampir semua                        |
| Daya (W), suhu, status     | ✅ selalu                              |
| Tegangan input adapter VIN | ⚠️ hanya jika ROM menyediakan (best-effort) |

Jika VIN tidak tersedia di perangkatmu, aplikasi menampilkan **tegangan sel** sebagai
acuan dan menandainya di label. Perhitungan *daya* (Watt) memakai tegangan × arus aktual,
jadi tetap akurat.

## 🏗️ Struktur

```
app/src/main/java/com/voltguard/app/
├── MainActivity.kt              # Entry + bottom navigation (3 tab)
├── VoltGuardApp.kt              # Application + single repo instance
├── data/
│   ├── PowerSnapshot.kt         # Model data + formatter + penilaian kesehatan
│   ├── PowerCollector.kt        # Baca BatteryManager -> PowerSnapshot
│   ├── PowerRepository.kt       # Sampling, alert, persist (sumber kebenaran)
│   ├── AlertEvent.kt
│   ├── db/                       # Room (Entity, DAO, Database)
│   └── prefs/                    # DataStore (ambang, toggle)
├── service/CollectorService.kt  # Foreground service + notifikasi
└── ui/
    ├── MainViewModel.kt
    ├── components/               # Gauge, grafik, kartu, dll
    ├── screens/                  # Dashboard, Riwayat, Pengaturan
    └── theme/                    # Tema Material 3 (dark)
```

## 📦 Build APK

> ⚠️ Build Android membutuhkan **mesin x86-64** dengan Android SDK.
> (Lingkungan dev yang dipakai saat pengembangan adalah Android **aarch64**,
>  sehingga aapt2 daemon-nya tidak berjalan di sana — build-nya dijalankan di CI x86-64.)

### Opsi 1 — GitHub Actions (paling mudah)
Push repo ke GitHub. Workflow `.github/workflows/android.yml` otomatis:
1. Menyiapkan JDK 17 + Android SDK,
2. Membuat keystore fresh,
3. `./gradlew :app:assembleRelease`,
4. Mengunggah APK + SHA-256 sebagai **artifact** (download langsung).

Lalu buka tab **Actions** → run-nya → download `VoltGuard-release.apk`.

### Opsi 2 — Lokal (PC x86-64)
```bash
# Prasyarat: JDK 17, Android SDK (install via Android Studio / cmdline-tools)
# Siapkan keystore (sekali saja):
mkdir -p keystore
keytool -genkeypair -v -keystore keystore/voltguard-release.jks \
  -alias voltguard -keyalg RSA -keysize 2048 -validity 10950 \
  -storepass GANTI_INI -keypass GANTI_INI \
  -dname "CN=VoltGuard, OU=Dev, O=VoltGuard, L=Jakarta, C=ID"

# Build release (signed) — set env kredensial sesuai keystore-mu:
export VOLTGUARD_KEYSTORE_PASSWORD=GANTI_INI
export VOLTGUARD_KEY_PASSWORD=GANTI_INI
export VOLTGUARD_KEY_ALIAS=voltguard
./gradlew :app:assembleRelease

# Hasil APK:
app/build/outputs/apk/release/app-release.apk
```

### Opsi 3 — Debug (untuk develop)
```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Install ke HP
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## 🔐 Menandatangani (signing) produksi

Keystore dibuat otomatis di CI (fresh setiap build) supaya tidak bocor.
**Untuk deploy permanen** (update app yang sudah terinstall), gunakan keystore tetap
yang sama tiap kali — simpan aman sebagai GitHub Secrets:
`VOLTGUARD_KEYSTORE_BASE64`, `VOLTGUARD_KEYSTORE_PASSWORD`, `VOLTGUARD_KEY_PASSWORD`,
`VOLTGUARD_KEY_ALIAS`.

## ✅ Permission

| Permission             | Alasan                                  |
| ---------------------- | --------------------------------------- |
| FOREGROUND_SERVICE     | Monitoring latar belakang               |
| POST_NOTIFICATIONS     | Notifikasi alert & status               |
| VIBRATE                | Getar saat alert (opsional)             |

Aplikasi **hanya membaca** — tidak memodifikasi perangkat.

## 🧩 Versi & dependensi

- Kotlin 2.1.0, Compose BOM 2024.10.00 (Material 3)
- Room 2.6.1, DataStore 1.1.1, Coroutines 1.9.0
- compileSdk 35, targetSdk 35, minSdk 24, AGP 8.9.1, Gradle 8.12

## 📄 Lisensi

MIT — silakan gunakan & modifikasi.
