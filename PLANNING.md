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
5. **Alur Web & QR Code (Opsi Rencana Lanjutan):**
   - Pelanggan memesan via Web Customer Ordering (tersimpan ke Firestore).
   - QR Code dibuat berdasarkan `orderId`. Pelanggan dapat menunjukkan QR code tersebut di outlet saat mengambil pesanan, dan kasir melakukan scan verifikasi. Aset QR code juga dapat diarsipkan ke Supabase Storage (opsi fase berikutnya).

---

## 📋 Status Pengerjaan (Progress):

### Milik Yoga (Selesai):
- [x] Setup proyek di `/home/yoga/Data/Project/PesenHubJenggirat` (ViewBinding + DataBinding aktif).
- [x] `LoginActivity.kt` + `activity_login.xml`:
  - Firebase Authentication (`signInWithEmailAndPassword`, `createUserWithEmailAndPassword`).
  - SharedPreferences untuk opsi centang "Ingat Username".
- [x] `KasirFragment.kt` + `fragment_kasir.xml`:
  - Data pesanan dan katalog menu langsung tersinkronisasi realtime ke **Cloud Firestore**.
  - Form POS lengkap (RadioButton bayar, CheckBox topping, AutoComplete pelanggan, Spinner kategori).
- [x] `DBOpenHelper.kt`:
  - Tabel `riwayat_transaksi` untuk mengarsipkan pesanan yang sudah selesai (read-only history).
- [x] `web/index.html` & `web/app.js`:
  - Web Customer Ordering langsung terhubung ke Firestore.

### Milik Rydo (Siap Dikerjakan dengan Panduan & Prompt AI):
- [ ] Issue #8: KDS Antrean Pesanan Dapur (`AntrianFragment`, `ListView`, `ContextMenu`, `PopupMenu`).
- [ ] Issue #10: Modul QR Code Generator & Scanner kamera pengambilan pesanan.
- [ ] Issue #11: Peta Lokasi Outlet GPS & OSMdroid (`MapView`, `OverlayItem`).
- [ ] Issue #12: Firebase Cloud Messaging (FCM), In-App Messaging & Audio Alert (`MediaPlayer`).
- [ ] Issue #13: Dokumen teknis dan slide presentasi UTS.
