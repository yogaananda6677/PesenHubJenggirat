# Rencana Implementasi & Jadwal Eksekusi — PesenHub Jenggirat

## Tahapan Eksekusi

### Fase 1: Setup Repository & Git
- [x] Susun PRD lengkap di `PesenHubJenggirat/PRD.md`
- [x] Buat rencana implementasi (`PLANNING.md`)
- [ ] Buat remote repositori GitHub `yogaananda6677/PesenHubJenggirat` via `gh repo create`
- [ ] Push commit inisial dokumen dan struktur proyek ke GitHub
- [ ] Buat GitHub Issues untuk pembagian tugas (Task Yoga & Task Rydo)

### Fase 2: Setup Proyek Android (Gaya Modul PM & PML)
- [ ] Inisialisasi struktur Android Gradle di `PesenHubJenggirat/app`
- [ ] Setup `settings.gradle.kts` & `build.gradle.kts` dengan dependency:
  - Firebase BoM (Firestore, Auth, FCM, In-App Messaging)
  - Supabase Storage & Postgrest
  - Volley
  - OSMdroid & AirLocation (GPS / OpenStreetMap modul Bab 13)
  - ZXing Embedded (QR Code modul Bab 10)
  - ViewBinding enabled
- [ ] Setup `AndroidManifest.xml` lengkap dengan permission:
  - Internet, Access Fine Location, Camera, Read/Write Storage, Post Notifications
- [ ] Setup resource tema, warna, dan strings modul

### Fase 3: Pengerjaan Bagian Yoga (Core, POS Kasir, SQLite & Web)
- [ ] `DBOpenHelper.kt` (SQLite lokal sesuai Bab 08 PM)
- [ ] `KasirFragment.kt` + `fragment_kasir.xml`:
  - RadioButton (pembayaran tunai/QRIS)
  - CheckBox (topping martabak)
  - AutoCompleteTextView (nama pelanggan)
  - Spinner (kategori menu)
  - EditText & Button
- [ ] `MenuFragment.kt` + `LaporanFragment.kt`:
  - DatePickerDialog & TimePickerDialog
  - Camera & Gallery picker (Supabase Storage upload)
- [ ] Web Customer (`web/index.html` + `web/app.js`):
  - Halaman pemesanan web yang langsung terhubung ke Firestore

### Fase 4: Pengerjaan Bagian Rydo (KDS Antrean, Auth, QR, Peta & Notifikasi)
- [ ] `AntrianFragment.kt` + `fragment_antrian.xml`:
  - ListView pesanan
  - ContextMenu (long click item pesanan)
  - PopupMenu (update status pesanan)
- [ ] `LoginActivity.kt` + Firebase Authentication & SharedPreferences
- [ ] `QRScanActivity.kt` & QR Generator
- [ ] `MapsActivity.kt` (OSMdroid MapView + GPS)
- [ ] `PesenHubFCMService.kt` + MediaPlayer notifikasi suara
- [ ] Slide presentasi UTS

---

## Daftar GitHub Issues yang Akan Dibuat
1. **[Core] Setup Android Project, Gradle Dependencies & Module Convention** (Assignee: Yoga)
2. **[Feature] SQLite Database Local Cache via DBOpenHelper** (Assignee: Yoga)
3. **[Feature] POS Kasir UI with RadioButton, CheckBox, AutoComplete, Spinner** (Assignee: Yoga)
4. **[Feature] Firestore Integration for Menu & Orders** (Assignee: Yoga)
5. **[Feature] Menu Management with Camera, Gallery & Supabase Storage** (Assignee: Yoga)
6. **[Feature] Laporan Penjualan with DatePickerDialog & TimePickerDialog** (Assignee: Yoga)
7. **[Feature] Customer Web Application integrated with Firestore** (Assignee: Yoga)
8. **[Feature] KDS Antrean with ListView, ContextMenu & PopupMenu** (Assignee: Rydo)
9. **[Feature] Firebase Authentication & SharedPreferences Session** (Assignee: Rydo)
10. **[Feature] QR Code Generation & Camera QR Scanner** (Assignee: Rydo)
11. **[Feature] GPS & OpenStreetMap (OSMdroid) Outlet Location** (Assignee: Rydo)
12. **[Feature] Firebase Cloud Messaging (FCM), In-App Messaging & Audio Alert** (Assignee: Rydo)
13. **[Docs] Technical Documentation & Presentation Slides** (Assignee: Yoga & Rydo)
