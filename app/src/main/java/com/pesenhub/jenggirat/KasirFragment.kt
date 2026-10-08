package com.pesenhub.jenggirat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.pesenhub.jenggirat.databinding.FragmentKasirBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KasirFragment : Fragment() {

    lateinit var b: FragmentKasirBinding
    lateinit var dbHelper: DBOpenHelper
    lateinit var dbFirestore: FirebaseFirestore

    val listMenu = ArrayList<String>()
    val listKategori = arrayOf("Semua Kategori", "Martabak Telur", "Terang Bulan", "Minuman")
    val listPelanggan = arrayOf("Yoga", "Rydo", "Budi", "Siti", "Andi")

    var menuTerpilih = "Martabak Telur Spesial"
    var hargaDasar = 35000
    var metodeBayar = "Tunai"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentKasirBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBOpenHelper(requireContext())
        dbFirestore = FirebaseFirestore.getInstance()

        // 1. AutoCompleteTextView Pelanggan (Sesuai Modul)
        val adapterAuto = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listPelanggan)
        b.autoNamaPelanggan.setAdapter(adapterAuto)

        // 2. Spinner Kategori (Sesuai Modul)
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listKategori)
        b.spKategori.adapter = adapterSpinner

        // 3. ListView Menu dari SQLite (Sesuai Bab 08 PM)
        muatMenuDariSQLite()

        b.lsMenu.setOnItemClickListener { _, _, position, _ ->
            menuTerpilih = listMenu[position]
            Toast.makeText(requireContext(), "Dipilih: $menuTerpilih", Toast.LENGTH_SHORT).show()
        }

        // 4. RadioButton Listener (Sesuai Modul Bab 02 PM)
        b.rgBayar.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                b.rbTunai.id -> metodeBayar = "Tunai"
                b.rbQris.id -> metodeBayar = "QRIS"
            }
        }

        // 5. Button Simpan Pesanan (Simpan ganda: SQLite + Firestore)
        b.btnSimpanPesanan.setOnClickListener {
            val nama = b.autoNamaPelanggan.text.toString().trim()
            val hp = b.edtHpPelanggan.text.toString().trim()
            val catatan = b.edtCatatan.text.toString().trim()

            if (nama.isEmpty()) {
                b.autoNamaPelanggan.error = "Nama pelanggan wajib diisi"
                return@setOnClickListener
            }

            // Hitung total dengan CheckBox topping
            var total = hargaDasar
            val topping = ArrayList<String>()
            if (b.cbKeju.isChecked) { total += 5000; topping.add("Keju") }
            if (b.cbCoklat.isChecked) { total += 4000; topping.add("Coklat") }
            if (b.cbPedas.isChecked) { topping.add("Pedas Lvl 2") }

            val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            val idPesanan = "ORD-${sdf.format(Date())}"
            val waktu = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val detailItem = "$menuTerpilih (${topping.joinToString(", ")})"

            // A. Simpan ke SQLite (DBOpenHelper - Bab 08 PM)
            val dbLocal = dbHelper.writableDatabase
            val sqlInsert = "insert into pesanan_offline(id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, status_order, waktu_dibuat, is_synced) " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?, 1)"
            dbLocal.execSQL(sqlInsert, arrayOf(idPesanan, nama, hp, detailItem, metodeBayar, total, "PENDING", waktu))

            // B. Simpan ke Firebase Firestore (Bab 02 & Bab 03 PML)
            val dataFirestore = hashMapOf(
                "orderNumber" to idPesanan,
                "customerName" to nama,
                "customerPhone" to hp,
                "menuItem" to detailItem,
                "paymentMethod" to metodeBayar,
                "total" to total,
                "notes" to catatan,
                "status" to "PENDING",
                "source" to "CASHIER",
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            dbFirestore.collection("orders").document(idPesanan)
                .set(dataFirestore)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Pesanan $idPesanan berhasil disimpan ke SQLite & Firestore!", Toast.LENGTH_LONG).show()
                    b.autoNamaPelanggan.setText("")
                    b.edtHpPelanggan.setText("")
                    b.edtCatatan.setText("")
                    b.cbKeju.isChecked = false
                    b.cbCoklat.isChecked = false
                    b.cbPedas.isChecked = false
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Tersimpan offline di SQLite (Gagal Cloud: ${e.message})", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun muatMenuDariSQLite() {
        listMenu.clear()
        val dbLocal = dbHelper.readableDatabase
        val cursor = dbLocal.rawQuery("select nama_menu, harga from master_menu where tersedia = 1", null)
        if (cursor.moveToFirst()) {
            do {
                val nama = cursor.getString(0)
                val harga = cursor.getInt(1)
                listMenu.add("$nama - Rp $harga")
            } while (cursor.moveToNext())
        }
        cursor.close()

        val adapterList = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listMenu)
        b.lsMenu.adapter = adapterList
    }
}
