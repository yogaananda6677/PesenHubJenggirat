package com.pesenhub.jenggirat

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.pesenhub.jenggirat.databinding.ActivityOrderDetailBinding

class OrderDetailActivity : AppCompatActivity() {

    lateinit var b: ActivityOrderDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        val orderNumber = intent.getStringExtra("EXTRA_ORDER_NUMBER") ?: "ORD-DEMO"
        val customerName = intent.getStringExtra("EXTRA_CUSTOMER_NAME") ?: "-"
        val customerPhone = intent.getStringExtra("EXTRA_CUSTOMER_PHONE") ?: "-"
        val menuItem = intent.getStringExtra("EXTRA_MENU_ITEM") ?: "-"
        val paymentMethod = intent.getStringExtra("EXTRA_PAYMENT_METHOD") ?: "Tunai"
        val total = intent.getIntExtra("EXTRA_TOTAL", 0)
        val status = intent.getStringExtra("EXTRA_STATUS") ?: "PENDING"
        val notes = intent.getStringExtra("EXTRA_NOTES") ?: "-"

        b.txOrderNumber.text = "No. Pesanan: #$orderNumber"
        b.txCustomerName.text = "Pelanggan: $customerName"
        b.txCustomerPhone.text = "No. HP: $customerPhone"
        b.txMenuItem.text = menuItem
        b.txPaymentMethod.text = "Bayar: $paymentMethod"
        b.txOrderStatus.text = "STATUS: $status"
        b.txNotes.text = "Catatan: $notes"
        b.txTotalPayment.text = "Total: Rp " + "%,d".format(total).replace(',', '.')

        // Generate QR Code menggunakan ZXing BarcodeEncoder (Bab 10 Modul PM)
        generateQrCode(orderNumber)

        b.btnKembali.setOnClickListener {
            finish()
        }
    }

    private fun generateQrCode(text: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 500, 500)
            b.imgQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuat QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
