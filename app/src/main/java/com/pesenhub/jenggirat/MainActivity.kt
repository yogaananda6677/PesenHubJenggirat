package com.pesenhub.jenggirat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.pesenhub.jenggirat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var b: ActivityMainBinding
    lateinit var auth: FirebaseAuth

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
        supportActionBar?.subtitle = "Kasir: ${auth.currentUser?.email}"

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

    fun navigasiKeTab(menuId: Int) {
        b.bottomNav.selectedItemId = menuId
    }

    private fun gantiFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.flContainer, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                Toast.makeText(this, "Data Firestore tersinkron otomatis secara realtime", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_maps -> {
                Toast.makeText(this, "Membuka Lokasi Outlet OSMdroid (Task Rydo)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_qr_scan -> {
                Toast.makeText(this, "Membuka Scan QR Ambil (Task Rydo)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_logout -> {
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
}
