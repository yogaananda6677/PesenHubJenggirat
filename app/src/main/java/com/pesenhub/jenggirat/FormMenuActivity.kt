package com.pesenhub.jenggirat

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.pesenhub.jenggirat.databinding.ActivityFormMenuBinding
import java.io.ByteArrayOutputStream
import kotlin.math.roundToLong

class FormMenuActivity : AppCompatActivity() {

    lateinit var b: ActivityFormMenuBinding
    lateinit var dbFirestore: FirebaseFirestore

    private var isEditMode = false
    private var originalSku = ""
    private var fotoBitmap: Bitmap? = null
    private var existingImageUrl = "default_food_icon"

    private val listKategori = arrayOf("Martabak Telur", "Terang Bulan", "Minuman")

    private val launcherKamera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            fotoBitmap = bitmap
            b.imgFormPreview.setImageBitmap(bitmap)
            Toast.makeText(this, "Foto berhasil diambil dari kamera", Toast.LENGTH_SHORT).show()
        }
    }

    private val launcherGaleri = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                fotoBitmap = bitmap
                b.imgFormPreview.setImageBitmap(bitmap)
                Toast.makeText(this, "Foto berhasil dipilih dari galeri", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memuat foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFormMenuBinding.inflate(layoutInflater)
        setContentView(b.root)

        dbFirestore = FirebaseFirestore.getInstance()

        // Setup Spinner Kategori
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listKategori)
        b.spFormKategori.adapter = adapterSpinner

        // Cek apakah Edit Mode
        isEditMode = intent.getBooleanExtra("IS_EDIT", false)
        if (isEditMode) {
            b.toolbarFormMenu.title = "Edit Menu & Harga"
            originalSku = intent.getStringExtra("SKU") ?: ""
            b.etFormSku.setText(originalSku)
            b.etFormSku.isEnabled = false // SKU tidak dapat diubah saat edit

            b.etFormNama.setText(intent.getStringExtra("NAMA") ?: "")
            b.etFormDeskripsi.setText(intent.getStringExtra("DESKRIPSI") ?: "")

            val hpp = intent.getLongExtra("HPP", 0L)
            if (hpp > 0) b.etFormHpp.setText(hpp.toString())

            b.cbFormKetersediaan.isChecked = intent.getBooleanExtra("AVAILABLE", true)

            val kat = intent.getStringExtra("KATEGORI") ?: "Martabak Telur"
            val katIndex = listKategori.indexOf(kat)
            if (katIndex >= 0) b.spFormKategori.setSelection(katIndex)

            b.etFormHargaOffline.setText(intent.getLongExtra("PRICE_OFFLINE", 0L).toString())
            b.etFormHargaWeb.setText(intent.getLongExtra("PRICE_WEB", 0L).toString())
            b.etFormHargaGofood.setText(intent.getLongExtra("PRICE_GOFOOD", 0L).toString())
            b.etFormHargaGrabfood.setText(intent.getLongExtra("PRICE_GRABFOOD", 0L).toString())
            b.etFormHargaShopeefood.setText(intent.getLongExtra("PRICE_SHOPEEFOOD", 0L).toString())
        } else {
            b.toolbarFormMenu.title = "Tambah Menu & Multi-Channel"
        }

        b.toolbarFormMenu.setNavigationOnClickListener {
            finish()
        }

        // Auto-fill Web Price saat Offline Price diisi jika kosong
        b.etFormHargaOffline.doAfterTextChanged { s ->
            val offVal = s?.toString()?.trim() ?: ""
            val webVal = b.etFormHargaWeb.text.toString().trim()
            if (offVal.isNotEmpty() && (webVal.isEmpty() || webVal == "0")) {
                b.etFormHargaWeb.setText(offVal)
            }
        }

        // Tombol Auto Markup Ojol (+20% GoFood/GrabFood, +15% ShopeeFood)
        b.btnAutoMarkupOjol.setOnClickListener {
            hitungAutoMarkupOjol()
        }

        // Tombol Kamera & Galeri
        b.btnFormKamera.setOnClickListener {
            launcherKamera.launch(null)
        }

        b.btnFormGaleri.setOnClickListener {
            launcherGaleri.launch("image/*")
        }

        // Tombol Simpan
        b.btnSimpanMenuForm.setOnClickListener {
            simpanMenuKeFirestore()
        }
    }

    private fun hitungAutoMarkupOjol() {
        val base = b.etFormHargaOffline.text.toString().trim().toLongOrNull() ?: 0L
        if (base <= 0) {
            Toast.makeText(this, "Masukkan harga dasar kasir offline terlebih dahulu", Toast.LENGTH_SHORT).show()
            b.etFormHargaOffline.requestFocus()
            return
        }

        val ojolPrice = ((base * 1.20) / 1000.0).roundToLong() * 1000L
        val shopeePrice = ((base * 1.15) / 1000.0).roundToLong() * 1000L

        if (b.etFormHargaWeb.text.isNullOrEmpty()) {
            b.etFormHargaWeb.setText(base.toString())
        }
        b.etFormHargaGofood.setText(ojolPrice.toString())
        b.etFormHargaGrabfood.setText(ojolPrice.toString())
        b.etFormHargaShopeefood.setText(shopeePrice.toString())

        Toast.makeText(this, "Harga ojol dihitung otomatis (+20%)", Toast.LENGTH_SHORT).show()
    }

    private fun simpanMenuKeFirestore() {
        val nama = b.etFormNama.text.toString().trim()
        val sku = b.etFormSku.text.toString().trim().uppercase()
        val kategori = b.spFormKategori.selectedItem?.toString() ?: "Martabak Telur"
        val deskripsi = b.etFormDeskripsi.text.toString().trim()
        val hpp = b.etFormHpp.text.toString().trim().toLongOrNull() ?: 0L
        val isAvailable = b.cbFormKetersediaan.isChecked

        val pOffline = b.etFormHargaOffline.text.toString().trim().toLongOrNull() ?: 0L
        val pWeb = b.etFormHargaWeb.text.toString().trim().toLongOrNull() ?: pOffline
        val pGofood = b.etFormHargaGofood.text.toString().trim().toLongOrNull() ?: pOffline
        val pGrabfood = b.etFormHargaGrabfood.text.toString().trim().toLongOrNull() ?: pOffline
        val pShopeefood = b.etFormHargaShopeefood.text.toString().trim().toLongOrNull() ?: pOffline

        if (nama.isEmpty()) {
            b.etFormNama.error = "Nama menu tidak boleh kosong"
            b.etFormNama.requestFocus()
            return
        }

        if (sku.isEmpty()) {
            b.etFormSku.error = "Kode SKU wajib diisi"
            b.etFormSku.requestFocus()
            return
        }

        if (pOffline <= 0) {
            b.etFormHargaOffline.error = "Harga kasir offline wajib diisi"
            b.etFormHargaOffline.requestFocus()
            return
        }

        b.btnSimpanMenuForm.isEnabled = false
        b.btnSimpanMenuForm.text = "Menyimpan ke Cloud Firestore..."

        val channelMap = hashMapOf(
            "OFFLINE" to pOffline,
            "CUSTOMER_WEB" to pWeb,
            "GOFOOD" to pGofood,
            "GRABFOOD" to pGrabfood,
            "SHOPEEFOOD" to pShopeefood
        )

        val dataFirestore = hashMapOf(
            "sku" to sku,
            "name" to nama,
            "category" to kategori,
            "description" to deskripsi,
            "price" to pOffline,
            "hppAmount" to hpp,
            "available" to isAvailable,
            "imageUrl" to existingImageUrl,
            "channelPrices" to channelMap,
            "updatedAt" to Timestamp.now()
        )

        if (!isEditMode) {
            dataFirestore["createdAt"] = Timestamp.now()
        }

        dbFirestore.collection("menus").document(sku)
            .set(dataFirestore)
            .addOnSuccessListener {
                val aksi = if (isEditMode) "diperbarui" else "ditambahkan"
                Toast.makeText(this, "Menu '$nama' ($sku) berhasil $aksi di Cloud Firestore!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                b.btnSimpanMenuForm.isEnabled = true
                b.btnSimpanMenuForm.text = "Simpan Menu & Multi-Channel"
                Toast.makeText(this, "Gagal menyimpan ke Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
