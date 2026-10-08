package com.pesenhub.jenggirat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.pesenhub.jenggirat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(b.toolbar)

        // Default tampilkan KasirFragment
        if (savedInstanceState == null) {
            gantiFragment(KasirFragment())
        }

        // Listener BottomNavigationView sesuai modul
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
                    Toast.makeText(this, "Modul Menu Manajemen", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_laporan -> {
                    Toast.makeText(this, "Modul Laporan Penjualan", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Sinkronisasi data diperbarui", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_maps -> {
                Toast.makeText(this, "Membuka Lokasi Outlet OSMdroid (Task Rydo)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_qr_scan -> {
                Toast.makeText(this, "Membuka Scan QR (Task Rydo)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_logout -> {
                Toast.makeText(this, "Sesi login ditutup", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
