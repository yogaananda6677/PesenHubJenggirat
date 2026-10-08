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

---

## 📋 Status Pengerjaan (Progress):

### Milik Yoga (Selesai):
- [x] Setup proyek di `/home/yoga/Data/Project/PesenHubJenggirat` (ViewBinding + DataBinding aktif).
- [x] `LoginActivity.kt` + `activity_login.xml`:
  - Firebase Authentication (`signInWithEmailAndPassword`, `createUserWithEmailAndPassword`).
  - SharedPreferences untuk opsi centang "Ingat Username".
- [x] `DashboardFragment.kt` + `fragment_dashboard.xml`:
  - Ringkasan realtime operasional outlet (Pesanan Masuk & Dimasak dari Firestore).
  - Total transaksi selesai & akumulasi omzet dari SQLite lokal.
  - Kartu profil kasir & tombol aksi cepat navigasi.
- [x] `KasirFragment.kt` + `fragment_kasir.xml`:
  - Form POS Kasir terhubung realtime ke **Cloud Firestore**.
  - **`AutoCompleteTextView` untuk Search Item Menu** (rekomendasi otomatis nama menu & auto-fill harga).
  - `EditText` untuk data pelanggan & nomor HP.
  - Filter kategori menu via `Spinner`.
  - RadioButton bayar (Tunai / QRIS) & CheckBox topping/modifier.
- [x] `AntrianFragment.kt` + `fragment_antrian.xml` + `AntrianAdapter.kt`:
  - KDS dapur realtime: status MASUK (PENDING) ➔ DIPROSES (PREPARING) ➔ SELESAI (COMPLETED).
  - `ContextMenu` (long press) dan `PopupMenu` (tombol titik tiga).
  - Otomatis menyimpan arsip ke SQLite saat status pesanan diubah ke `COMPLETED`.
- [x] `RiwayatFragment.kt` + `fragment_riwayat.xml` + `RiwayatAdapter.kt`:
  - Menampilkan riwayat transaksi selesai dari SQLite lokal (`DBOpenHelper`).
  - Fitur filter riwayat per tanggal menggunakan **`DatePickerDialog`** (Bab III Modul PM).
  - Rekap total transaksi & pendapatan bersih selesai.
- [x] `MainActivity.kt` + `bottom_nav_menu.xml`:
  - 4 Menu Navigasi Bawah: **Dashboard**, **Kasir**, **Antrean**, **Riwayat**.
- [x] `web/index.html` & `web/app.js`:
  - Web Customer Ordering langsung terhubung ke Firestore yang sama.

### Milik Rydo (Siap Dikerjakan dengan Panduan & Prompt AI):
- [ ] Issue #10: Modul QR Code Generator & Scanner kamera pengambilan pesanan (`ZXing`).
- [ ] Issue #11: Peta Lokasi Outlet GPS & OSMdroid (`MapView`, `OverlayItem`).
- [ ] Issue #12: Firebase Cloud Messaging (FCM), In-App Messaging & Audio Alert (`MediaPlayer`).
- [ ] Issue #13: Dokumen teknis dan slide presentasi UTS.
