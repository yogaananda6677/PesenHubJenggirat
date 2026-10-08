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

        // Default tampilkan KasirFragment
        if (savedInstanceState == null) {
            gantiFragment(KasirFragment())
        }

        // Listener BottomNavigationView
        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_kasir -> {
                    gantiFragment(KasirFragment())
                    true
                }
                R.id.nav_antrian -> {
                    Toast.makeText(this, "Modul Antrean KDS (Task Rydo)", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_menu -> {
                    Toast.makeText(this, "Modul Menu Manajemen & Supabase Storage", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_laporan -> {
                    Toast.makeText(this, "Modul Riwayat Transaksi (SQLite Arsip)", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
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
