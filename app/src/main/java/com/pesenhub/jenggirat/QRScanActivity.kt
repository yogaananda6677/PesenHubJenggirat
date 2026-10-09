package com.pesenhub.jenggirat

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.pesenhub.jenggirat.databinding.ActivityQrScanBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QRScanActivity : AppCompatActivity() {

    lateinit var b: ActivityQrScanBinding
    lateinit var dbFirestore: FirebaseFirestore
    lateinit var dbHelper: DBOpenHelper

    private var scannedOrderId: String? = null
    private var dataOrderDitemukan: Map<String, Any?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityQrScanBinding.inflate(layoutInflater)
        setContentView(b.root)

        dbFirestore = FirebaseFirestore.getInstance()
        dbHelper = DBOpenHelper(this)

        // Cek izin Kamera
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 101)
        }

        // Mulai scanning QR Code (Bab 10 Modul PM Pak Benni)
        b.barcodeScannerView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result != null && result.text.isNotEmpty()) {
                    prosesHasilScan(result.text.trim())
                }
            }
        })

        b.btnVerifikasiSelesai.setOnClickListener {
            selesaikanPesananDanArsipkan()
        }

        b.btnScanUlang.setOnClickListener {
            resetScanner()
        }
    }

    private fun prosesHasilScan(orderId: String) {
        scannedOrderId = orderId
        b.barcodeScannerView.pause()

        Toast.makeText(this, "QR Terbaca: $orderId, mencari data...", Toast.LENGTH_SHORT).show()

        // Cari dokumen pesanan di Firestore
        dbFirestore.collection("orders").document(orderId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val data = doc.data ?: mapOf()
                    dataOrderDitemukan = data

                    val noOrder = data["orderNumber"] ?: doc.id
                    val nama = data["customerName"] ?: "-"
                    val item = data["menuItem"] ?: "-"
                    val status = data["status"] ?: "PENDING"

                    b.txHasilOrderNumber.text = "No. Pesanan: #$noOrder"
                    b.txHasilCustomer.text = "Pelanggan: $nama"
                    b.txHasilItem.text = "Item: $item"
                    b.txHasilStatus.text = "Status: $status"

                    b.layoutHasilScan.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Pesanan #$orderId tidak ditemukan di server!", Toast.LENGTH_LONG).show()
                    resetScanner()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
                resetScanner()
            }
    }

    private fun selesaikanPesananDanArsipkan() {
        val orderId = scannedOrderId ?: return
        val order = dataOrderDitemukan ?: return

        // 1. Update status pesanan di Firestore menjadi COMPLETED
        dbFirestore.collection("orders").document(orderId)
            .update("status", "COMPLETED")
            .addOnSuccessListener {
                // 2. Arsipkan ke SQLite lokal
                val nama = order["customerName"]?.toString() ?: "-"
                val hp = order["customerPhone"]?.toString() ?: "-"
                val menu = order["menuItem"]?.toString() ?: "-"
                val metode = order["paymentMethod"]?.toString() ?: "Tunai"
                val total = (order["total"] as? Number)?.toInt() ?: 0
                val waktuSelesai = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                try {
                    val dbLocal = dbHelper.writableDatabase
                    val sqlInsert = "insert or replace into riwayat_transaksi(id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, status_order, waktu_selesai) " +
                            "values (?, ?, ?, ?, ?, ?, ?, ?)"
                    dbLocal.execSQL(sqlInsert, arrayOf(orderId, nama, hp, menu, metode, total, "COMPLETED", waktuSelesai))
                    Toast.makeText(this, "Pesanan #$orderId berhasil diselesaikan dan diarsipkan ke SQLite!", Toast.LENGTH_LONG).show()
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Gagal arsip SQLite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal update status Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetScanner() {
        b.layoutHasilScan.visibility = View.GONE
        scannedOrderId = null
        dataOrderDitemukan = null
        b.barcodeScannerView.resume()
        b.barcodeScannerView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result != null && result.text.isNotEmpty()) {
                    prosesHasilScan(result.text.trim())
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        b.barcodeScannerView.resume()
    }

    override fun onPause() {
        super.onPause()
        b.barcodeScannerView.pause()
    }
}
