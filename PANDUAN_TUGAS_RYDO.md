# BRIEF & PANDUAN PENGERJAAN TUGAS — RYDO
**Proyek:** PesenHub Jenggirat (Android Kotlin Native)  
**Repository GitHub:** https://github.com/yogaananda6677/PesenHubJenggirat  
**Branch Utama:** `main` (Silakan buat branch per fitur, misal: `feat/antrian-kds`, `feat/auth`, dll)

---

## 🎯 DAFTAR TUGAS & ISSUE GITHUB RYDO

Kamu bertanggung jawab pada **6 Modul Fitur** berikut (Total kontribusi poin sangat besar di UTS):

| Issue | Judul Fitur | Bobot | Checklist Penilaian |
|---|---|---|---|
| **#8** | KDS Antrean Pesanan Dapur | 3% | `ListView` (#9), `ContextMenu` (#11), `PopupMenu` (#12) |
| **#9** | Otentikasi Kasir & Sesi | 7% | `Firebase Authentication` (#29), `SharedPreferences` (#23) |
| **#10** | QR Code Pesanan | 2% | Generator QR Code & Scanner Kamera ZXing (#25) |
| **#11** | Lokasi Outlet & Peta | 4% | `GPS` (#21), `OpenStreetMap` / OSMdroid (#22) |
| **#12** | Notifikasi & Audio Alert | 8% | `Firebase Cloud Messaging / FCM` (#31), `In-App Messaging` (#32), `MediaPlayer` (#24) |
| **#13** | Dokumen Teknis & Slide Presentasi | 15% | Format UTS 10 Poin + Slide Demo Kelas |

---

## 📋 STANDAR GAYA PENULISAN KODE (WAJIB SESUAI MODUL DOSEN)

Dosen sangat teliti dengan format kode yang diajarkan di kelas. Buka folder `PM/` dan `PML/` di root proyek untuk melihat PDF modul asli:

1. **ViewBinding Ringkas:**
   ```kotlin
   lateinit var b: ActivityMainBinding // atau FragmentAntrianBinding
   b = ActivityMainBinding.inflate(layoutInflater)
   setContentView(b.root)
   ```
2. **KDS Antrean Pesanan (Issue #8):**
   - Gunakan `ListView` (`b.lsPesanan`).
   - Daftarkan `registerForContextMenu(b.lsPesanan)`.
   - Override `onCreateContextMenu(...)` dan `onContextItemSelected(...)` (untuk ubah status ke *PREPARING* / *COMPLETED*).
   - Gunakan `PopupMenu(context, view)` untuk tombol overflow titik tiga.
   - Sambungkan listener realtime Firestore: `db.collection("orders").addSnapshotListener { snapshot, e -> ... }`.
3. **Firebase Authentication & SharedPreferences (Issue #9):**
   - Gunakan `FirebaseAuth.getInstance()`.
   - Simpan status login di `getSharedPreferences("pesenhub_pref", Context.MODE_PRIVATE)`.
4. **QR Code Generator & Scanner (Issue #10 — Modul Bab 10 PM):**
   - Library `zxing-android-embedded` dan `zxing:core` sudah terpasang di gradle.
   - Gunakan `BarcodeEncoder().encodeBitmap(orderId, BarcodeFormat.QR_CODE, 400, 400)` untuk membuat QR.
   - Gunakan `IntentIntegrator(this).initiateScan()` untuk membuka kamera scanner.
5. **GPS & OpenStreetMaps OSMdroid (Issue #11 — Modul Bab 13 PM):**
   - Gunakan `MapView`, `Configuration.getInstance().load(...)`, `GeoPoint(lat, lng)`.
   - Gunakan `MyLocationNewOverlay(GpsMyLocationProvider(context), map)` dan `OverlayItem` untuk menandai lokasi outlet martabak.
6. **Notifikasi FCM & Bel Pesanan (Issue #12 — Modul Pertemuan 05 PML):**
   - Buat class `PesenHubFCMService : FirebaseMessagingService()`.
   - Bunyikan suara bel pesanan baru masuk menggunakan `MediaPlayer.create(this, R.raw.bell_order).start()`.

---

## 🚀 LANGKAH AWAL UNTUK RYDO:
1. Clone repo:
   ```bash
   git clone https://github.com/yogaananda6677/PesenHubJenggirat.git
   cd PesenHubJenggirat
   ```
2. Buka proyek di Android Studio, lalu jalankan:
   ```bash
   ./gradlew assembleDebug
   ```
   (Setup Gradle, DataBinding, dan ViewBinding sudah teruji `BUILD SUCCESSFUL`).
3. Pilih Issue di tab GitHub Issues, buat branch, dan mulai kerjakan fitur kamu!
