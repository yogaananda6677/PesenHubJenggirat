package com.pesenhub.jenggirat

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.pesenhub.jenggirat.databinding.ActivityMapsBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MapsActivity : AppCompatActivity() {

    lateinit var b: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Koordinat Pusat Outlet Martabak Jenggirat (Banyuwangi)
    private val outletPoint = GeoPoint(-8.2192, 114.3692)
    private var markerUser: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inisialisasi OSMdroid Configuration (Bab 13 Modul PM Pak Benni)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        b = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(b.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 2. Setup MapView
        b.mapView.setTileSource(TileSourceFactory.MAPNIK)
        b.mapView.setMultiTouchControls(true)
        val mapController = b.mapView.controller
        mapController.setZoom(16.0)
        mapController.setCenter(outletPoint)

        // 3. Tambahkan Pin / Marker Lokasi Outlet
        val markerOutlet = Marker(b.mapView)
        markerOutlet.position = outletPoint
        markerOutlet.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        markerOutlet.title = "PesenHub Jenggirat"
        markerOutlet.snippet = "Pusat Martabak & Terang Bulan Jenggirat"
        b.mapView.overlays.add(markerOutlet)
        b.mapView.invalidate()

        // 4. Tombol Pusatkan ke Outlet
        b.btnPusatOutlet.setOnClickListener {
            mapController.animateTo(outletPoint)
            mapController.setZoom(16.5)
            Toast.makeText(this, "Berpindah ke Outlet Jenggirat", Toast.LENGTH_SHORT).show()
        }

        // 5. Tombol Deteksi GPS Lokasi Saya
        b.btnLokasiSaya.setOnClickListener {
            ambilLokasiGps()
        }

        // Cek izin lokasi saat pertama kali dibuka
        cekIzinLokasiDanMuat()
    }

    private fun cekIzinLokasiDanMuat() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                102
            )
        } else {
            ambilLokasiGps()
        }
    }

    private fun ambilLokasiGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin lokasi diperlukan untuk fitur GPS", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userPoint = GeoPoint(location.latitude, location.longitude)

                if (markerUser == null) {
                    markerUser = Marker(b.mapView).apply {
                        title = "Posisi Kasir (GPS)"
                        snippet = "Akurasi: ${location.accuracy} meter"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    b.mapView.overlays.add(markerUser)
                }

                markerUser?.position = userPoint
                b.mapView.controller.animateTo(userPoint)
                b.mapView.controller.setZoom(17.0)
                b.mapView.invalidate()

                // Hitung estimasi jarak ke outlet
                val jarakMeter = location.distanceTo(Location("outlet").apply {
                    latitude = outletPoint.latitude
                    longitude = outletPoint.longitude
                })

                val strJarak = if (jarakMeter >= 1000) {
                    "%.2f km".format(jarakMeter / 1000)
                } else {
                    "${jarakMeter.toInt()} meter"
                }

                b.txJarakInfo.text = "GPS Terdeteksi: Lat ${"%.4f".format(location.latitude)}, Lng ${"%.4f".format(location.longitude)} (Jarak ke outlet: $strJarak)"
                Toast.makeText(this, "Lokasi GPS berhasil ditemukan ($strJarak ke outlet)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sinyal GPS belum didapatkan, pastikan GPS aktif", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Gagal mengambil GPS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        b.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        b.mapView.onPause()
    }
}
