package com.pesenhub.jenggirat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pesenhub.jenggirat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var b: ActivityMainBinding
    lateinit var auth: FirebaseAuth
    private var userRole: String = "kasir"
    private var userNama: String = ""
    private var namaOutlet: String = "Jenggirat Kediri"
    private var currentTabTitle: String = "Dashboard"

    private var webOrderListener: ListenerRegistration? = null
    private var isFirstOrderSnapshot: Boolean = true

    // Launcher untuk izin push notification di Android 13+ (API 33+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Izin notifikasi diberikan
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek Sesi Firebase Auth: Jika belum login, arahkan ke LoginActivity
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Selaraskan status bar dengan Toolbar (Warna oranye primer menyatu, ikon status bar terang)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            b.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setSupportActionBar(b.toolbar)

        // Setup channel notifikasi & runtime permission
        buatNotificationChannel()
        mintaIzinNotifikasi()

        // Baca sesi role & profil pengguna
        muatSesiPengguna()

        // Listener BottomNavigationView (Bab VI Modul PM Pak Benni)
        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    gantiFragment(DashboardFragment())
                    perbaruiToolbar("Dashboard")
                    true
                }
                R.id.nav_kasir -> {
                    gantiFragment(KasirFragment())
                    perbaruiToolbar("Kasir POS")
                    true
                }
                R.id.nav_antrian -> {
                    gantiFragment(AntrianFragment())
                    perbaruiToolbar("Antrean Pesanan")
                    true
                }
                R.id.nav_laporan -> {
                    gantiFragment(RiwayatFragment())
                    perbaruiToolbar("Riwayat Transaksi")
                    true
                }
                R.id.nav_akun -> {
                    gantiFragment(AkunFragment())
                    perbaruiToolbar("Profil Akun")
                    true
                }
                else -> false
            }
        }

        // Tampilkan tab awal saat pertama kali dibuka
        if (savedInstanceState == null) {
            val targetTab = intent?.getIntExtra("EXTRA_NAV_TAB", R.id.nav_dashboard) ?: R.id.nav_dashboard
            navigasiKeTab(targetTab)
        }

        // Mulai pantau pesanan web secara realtime untuk notifikasi ganda
        mulaiPantauPesananWeb()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetTab = intent.getIntExtra("EXTRA_NAV_TAB", 0)
        if (targetTab != 0) {
            navigasiKeTab(targetTab)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webOrderListener?.remove()
    }

    private fun mintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun buatNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                "pesenhub_orders_channel_v3",
                "Pesanan Pelanggan Web",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pesanan baru masuk dari website pelanggan"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun mulaiPantauPesananWeb() {
        isFirstOrderSnapshot = true
        webOrderListener?.remove()

        webOrderListener = FirebaseFirestore.getInstance().collection("orders")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                // Abaikan batch pertama saat inisialisasi agar pesanan lama tidak memicu notifikasi ulang
                if (isFirstOrderSnapshot) {
                    isFirstOrderSnapshot = false
                    return@addSnapshotListener
                }

                for (dc in snapshots.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val doc = dc.document
                        val source = doc.getString("source") ?: ""
                        val orderNumber = doc.getString("orderNumber") ?: doc.id
                        val isFromWeb = source == "CUSTOMER_WEB" || orderNumber.startsWith("ORD-WEB")

                        if (isFromWeb) {
                            val custName = doc.getString("customerName") ?: "Pelanggan Web"
                            val total = doc.getLong("total") ?: doc.getLong("totalPrice") ?: 0L
                            val detailItem = doc.getString("detailItem") ?: doc.getString("menuItem") ?: "Pesanan Baru Masuk"

                            // 1. Bunyikan Bel Notifikasi (ToneGenerator)
                            SoundHelper.playBell()

                            // 2. Notifikasi di HP Sendiri (Status Bar Heads-up & Vibration)
                            tampilkanNotifikasiStatusBar(orderNumber, custName, total, detailItem)

                            // 3. Notifikasi di Dalam Aplikasi (In-App Dialog Modern)
                            tampilkanNotifikasiInApp(orderNumber, custName, total, detailItem)
                        }
                    }
                }
            }
    }

    private fun formatRupiah(nominal: Long): String {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(nominal)
    }

    private fun tampilkanNotifikasiStatusBar(
        orderNumber: String,
        customerName: String,
        total: Long,
        detailItem: String
    ) {
        val totalFormatted = formatRupiah(total)
        val title = "🔔 Pesanan Baru dari Web ($orderNumber)"
        val body = "$customerName: $detailItem • $totalFormatted"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAV_TAB", R.id.nav_antrian)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            orderNumber.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, "pesenhub_orders_channel_v3")
            .setSmallIcon(R.drawable.ic_stat_order)
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nSentuh notifikasi ini untuk membuka daftar antrean."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(orderNumber.hashCode(), notification)
    }

    private fun tampilkanNotifikasiInApp(
        orderNumber: String,
        customerName: String,
        total: Long,
        detailItem: String
    ) {
        if (isFinishing || isDestroyed) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_pesanan_baru_web, null)
        val txOrderNumber = dialogView.findViewById<TextView>(R.id.txDialogOrderNumber)
        val txCustomerName = dialogView.findViewById<TextView>(R.id.txDialogCustomerName)
        val txItems = dialogView.findViewById<TextView>(R.id.txDialogItems)
        val txTotal = dialogView.findViewById<TextView>(R.id.txDialogTotal)
        val btnTutup = dialogView.findViewById<View>(R.id.btnDialogTutup)
        val btnBukaAntrian = dialogView.findViewById<View>(R.id.btnDialogBukaAntrian)

        txOrderNumber.text = "#$orderNumber"
        txCustomerName.text = customerName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        txItems.text = detailItem
        txTotal.text = formatRupiah(total)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnTutup.setOnClickListener {
            dialog.dismiss()
        }

        btnBukaAntrian.setOnClickListener {
            dialog.dismiss()
            navigasiKeTab(R.id.nav_antrian)
        }

        dialog.show()
    }

    private fun muatSesiPengguna() {
        val pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userRole = pref.getString("role", "kasir") ?: "kasir"
        userNama = pref.getString("nama", "") ?: ""
        namaOutlet = "Jenggirat Kediri"

        perbaruiToolbar()

        // Sinkronisasi data user terbaru dari Firestore
        val uid = auth.currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    userRole = doc.getString("role") ?: userRole
                    userNama = doc.getString("nama") ?: userNama
                    namaOutlet = "Jenggirat Kediri"

                    pref.edit()
                        .putString("role", userRole)
                        .putString("nama", userNama)
                        .putString("namaOutlet", namaOutlet)
                        .apply()

                    perbaruiToolbar()
                    invalidateOptionsMenu()
                }
            }
    }

    fun perbaruiToolbar(judul: String? = null) {
        if (judul != null) {
            currentTabTitle = judul
        }
        supportActionBar?.title = currentTabTitle
        val roleLabel = if (userRole == "admin") "Admin" else "Kasir"
        // Subtitle ringkas dan proporsional agar tidak terpotong di layar HP
        supportActionBar?.subtitle = "$namaOutlet • $roleLabel"
    }

    fun perbaruiToolbarSubtitle() {
        perbaruiToolbar()
    }

    fun getUserRole(): String = userRole

    fun navigasiKeTab(menuId: Int) {
        b.bottomNav.selectedItemId = menuId
        when (menuId) {
            R.id.nav_dashboard -> {
                gantiFragment(DashboardFragment())
                perbaruiToolbar("Dashboard")
            }
            R.id.nav_kasir -> {
                gantiFragment(KasirFragment())
                perbaruiToolbar("Kasir POS")
            }
            R.id.nav_antrian -> {
                gantiFragment(AntrianFragment())
                perbaruiToolbar("Antrean Pesanan")
            }
            R.id.nav_laporan -> {
                gantiFragment(RiwayatFragment())
                perbaruiToolbar("Riwayat Transaksi")
            }
            R.id.nav_akun -> {
                gantiFragment(AkunFragment())
                perbaruiToolbar("Profil Akun")
            }
        }
    }

    private fun gantiFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.flContainer, fragment)
            .commit()
    }

    // a method to create OptionsMenu (Bab V Modul PM Pak Benni)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val mnuInflater = menuInflater
        mnuInflater.inflate(R.menu.main_menu, menu)

        // Hanya tampilkan opsi 'Undang Kasir Baru' dan 'Kelola Menu' jika pengguna adalah Admin
        val itemUndang = menu?.findItem(R.id.action_invite_kasir)
        itemUndang?.isVisible = (userRole == "admin")

        val itemKelolaMenu = menu?.findItem(R.id.action_kelola_menu)
        itemKelolaMenu?.isVisible = (userRole == "admin")

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                Toast.makeText(this, "Data Firestore tersinkron otomatis secara realtime", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_kelola_menu -> {
                val intent = Intent(this, KelolaMenuActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_invite_kasir -> {
                tampilkanDialogUndangKasir()
                true
            }
            R.id.action_maps -> {
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_qr_scan -> {
                val intent = Intent(this, QRScanActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit().clear().apply()
                auth.signOut()
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun tampilkanDialogUndangKasir() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_undang_kasir, null)
        val edNamaKasir = dialogView.findViewById<EditText>(R.id.edDialogNamaKasir)
        val edEmailKasir = dialogView.findViewById<EditText>(R.id.edDialogEmailKasir)
        val btnGenerate = dialogView.findViewById<View>(R.id.btnGenerateKode)
        val layoutHasil = dialogView.findViewById<View>(R.id.layoutHasilKode)
        val txKodeHasil = dialogView.findViewById<TextView>(R.id.txKodeHasil)
        val btnKirimEmail = dialogView.findViewById<View>(R.id.btnKirimEmail)
        val btnSalin = dialogView.findViewById<View>(R.id.btnSalinKode)
        val btnBagikan = dialogView.findViewById<View>(R.id.btnBagikanKode)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Tutup", null)
            .create()

        btnGenerate.setOnClickListener {
            val namaKasir = edNamaKasir.text.toString().trim()
            val emailKasir = edEmailKasir.text.toString().trim()

            if (namaKasir.isEmpty()) {
                edNamaKasir.error = "Nama kasir wajib diisi"
                return@setOnClickListener
            }

            val kodeBaru = "JGR-" + (1000..9999).random()
            val dataUndangan = hashMapOf(
                "kodeUndangan" to kodeBaru,
                "namaKasir" to namaKasir,
                "emailKasir" to emailKasir.lowercase(),
                "namaOutlet" to namaOutlet,
                "adminEmail" to (auth.currentUser?.email ?: ""),
                "adminUid" to (auth.currentUser?.uid ?: ""),
                "status" to "PENDING",
                "createdAt" to Timestamp.now()
            )

            FirebaseFirestore.getInstance().collection("invitations").document(kodeBaru).set(dataUndangan)
                .addOnSuccessListener {
                    layoutHasil.visibility = View.VISIBLE
                    txKodeHasil.text = kodeBaru
                    Toast.makeText(this, "Kode undangan kasir berhasil dibuat: $kodeBaru", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal membuat kode undangan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnKirimEmail.setOnClickListener {
            val kode = txKodeHasil.text.toString()
            val namaKasir = edNamaKasir.text.toString().trim().ifEmpty { "Rekan Kasir" }
            val emailKasir = edEmailKasir.text.toString().trim()
            val adminEmail = auth.currentUser?.email ?: "Admin Outlet"

            if (emailKasir.isEmpty()) {
                edEmailKasir.error = "Email kasir wajib diisi untuk mengirim undangan"
                Toast.makeText(this, "Masukkan Email Kasir terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val subject = "[PesenHub Jenggirat] Undangan Bergabung sebagai Staf Kasir - $namaOutlet"
            val body = """
                Halo $namaKasir,

                Anda telah diundang oleh Admin ($adminEmail) untuk bergabung sebagai Staf Kasir resmi di outlet:
                Outlet : $namaOutlet
                Role   : Kasir (Cashier POS)

                Berikut detail kredensial dan kode aktivasi Anda:
                =================================================
                KODE UNDANGAN : $kode
                EMAIL TUJUAN  : $emailKasir
                STATUS        : PENDING (Siap diaktivasi)
                =================================================

                Panduan Aktivasi Akun Kasir:
                1. Buka aplikasi PesenHub Jenggirat di smartphone Android Anda.
                2. Pada halaman Login, klik 'Belum punya akun? Daftar sekarang'.
                3. Pilih tipe akun 'Kasir (Memerlukan Undangan)'.
                4. Lengkapi Nama Lengkap Anda, Email ($emailKasir), buat Password, dan masukkan Kode Undangan di atas ($kode).
                5. Klik 'DAFTAR SEBAGAI KASIR'.
                6. Selesai! Anda dapat langsung login dan melayani transaksi kasir di $namaOutlet.

                Jika ada kendala aktivasi, silakan hubungi Admin Outlet melalui email: $adminEmail.

                Salam hangat,
                Manajemen $namaOutlet
                PesenHub Jenggirat System
            """.trimIndent()

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$emailKasir")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailKasir))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            try {
                startActivity(Intent.createChooser(emailIntent, "Kirim Undangan Kasir via Email"))
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak ada aplikasi email yang terpasang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnSalin.setOnClickListener {
            val kode = txKodeHasil.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Kode Undangan Kasir", kode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Kode $kode disalin ke clipboard!", Toast.LENGTH_SHORT).show()
        }

        btnBagikan.setOnClickListener {
            val kode = txKodeHasil.text.toString()
            val namaKasir = edNamaKasir.text.toString().trim().ifEmpty { "Rekan Kasir" }
            val emailKasir = edEmailKasir.text.toString().trim()
            val emailInfo = if (emailKasir.isNotEmpty()) "\n📧 *Email Terdaftar:* $emailKasir" else ""
            val pesan = """
                *UNDANGAN RESMI KASIR - PESENHUB JENGGIRAT* ☕🍽️

                Halo *$namaKasir*,
                Anda telah diundang oleh Admin untuk bergabung sebagai *Staf Kasir* di outlet:
                🏪 *Outlet:* $namaOutlet
                👤 *Role:* Kasir$emailInfo

                🔑 *KODE UNDANGAN:* *$kode*

                *Langkah Aktivasi Akun:*
                1. Buka aplikasi *PesenHub Jenggirat*.
                2. Di halaman Login, klik *Daftar Sekarang*.
                3. Pilih tipe akun *Kasir (Memerlukan Undangan)*.
                4. Masukkan nama, email ($emailKasir), password, serta Kode Undangan di atas (*$kode*).
                5. Selesai! Anda langsung terhubung dan siap melayani transaksi di outlet.

                _Harap simpan kode undangan ini dengan baik._
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, pesan)
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Undangan Kasir via Chat"))
        }

        dialog.show()
    }
}
