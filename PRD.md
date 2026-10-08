# PRD — PesenHub Jenggirat (Mobile Android Kotlin)
**UTS Pemrograman Mobile Lanjut 2026/2027**  
**Afiliasi Proyek:** Ekstensi Mobile dari Proyek Teknologi Terintegrasi (PesenHub)  
**Target Platform:** Android (Native Kotlin) + Web Client (Terintegrasi Firestore)  
**Tim Pengembang:**
- Yoga Ananda Sabila Rizqi (Lead / Core Architecture & POS Kasir / Local Storage)
- Rydo (Co-developer / KDS Antrian / Auth & In-App Messaging)

---

## 1. Latar Belakang & Identitas Proyek
PesenHub Jenggirat adalah implementasi aplikasi mobile Android berbasis native Kotlin yang mengadaptasi dan mengembangkan fungsionalitas sistem **PesenHub (Outlet Order Management System)** yang sebelumnya dirancang pada mata kuliah Proyek Teknologi Terintegrasi (PTT). 

Pada versi PTT, sistem menggunakan arsitektur Flutter, Golang Backend, MySQL, dan WhatsApp Gateway. Untuk kebutuhan mata kuliah Pemrograman Mobile Lanjut (PML), sistem dikembangkan menjadi **aplikasi native Android menggunakan bahasa Kotlin** dengan integrasi langsung ke **Firebase Firestore, Firebase Authentication, Firebase Cloud Messaging (FCM), In-App Messaging, Supabase Storage, serta SQLite lokal**.

Gaya penulisan kode Kotlin dirancang secara ketat mengikuti **konvensi modul perkuliahan Pemrograman Mobile (PM) dan Pemrograman Mobile Lanjut (PML)**:
- ViewBinding (`lateinit var b: ActivityMainBinding`)
- Penamaan ID & referensi UI modul (`edt...`, `btn...`, `sp...`, `ls...`, `tx...`)
- Operasi database langsung & terstruktur (`DBOpenHelper : SQLiteOpenHelper`, rawQuery, execSQL)
- Firestore listener snapshot (`db.collection(...).addSnapshotListener`), `SimpleAdapter` / `ArrayAdapter`
- Integrasi GPS & OSMdroid (`AirLocation`, `MapView`, `OverlayItem`, `GpsMyLocationProvider`)
- QR Code Scanner & Generator (ZXing Embedded & BarcodeEncoder)

---

## 2. Analisis Permasalahan & Pemecahan Masalah
### 2.1 Permasalahan
1. Antrean pesanan martabak/terang bulan sering menumpuk dan kasir kesulitan mencatat pesanan kustom (topping, level rasa, varian).
2. Diperlukan pencatatan offline saat sinyal tidak stabil di outlet (butuh SQLite lokal).
3. Sinkronisasi data real-time antara kasir di outlet dan pesanan yang masuk dari aplikasi web pelanggan.
4. Notifikasi instan pesanan masuk ke bagian dapur/kasir dan tanda bukti pesanan digital berbasis QR code.

### 2.2 Pemecahan Masalah
1. Membangun modul POS (Kasir) interaktif dengan RadioButton (metode bayar), CheckBox (pilihan topping), AutoCompleteTextView (nama pelanggan), dan Spinner (kategori).
2. Menyediakan database ganda: **SQLite lokal (`DBOpenHelper`)** untuk cache/riwayat transaksi offline, dan **Firebase Firestore** untuk cloud synchronization.
3. Membangun Customer Web berbasis HTML/JS yang langsung terhubung ke Firestore yang sama sehingga pesanan web langsung muncul di Android secara real-time.
4. Menerapkan QR Code generator di struk/detail order dan scanner kamera untuk verifikasi pengambilan pesanan.
5. Memasang Firebase Cloud Messaging (FCM) dan In-App Messaging untuk banner promosi dan notifikasi status pesanan.

---

## 3. Matriks 33 Fitur Checklist Penilaian UTS PML (Bobot Total 110%)

| No | Fitur | Bobot | Penerapan di PesenHub Jenggirat | Penanggung Jawab |
|---|---|---|---|---|
| 1 | RadioButton | 1% | Pilihan metode bayar (Tunai / QRIS / Transfer) | Yoga |
| 2 | CheckBox | 1% | Pilihan modifier/topping menu martabak | Yoga |
| 3 | Button | 1% | Tombol navigasi, simpan pesanan, update status | Yoga & Rydo |
| 4 | EditText | 1% | Input nama pelanggan, harga, catatan pesanan | Yoga & Rydo |
| 5 | AutoCompleteTextView | 1% | Input nama pelanggan dengan saran data pelanggan | Yoga |
| 6 | DatePickerDialog | 1% | Filter laporan transaksi berdasarkan rentang tanggal | Yoga |
| 7 | TimePickerDialog | 1% | Pengaturan jam ketersediaan menu / jam operasional | Yoga |
| 8 | Spinner | 1% | Pilihan kategori menu (Martabak Manis, Telur, Minuman) | Yoga |
| 9 | ListView | 1% | Daftar pesanan aktif dan daftar menu | Yoga & Rydo |
| 10 | OptionsMenu | 1% | Menu toolbar atas (Refresh, Lokasi Outlet, Scan QR, Logout) | Yoga |
| 11 | ContextMenu | 1% | Long click item pesanan (Update Status, Batalkan) | Rydo |
| 12 | PopupMenu | 1% | Tombol aksi ⋮ per item antrean | Rydo |
| 13 | BottomNavigationView | 1% | Navigasi menu utama: Kasir, Antrean, Menu, Laporan | Yoga |
| 14 | FrameLayout | 1% | Container penampung Fragment utama | Yoga |
| 15 | Fragment | 1% | KasirFragment, AntreanFragment, MenuFragment, LaporanFragment | Yoga & Rydo |
| 16 | Activity | 1% | MainActivity, LoginActivity, OrderDetailActivity, MapsActivity, QRScanActivity | Yoga & Rydo |
| 17 | Database SQLite | 1% | `DBOpenHelper` lokal untuk cache offline pesanan & master | Yoga |
| 18 | Database MySQL & Web Service/API | 2% | Endpoint API Sinkronisasi / Web Service | Yoga |
| 19 | Pustaka Volley | 1% | Request HTTP GET/POST fetch status server & katalog | Yoga |
| 20 | Kamera | 1% | Foto produk menu baru langsung dari kamera perangkat | Yoga |
| 21 | GPS | 2% | Deteksi koordinat outlet & lokasi kasir saat buka shift | Rydo |
| 22 | Google Maps / OpenStreetMaps | 2% | OSMdroid (`MapView`, `OverlayItem`, `MyLocationNewOverlay`) | Rydo |
| 23 | SharedPreferences | 1% | Simpan sesi login user, branch active, nama kasir | Yoga |
| 24 | Audio/Video | 1% | Suara bel notifikasi pesanan masuk (MediaPlayer) | Rydo |
| 25 | QR-Code | 2% | Generator QR order id + Scanner QR pengambilan pesanan | Rydo |
| 26 | Gallery | 1% | Pemilihan gambar menu dari galeri smartphone | Yoga |
| 27 | Aplikasi Web (DB terintegrasi mobile) | 10% | Web Customer Order terhubung langsung Firestore | Yoga |
| 28 | Aplikasi Web (terintegrasi mobile) | 10% | Pemesanan di Web langsung realtime muncul di antrean Android | Yoga |
| 29 | Firebase Authentication | 6% | Login kasir & admin outlet (Email/Password & Google) | Rydo |
| 30 | Firebase Realtime / Firestore | 5% | Firestore CRUD pesanan, menu, cabang, status update | Yoga |
| 31 | Firebase Cloud Messaging (FCM) | 6% | Push Notification pesanan baru masuk ke Android | Rydo |
| 32 | Firebase In-App Messaging | 1% | Pop-up campaign pengumuman promo outlet | Rydo |
| 33 | Supabase | 7% | Supabase Storage untuk upload & hosting aset gambar menu | Yoga |
| 34 | Kelengkapan isi laporan teknis & presentasi | 30% | Dokumen teknis 10 poin + Slide presentasi | Yoga & Rydo |
| **TOTAL** | | **110%** | | |

---

## 4. Pembagian Kerja Tim (Work Breakdown Structure)

### Scope Pekerjaan Yoga Ananda (Lead & Core Architecture):
1. Inisialisasi struktur repositori `PesenHubJenggirat`, konfigurasi Gradle modular, dan arsitektur dasar.
2. Modul Kasir (POS): UI Form dengan `RadioButton`, `CheckBox`, `EditText`, `AutoCompleteTextView`, `Spinner`, `Button`.
3. Modul SQLite Database: implementasi `DBOpenHelper : SQLiteOpenHelper`, tabel offline orders, rawQuery & execSQL.
4. Integrasi Firestore Core: CRUD Menu & Orders, sync snapshot listener.
5. Modul Laporan & Waktu: `DatePickerDialog`, `TimePickerDialog`, rekap penjualan.
6. Modul Kamera & Galeri + Supabase Storage upload gambar menu.
7. Modul Web Customer (HTML/JS + Firestore) & integrasi Volley HTTP.

### Scope Pekerjaan Rydo (Antrean, Auth, Hardware & Firebase Messaging):
1. Modul Antrean (KDS): Tampilan `ListView`, `ContextMenu` (long click order), `PopupMenu` (status order).
2. Modul Firebase Authentication: Form Login/Register, validasi akun, integrasi dengan `SharedPreferences`.
3. Modul QR Code: Generate QR Code pesanan (ZXing) dan QR Scanner verifikasi pengambilan order.
4. Modul Lokasi & Peta: Integrasi GPS (`AirLocation` / FusedLocation) dan OSMdroid (`MapView`, `OverlayItem`, `MyLocationNewOverlay`).
5. Modul Notifikasi & Suara: Firebase Cloud Messaging (`FirebaseMessagingService`), In-App Messaging, dan `MediaPlayer` suara pesanan masuk.
6. Penyusunan slide presentasi dan pendampingan dokumen teknis.

---

## 5. Arsitektur Teknis & Skema Database
### 5.1 Skema SQLite Lokal (`DBOpenHelper`)
- Database Name: `pesenhub_local.db`
- Versi: 1
- Tabel `pesanan_offline`:
  - `id_pesanan` TEXT PRIMARY KEY
  - `nama_pelanggan` TEXT NOT NULL
  - `hp_pelanggan` TEXT
  - `detail_item` TEXT
  - `metode_bayar` TEXT
  - `total_bayar` INTEGER
  - `status_order` TEXT
  - `waktu_dibuat` TEXT
  - `is_synced` INTEGER DEFAULT 0

### 5.2 Skema Cloud Firestore
- Koleksi `/orders/{orderId}`:
  - `orderNumber`: string ("ORD-20261008-001")
  - `customerName`: string
  - `customerPhone`: string
  - `source`: string ("CASHIER" / "WEB")
  - `status`: string ("PENDING" / "PREPARING" / "READY" / "COMPLETED" / "CANCELLED")
  - `paymentMethod`: string ("CASH" / "QRIS")
  - `total`: number
  - `notes`: string
  - `createdAt`: timestamp
- Koleksi `/menus/{menuId}`:
  - `name`: string
  - `category`: string
  - `price`: number
  - `imageUrl`: string
  - `available`: boolean
- Koleksi `/outlets/{outletId}`:
  - `name`: string
  - `lat`: number
  - `lng`: number
  - `address`: string

---

## 6. Standar Penulisan Kode (Modul Style)
Semua file Kotlin wajib mengikuti pola modul dosen:
1. ViewBinding variabel ringkas: `lateinit var b: ActivityMainBinding` / `lateinit var b: FragmentKasirBinding`.
2. Listener View.OnClickListener di class atau lambda langsung `b.btnSimpan.setOnClickListener { ... }`.
3. SQLite menggunakan pendekatan `DBOpenHelper` dengan `db.execSQL(...)` dan `db.rawQuery(...)` sesuai Bab 08 PM.
4. Firestore menggunakan `db = FirebaseFirestore.getInstance()` dan `db.collection(...).addSnapshotListener` sesuai Bab 02-03 PML.
5. Peta menggunakan OSMdroid dan GPS sesuai Bab 13 PM.
