package com.pesenhub.jenggirat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pesenhub.jenggirat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var b: ActivityMainBinding
    lateinit var auth: FirebaseAuth
    private var userRole: String = "kasir"
    private var userNama: String = ""
    private var namaOutlet: String = "Jenggirat Kediri"

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

        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(b.toolbar)

        // Baca sesi role & profil pengguna
        muatSesiPengguna()

        // Default tampilkan DashboardFragment saat pertama kali dibuka
        if (savedInstanceState == null) {
            b.bottomNav.selectedItemId = R.id.nav_dashboard
            gantiFragment(DashboardFragment())
        }

        // Listener BottomNavigationView (Bab VI Modul PM Pak Benni)
        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    gantiFragment(DashboardFragment())
                    true
                }
                R.id.nav_kasir -> {
                    gantiFragment(KasirFragment())
                    true
                }
                R.id.nav_antrian -> {
                    gantiFragment(AntrianFragment())
                    true
                }
                R.id.nav_laporan -> {
                    gantiFragment(RiwayatFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun muatSesiPengguna() {
        val pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userRole = pref.getString("role", "kasir") ?: "kasir"
        userNama = pref.getString("nama", "") ?: ""
        namaOutlet = "Jenggirat Kediri"

        perbaruiToolbarSubtitle()

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

                    perbaruiToolbarSubtitle()
                    invalidateOptionsMenu()
                }
            }
    }

    private fun perbaruiToolbarSubtitle() {
        supportActionBar?.title = "Jenggirat Kediri"
        supportActionBar?.subtitle = if (userRole == "admin") "Admin" else "Kasir"
    }

    fun getUserRole(): String = userRole

    fun navigasiKeTab(menuId: Int) {
        b.bottomNav.selectedItemId = menuId
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
            R.id.action_upload_menu -> {
                val intent = Intent(this, UploadMenuActivity::class.java)
                startActivity(intent)
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

        btnSalin.setOnClickListener {
            val kode = txKodeHasil.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Kode Undangan Kasir", kode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Kode $kode disalin ke clipboard!", Toast.LENGTH_SHORT).show()
        }

        btnBagikan.setOnClickListener {
            val kode = txKodeHasil.text.toString()
            val namaKasir = edNamaKasir.text.toString().trim()
            val pesan = "Halo $namaKasir! Anda diundang menjadi Kasir di $namaOutlet. Silakan unduh/buka aplikasi PesenHub Jenggirat, pilih 'Daftar Kasir (Diundang)', dan masukkan Kode Undangan: *$kode*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, pesan)
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Kode Undangan Kasir"))
        }

        dialog.show()
    }
}
