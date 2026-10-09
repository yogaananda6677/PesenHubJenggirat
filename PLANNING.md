# Rencana Implementasi & Penyesuaian Arsitektur — PesenHub Jenggirat

## 💡 Keputusan Desain & Penyesuaian Baru:
1. **Penyimpanan Data Utama:**
   - **Cloud Firestore** adalah basis data utama untuk data transaksi kasir, katalog menu, dan pesanan pelanggan (Android & Web berbagi Firestore yang sama secara realtime).
2. **Penyimpanan Media & Gambar:**
   - **Supabase Storage** digunakan khusus untuk menyimpan foto menu martabak & terang bulan (di-upload dari kamera/galeri kasir).
3. **Penyimpanan Riwayat Selesai (Offline/Arsip):**
   - **SQLite (`DBOpenHelper`)** digunakan khusus menyimpan **arsip riwayat transaksi yang sudah selesai (`COMPLETED`)**. Data riwayat bersifat final / read-only sehingga sangat cocok di-cache ke SQLite lokal.
4. **Sesi Pengguna:**
   - **Firebase Authentication** untuk login kasir/admin.
   - **SharedPreferences** untuk menyimpan username/email kasir (dengan CheckBox "Ingat Username", jika dicentang disimpan, jika tidak dicentang dihapus).
5. **Dashboard & Operasional Kasir:**
   - Halaman **Dashboard** sebagai beranda sistem, memantau ringkasan pesanan masuk (Firestore PENDING), pesanan diproses (Firestore PREPARING), arsip selesai (SQLite), dan total omzet secara langsung.
6. **Search Bar Menu (AutoCompleteTextView):**
   - Di menu Kasir, `AutoCompleteTextView` difungsikan untuk pencarian nama menu makanan/minuman secara instan (sesuai Bab II Modul PM).
7. **QR-Code & Hardware:**
   - Generator QR Code pesanan (`BarcodeEncoder`) dan Scanner Kamera QR (`CompoundBarcodeView` / ZXing) untuk verifikasi pengambilan pesanan di outlet.
8. **Peta & GPS:**
   - Peta lokasi outlet dan pelacakan GPS kasir menggunakan `OSMdroid` + `FusedLocationProviderClient`.
9. **Kamera & Galeri + Supabase:**
   - Pengambilan foto menu dari Kamera/Galeri dan upload ke Supabase Storage.

---

## 📋 Status Pengerjaan (Progress):

### Fitur Aplikasi (SELESAI 100%):
- [x] Setup proyek di `/home/yoga/Data/Project/PesenHubJenggirat` (ViewBinding + DataBinding aktif).
- [x] `LoginActivity.kt` + `activity_login.xml`:
  - Firebase Authentication (`signInWithEmailAndPassword`, `createUserWithEmailAndPassword`).
  - SharedPreferences untuk opsi centang "Ingat Username".
- [x] `DashboardFragment.kt` + `fragment_dashboard.xml`:
  - Ringkasan realtime operasional outlet (Pesanan Masuk & Dimasak dari Firestore).
  - Total transaksi selesai & akumulasi omzet dari SQLite lokal.
  - Kartu profil kasir & tombol aksi cepat navigasi.
  - Fetch inspirasi outlet via **Volley HTTP Request** (Checklist #19).
- [x] `KasirFragment.kt` + `fragment_kasir.xml`:
  - Form POS Kasir terhubung realtime ke **Cloud Firestore**.
  - **`AutoCompleteTextView` untuk Search Item Menu** (rekomendasi otomatis nama menu & auto-fill harga).
  - `EditText` untuk data pelanggan & nomor HP.
  - Filter kategori menu via `Spinner`.
  - RadioButton bayar (Tunai / QRIS) & CheckBox topping/modifier.
  - **`TimePickerDialog` Estimasi Jam Siap Ambil** (Bab III Modul PM / Checklist #7).
  - Otomatis membuka `OrderDetailActivity` dan membunyikan **Audio Bel** (`SoundHelper`).
- [x] `AntrianFragment.kt` + `fragment_antrian.xml` + `AntrianAdapter.kt`:
  - KDS dapur realtime: status MASUK (PENDING) ➔ DIPROSES (PREPARING) ➔ SELESAI (COMPLETED).
  - `ContextMenu` (long press) dan `PopupMenu` (tombol titik tiga).
  - Otomatis menyimpan arsip ke SQLite saat status pesanan diubah ke `COMPLETED`.
  - Klik item membuka `OrderDetailActivity` & memutar audio sukses (`SoundHelper`).
- [x] `RiwayatFragment.kt` + `fragment_riwayat.xml` + `RiwayatAdapter.kt`:
  - Menampilkan riwayat transaksi selesai dari SQLite lokal (`DBOpenHelper`).
  - Fitur filter riwayat per tanggal menggunakan **`DatePickerDialog`** (Bab III Modul PM).
  - Rekap total transaksi & pendapatan bersih selesai.
- [x] `OrderDetailActivity.kt` + `activity_order_detail.xml`:
  - Tampilan detail struk pesanan.
  - **QR Code Generator** berbasis ZXing (`BarcodeEncoder` - Checklist #25).
- [x] `QRScanActivity.kt` + `activity_qr_scan.xml`:
  - **Scanner Kamera QR Code** berbasis ZXing (`CompoundBarcodeView` - Checklist #25).
  - Mencari pesanan di Firestore & tombol verifikasi pengambilan selesai ke SQLite.
- [x] `MapsActivity.kt` + `activity_maps.xml`:
  - **OpenStreetMap OSMdroid** (`MapView` & pin outlet martabak - Checklist #22).
  - **GPS Lokasi Pengguna** (`FusedLocationProviderClient` & kalkulasi jarak ke outlet - Checklist #21).
- [x] `UploadMenuActivity.kt` + `activity_upload_menu.xml`:
  - Ambil foto menu dari **Kamera** (`ActivityResultContracts.TakePicturePreview` - Checklist #20).
  - Pilih gambar menu dari **Galeri** (`ActivityResultContracts.GetContent` - Checklist #26).
  - Upload dan simpan aset ke **Supabase Storage** + Firestore (Checklist #33 & #30).
- [x] `PesenHubFCMService.kt`:
  - Background service **Firebase Cloud Messaging** & NotificationChannel status bar (Checklist #31).
- [x] `SoundHelper.kt`:
  - Efek audio bel lonceng pesanan & notifikasi (Checklist #24).
- [x] `MainActivity.kt` + `bottom_nav_menu.xml` + `main_menu.xml`:
  - 4 Menu Navigasi Bawah: **Dashboard**, **Kasir**, **Antrean**, **Riwayat**.
  - Toolbar OptionsMenu: Refresh, Foto Menu & Supabase, Peta Lokasi OSMdroid, Scan QR Kamera, Logout.
- [x] `web/index.html` & `web/app.js`:
  - Web Customer Ordering langsung terhubung ke Firestore yang sama (Checklist #27 & #28).

---

### Sisa Pekerjaan (Laporan Teknis & Presentasi UTS PML - Bobot 30%):
- [ ] Dokumen Teknis 10 Poin UTS PML (Latar Belakang, Manfaat, Masalah, Arsitektur, UML, Skema DB, UI, Hasil).
- [ ] Slide Presentasi Demo Aplikasi di Depan Kelas.
