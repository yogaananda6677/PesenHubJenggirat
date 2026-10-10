package com.pesenhub.jenggirat

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.pesenhub.jenggirat.databinding.ActivityOrderDetailBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    lateinit var b: ActivityOrderDetailBinding
    private val dbFirestore = FirebaseFirestore.getInstance()
    private var currentStatus: String = "PENDING"
    private var currentBarcodeUrl: String = ""
    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        val docId = intent.getStringExtra("EXTRA_DOC_ID") ?: intent.getStringExtra("EXTRA_ORDER_NUMBER") ?: "ORD-DEMO"
        val orderNumber = intent.getStringExtra("EXTRA_ORDER_NUMBER") ?: docId
        val customerName = intent.getStringExtra("EXTRA_CUSTOMER_NAME") ?: "-"
        val customerPhone = intent.getStringExtra("EXTRA_CUSTOMER_PHONE") ?: "-"
        val menuItem = intent.getStringExtra("EXTRA_MENU_ITEM") ?: "-"
        val paymentMethod = intent.getStringExtra("EXTRA_PAYMENT_METHOD") ?: "Tunai"
        val total = intent.getIntExtra("EXTRA_TOTAL", 0)
        currentStatus = intent.getStringExtra("EXTRA_STATUS") ?: "PENDING"
        val source = intent.getStringExtra("EXTRA_SOURCE") ?: "CASHIER"
        currentBarcodeUrl = intent.getStringExtra("EXTRA_BARCODE_URL") ?: ""
        val notes = intent.getStringExtra("EXTRA_NOTES") ?: "-"

        b.txOrderNumber.text = "No. Pesanan: #$orderNumber"
        b.txCustomerName.text = "Pelanggan: $customerName"
        b.txCustomerPhone.text = "No. HP: $customerPhone"
        b.txMenuItem.text = menuItem
        b.txPaymentMethod.text = "Bayar: $paymentMethod"
        b.txNotes.text = "Catatan: $notes"
        b.txTotalPayment.text = "Total: Rp " + "%,d".format(total).replace(',', '.')

        perbaruiTampilanStatus(currentStatus, currentBarcodeUrl)

        // Generate QR Code menggunakan ZXing BarcodeEncoder (Bab 10 Modul PM)
        generateQrCode(orderNumber)

        // Tombol Terima & Upload QR Supabase
        b.btnTerimaUploadQr.setOnClickListener {
            prosesDanUploadQr(docId, orderNumber, "CONFIRMED")
        }

        // Tombol Mulai Proses Masak (DIPROSES)
        b.btnMulaiProses.setOnClickListener {
            prosesDanUploadQr(docId, orderNumber, "PREPARING")
        }

        // Tombol Selesaikan Pesanan (SELESAI)
        b.btnSelesaiPesanan.setOnClickListener {
            selesaikanPesanan(docId, orderNumber, customerName, customerPhone, menuItem, paymentMethod, total)
        }

        b.btnKembali.setOnClickListener {
            finish()
        }
    }

    private fun perbaruiTampilanStatus(status: String, barcodeUrl: String) {
        currentStatus = status
        b.txOrderStatus.text = "STATUS: $status"

        if (barcodeUrl.isNotEmpty()) {
            b.txStatusSupabase.visibility = View.VISIBLE
            b.txStatusSupabase.text = "✓ QR Code aktif di Supabase Storage"
        } else {
            b.txStatusSupabase.visibility = View.GONE
        }

        when (status) {
            "PENDING" -> {
                b.btnTerimaUploadQr.visibility = View.VISIBLE
                b.btnMulaiProses.visibility = View.VISIBLE
                b.btnSelesaiPesanan.visibility = View.GONE
            }
            "CONFIRMED" -> {
                b.btnTerimaUploadQr.visibility = View.GONE
                b.btnMulaiProses.visibility = View.VISIBLE
                b.btnSelesaiPesanan.visibility = View.GONE
            }
            "PREPARING" -> {
                b.btnTerimaUploadQr.visibility = View.GONE
                b.btnMulaiProses.visibility = View.GONE
                b.btnSelesaiPesanan.visibility = View.VISIBLE
            }
            "COMPLETED", "CANCELLED" -> {
                b.layoutAksiPesanan.visibility = View.GONE
            }
        }
    }

    private fun generateQrCode(text: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 500, 500)
            qrBitmap = bitmap
            b.imgQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuat QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prosesDanUploadQr(docId: String, orderNumber: String, targetStatus: String) {
        val bitmap = qrBitmap
        if (bitmap == null) {
            updateFirestoreStatus(docId, orderNumber, targetStatus, null)
            return
        }

        b.btnTerimaUploadQr.isEnabled = false
        b.btnMulaiProses.isEnabled = false
        b.txStatusSupabase.visibility = View.VISIBLE
        b.txStatusSupabase.text = "Mengunggah QR Code ke Supabase Storage..."

        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val qrBytes = stream.toByteArray()

            SupabaseStorageHelper.uploadOrderQrCode(orderNumber, qrBytes) { success, publicUrl, error ->
                b.btnTerimaUploadQr.isEnabled = true
                b.btnMulaiProses.isEnabled = true

                if (success && publicUrl != null) {
                    currentBarcodeUrl = publicUrl
                    b.txStatusSupabase.text = "✓ QR Code berhasil diunggah ke Supabase!"
                    updateFirestoreStatus(docId, orderNumber, targetStatus, publicUrl)
                } else {
                    b.txStatusSupabase.text = "Supabase upload gagal: $error"
                    updateFirestoreStatus(docId, orderNumber, targetStatus, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updateFirestoreStatus(docId, orderNumber, targetStatus, null)
        }
    }

    private fun updateFirestoreStatus(docId: String, orderNumber: String, targetStatus: String, barcodeUrl: String?) {
        val updateMap = mutableMapOf<String, Any>(
            "status" to targetStatus,
            "updatedAt" to Timestamp.now()
        )
        if (!barcodeUrl.isNullOrEmpty()) {
            updateMap["barcodeUrl"] = barcodeUrl
            updateMap["barcode_url"] = barcodeUrl
            updateMap["barcodeCode"] = orderNumber
        }

        dbFirestore.collection("orders").document(docId)
            .update(updateMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Pesanan #$orderNumber berhasil diubah menjadi $targetStatus!", Toast.LENGTH_SHORT).show()
                perbaruiTampilanStatus(targetStatus, barcodeUrl ?: currentBarcodeUrl)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal update Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun selesaikanPesanan(
        docId: String,
        orderNumber: String,
        customerName: String,
        customerPhone: String,
        menuItem: String,
        paymentMethod: String,
        total: Int
    ) {
        b.btnSelesaiPesanan.isEnabled = false
        val updateMap = mutableMapOf<String, Any>(
            "status" to "COMPLETED",
            "updatedAt" to Timestamp.now()
        )

        dbFirestore.collection("orders").document(docId)
            .update(updateMap)
            .addOnSuccessListener {
                SoundHelper.playSuccess()
                Toast.makeText(this, "Pesanan #$orderNumber SELESAI!", Toast.LENGTH_SHORT).show()
                perbaruiTampilanStatus("COMPLETED", currentBarcodeUrl)

                // Simpan ke SQLite lokal
                try {
                    val dbHelper = DBOpenHelper(this)
                    val waktuSelesai = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    val sqlInsert = "insert or replace into riwayat_transaksi(" +
                            "id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, status_order, waktu_selesai) " +
                            "values (?, ?, ?, ?, ?, ?, ?, ?)"
                    dbHelper.writableDatabase.execSQL(
                        sqlInsert,
                        arrayOf(orderNumber, customerName, customerPhone, menuItem, paymentMethod, total.toLong(), "COMPLETED", waktuSelesai)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .addOnFailureListener { e ->
                b.btnSelesaiPesanan.isEnabled = true
                Toast.makeText(this, "Gagal menyelesaikan pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
