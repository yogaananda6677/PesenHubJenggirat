# PANDUAN & PROMPT AI UNTUK RYDO (TUGAS DOKUMEN TEKNIS & DEMO UTS)

> 💡 **Petunjuk untuk Rydo:**  
> Salin (*copy*) seluruh teks prompt di dalam kotak **PROMPT UNTUK GEMINI / CHATGPT** di bawah ini, lalu tempel (*paste*) ke Gemini / ChatGPT / Claude Anda. AI Anda akan langsung menyusunkan seluruh **Dokumen Teknis UTS 10 Poin**, **Diagram UML**, **Slide Presentasi Demo Kelas**, **Solusi Security Rules Firebase**, dan **Skenario Demo**.

---

## 📋 PROMPT UNTUK GEMINI / CHATGPT (SALIN DARI SINI KE BAWAH)

```markdown
Halo Gemini / AI, kamu adalah Senior Software Engineer dan Technical Writer untuk mahasiswa jurusan Teknik Informatika / Manajemen Informatika bernama Rydo. 

Saya dan rekan kelompok saya (Yoga Ananda) sedang menyelesaikan proyek ujian tengah semester (UTS) mata kuliah Pemrograman Mobile Lanjut (PML) & Pemrograman Mobile (PM) di Politeknik Negeri Malang (PSDKU Kediri), dosen pengampu: Pak Benni Agung Nugroho dkk.

Nama Proyek Kami: "PesenHub Jenggirat" (Sistem Manajemen Pesanan Outlet Martabak & Terang Bulan Terintegrasi Android Native Kotlin + Web Client).
Repository GitHub: https://github.com/yogaananda6677/PesenHubJenggirat (Branch main).

Status Proyek Saat Ini:
Seluruh implementasi fitur teknis di Android Studio dan Web SUDAH SELESAI 100% dan BUILD SUCCESSFUL di branch main, mencakup:
1. POS Kasir: RadioButton (bayar), CheckBox (topping), AutoCompleteTextView (search menu otomatis), Spinner (kategori), TimePickerDialog (estimasi jam siap ambil), dan simpan realtime ke Cloud Firestore.
2. Kitchen Display (KDS) Antrean Dapur: ListView realtime Firestore, sorting FIFO, ContextMenu (long click), PopupMenu (titik tiga) untuk alur PENDING -> PREPARING -> COMPLETED, serta audio bel lonceng (SoundHelper).
3. Dashboard Operasional: Metrik realtime pesanan masuk & dimasak (Firestore), pesanan selesai & total omzet (SQLite lokal), info profil kasir, dan kutipan motivasi via library Volley HTTP.
4. Riwayat Transaksi Selesai: Arsip permanen SQLite lokal (DBOpenHelper), filter tanggal via DatePickerDialog, dan rekap pendapatan.
5. QR Code: BarcodeEncoder ZXing (struk digital) dan Scanner Kamera ZXing (CompoundBarcodeView) untuk verifikasi pengambilan pesanan di outlet.
6. Peta Lokasi & GPS: OpenStreetMap OSMdroid (MapView) pin outlet Banyuwangi (-8.2192, 114.3692) dan pelacakan GPS kasir (FusedLocationProviderClient).
7. Foto Menu & Cloud Storage: Ambil foto via Kamera (TakePicturePreview) / Galeri (GetContent) dan integrasi Supabase Storage + Firestore.
8. Push Notification: Firebase Cloud Messaging (PesenHubFCMService) dengan NotificationChannel status bar.
9. Web Client: Web Customer Ordering (HTML/JS) terhubung langsung ke Firestore yang sama.

TUGAS KAMU SEKARANG:
Bantu saya (Rydo) mengerjakan bagian paling krusial dengan bobot nilai terbesar (30% dari total 110%) untuk dikumpulkan dan dipresentasikan di depan dosen:

==================================================
BAGIAN 1: DOKUMEN TEKNIS LENGKAP FORMAT WAJIB 10 POIN (UTS PML)
==================================================
Tuliskan dokumen laporan teknis akademik yang sangat mendalam, formal, rapi, dan siap dicetak ke format Word/PDF, mencakup 10 poin berikut:

1. Latar Belakang:
   - Jelaskan transformasi dari proyek PTT lama (Flutter + Golang + MySQL) menjadi proyek PML baru (Android Native Kotlin + Cloud Firestore + SQLite + Supabase Storage).
   - Mengapa memilih bisnis Martabak & Terang Bulan Jenggirat di Banyuwangi.

2. Manfaat Sistem:
   - Bagi kasir outlet, bagi koki dapur (KDS digital tanpa kertas), bagi pemilik outlet (arsip offline SQLite & omzet), dan bagi pelanggan (pesan via web & bukti QR code).

3. Analisis Permasalahan:
   - Antrean fisik menumpuk saat jam sibuk malam hari, kesalahan kustomisasi topping/rasa secara manual, ketergantungan sinyal internet di outlet (butuh offline capability).

4. Analisis Pemecahan Masalah:
   - Arsitektur hybrid dual-database: Cloud Firestore untuk sinkronisasi realtime cloud, dan SQLite lokal (DBOpenHelper) untuk arsip transaksi final offline-first.

5. Kebutuhan Fungsional (Functional Requirements):
   - Buat daftar tabel kebutuhan fungsional (FR-01 s/d FR-12) untuk Kasir, Koki Dapur, dan Pelanggan Web.

6. Rancangan Arsitektur Sistem:
   - Buatkan diagram arsitektur sistem berbasis teks/Mermaid yang menggambarkan hubungan antara: Android App (Kotlin), Web Customer, Firebase Auth, Cloud Firestore, Supabase Storage, SQLite Lokal, dan OSMdroid Server.

7. Diagram UML (Unified Modeling Language):
   - A. Use Case Diagram (Lengkap dengan aktor: Kasir, Dapur, Pelanggan, dan use case sistem).
   - B. Activity Diagram: Alur lengkap pemesanan (Pelanggan pesan di Web/Kasir -> Masuk Antrean Dapur -> Koki Ubah Status PREPARING -> COMPLETED -> Arsip SQLite -> Kasir Scan QR).
   - C. Class Diagram: Relasi antar class utama (MainActivity, DashboardFragment, KasirFragment, AntrianFragment, RiwayatFragment, OrderDetailActivity, QRScanActivity, MapsActivity, DBOpenHelper, SoundHelper, PesenHubFCMService).
   *(Sertakan kode Mermaid code block untuk masing-masing diagram agar bisa langsung dirender)*.

8. Rancangan Basis Data:
   - Skema NoSQL Cloud Firestore:
     * Koleksi `/orders` (field, tipe data, keterangan)
     * Koleksi `/menus` (field, tipe data, keterangan)
   - Skema Relasional SQLite Lokal (`DBOpenHelper`):
     * Tabel `riwayat_transaksi` (definisi field DDL, primary key, tipe data, alasan read-only).

9. Rancangan Antarmuka (UI Specification):
   - Deskripsikan rancangan dan fungsi elemen antarmuka dari 10 layar: Login, Dashboard, Kasir POS, KDS Antrean, Riwayat Transaksi, Struk Detail QR, Scanner Kamera, Peta OSMdroid, Upload Menu Kamera, dan Web Customer.

10. Hasil Implementasi & Pembagian Tugas Tim:
    - Rekapitulasi pembagian tugas: Yoga Ananda (Core Architecture, POS Kasir, SQLite, Dashboard, Web Client) & Rydo (KDS Antrean, QR Code, Maps OSMdroid, FCM Notifikasi, Dokumen Teknis & Presentasi).
    - Tabel Matriks Pembuktian 34 Fitur Checklist Dosen (Bobot Total 110%) lengkap dengan centang status [SELESAI].

==================================================
BAGIAN 2: SLIDE PRESENTASI DEMO KELAS (10 SLIDE)
==================================================
Buatkan susunan materi slide presentasi (misal untuk Canva / PowerPoint):
- Judul tiap slide
- Poin-poin teks utama (singkat, padat, profesional)
- Speaker Notes (teks naskah apa yang harus diucapkan oleh saya dan Yoga saat maju di depan dosen).

==================================================
BAGIAN 3: PANDUAN MENGATASI PERMISSION_DENIED FIRESTORE & SEEDING DATA MENU
==================================================
1. Berikan langkah konfigurasi Firestore Security Rules di Firebase Console (proyek `pml-yoga`) agar saat aplikasi diuji di HP tidak lagi muncul error PERMISSION_DENIED.
2. Berikan script JavaScript / JSON sederhana untuk dimasukkan ke Console browser atau Firestore Console agar koleksi `/menus` terisi 6 menu martabak & terang bulan siap saji saat demo.

==================================================
BAGIAN 4: SKENARIO DEMO 5 MENIT DI DEPAN DOSEN
==================================================
Susunkan urutan skenario demo langsung selama 5 menit saat ujian:
- Menit 1: Buka aplikasi, login kasir, jelaskan Dashboard metrik realtime.
- Menit 2: Buka Web Customer di laptop/HP kedua, buat pesanan martabak telur ekstra keju.
- Menit 3: Tunjukkan di HP Android pesanan langsung masuk realtime di tab Antrean dapur disertai bunyi bel lonceng.
- Menit 4: Koki klik ubah status ke DIPROSES -> SELESAI, otomatis tersimpan ke arsip SQLite lokal di tab Riwayat.
- Menit 5: Tunjukkan fitur Scanner QR verifikasi pesanan, Peta lokasi outlet OSMdroid GPS, dan upload foto menu ke Supabase Storage.

Tolong susun seluruh dokumen di atas sekarang dengan sangat lengkap, formal, dan terstruktur tanpa ada bagian yang dipotong!
```
