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
import com.google.firebase.firestore.FirebaseFirestore
import com.pesenhub.jenggirat.databinding.ActivityUploadMenuBinding
import java.io.ByteArrayOutputStream

class UploadMenuActivity : AppCompatActivity() {

    lateinit var b: ActivityUploadMenuBinding
    lateinit var dbFirestore: FirebaseFirestore

    private var fotoBitmap: Bitmap? = null
    val listKategori = arrayOf("Martabak Telur", "Terang Bulan", "Minuman")

    // Launcher Kamera (Bab 12 Modul PM / Checklist #20)
    private val launcherKamera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            fotoBitmap = bitmap
            b.imgPreviewFoto.setImageBitmap(bitmap)
            Toast.makeText(this, "Foto berhasil diambil dari kamera!", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher Galeri (Checklist #26)
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
                b.imgPreviewFoto.setImageBitmap(bitmap)
                Toast.makeText(this, "Foto berhasil dipilih dari galeri!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUploadMenuBinding.inflate(layoutInflater)
        setContentView(b.root)

        dbFirestore = FirebaseFirestore.getInstance()

        // 1. Setup Spinner Kategori
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listKategori)
        b.spKategoriMenu.adapter = adapterSpinner

        // 2. Tombol Kamera & Galeri
        b.btnAmbilKamera.setOnClickListener {
            launcherKamera.launch(null)
        }

        b.btnPilihGaleri.setOnClickListener {
            launcherGaleri.launch("image/*")
        }

        // 3. Tombol Simpan ke Supabase Storage & Cloud Firestore (Checklist #33 & #30)
        b.btnSimpanMenu.setOnClickListener {
            simpanMenuKeSupabaseDanFirestore()
        }
    }

    private fun simpanMenuKeSupabaseDanFirestore() {
        val nama = b.edtNamaMenu.text.toString().trim()
        val kategori = b.spKategoriMenu.selectedItem?.toString() ?: "Martabak Telur"
        val hargaStr = b.edtHargaMenu.text.toString().trim()

        if (nama.isEmpty()) {
            b.edtNamaMenu.error = "Nama menu tidak boleh kosong"
            b.edtNamaMenu.requestFocus()
            return
        }

        if (hargaStr.isEmpty()) {
            b.edtHargaMenu.error = "Harga menu tidak boleh kosong"
            b.edtHargaMenu.requestFocus()
            return
        }

        val harga = hargaStr.toIntOrNull() ?: 0
        val idMenu = "MENU-${System.currentTimeMillis()}"

        b.btnSimpanMenu.isEnabled = false
        b.txStatusUpload.text = "Mengunggah aset foto ke Supabase Storage..."

        // Kompresi foto jika ada
        val imageBytes = if (fotoBitmap != null) {
            val stream = ByteArrayOutputStream()
            fotoBitmap?.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.toByteArray()
        } else {
            null
        }

        if (imageBytes != null) {
            SupabaseStorageHelper.uploadMenuImage("$idMenu.jpg", imageBytes) { success, publicUrl, error ->
                val finalImageUrl = if (success && publicUrl != null) publicUrl else "default_food_icon"
                b.txStatusUpload.text = if (success) "Foto terunggah ke Supabase! Menyimpan ke Firestore..." else "Supabase: $error"
                simpanMenuKeFirestore(idMenu, nama, kategori, harga, finalImageUrl)
            }
        } else {
            simpanMenuKeFirestore(idMenu, nama, kategori, harga, "default_food_icon")
        }
    }

    private fun simpanMenuKeFirestore(idMenu: String, nama: String, kategori: String, harga: Int, imageUrl: String) {
        val dataMenu = hashMapOf(
            "name" to nama,
            "category" to kategori,
            "price" to harga.toLong(),
            "available" to true,
            "imageUrl" to imageUrl,
            "storageProvider" to if (imageUrl.startsWith("http")) "Supabase" else "Local",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        dbFirestore.collection("menus").document(idMenu)
            .set(dataMenu)
            .addOnSuccessListener {
                Toast.makeText(this, "Menu '$nama' berhasil diunggah dengan Supabase Storage!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                b.btnSimpanMenu.isEnabled = true
                Toast.makeText(this, "Gagal menyimpan menu: ${e.message}", Toast.LENGTH_SHORT).show()
                b.txStatusUpload.text = "Gagal menyimpan: ${e.message}"
            }
    }
}
