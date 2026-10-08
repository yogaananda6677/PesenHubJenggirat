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

        dbFirestore = FirebaseFirestore.getInstance()

        // 1. AutoCompleteTextView Pelanggan (Sesuai Modul PM)
        val adapterAuto = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listPelanggan)
        b.autoNamaPelanggan.setAdapter(adapterAuto)

        // 2. Spinner Kategori
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listKategori)
        b.spKategori.adapter = adapterSpinner

        // 3. Muat Data Menu Langsung Realtime dari Cloud Firestore (Penyimpanan Utama)
        muatMenuDariFirestore()

        b.lsMenu.setOnItemClickListener { _, _, position, _ ->
            if (position < listMenu.size) {
                menuTerpilih = listMenu[position]
                Toast.makeText(requireContext(), "Dipilih: $menuTerpilih", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. RadioButton Pembayaran (Bab 02 PM)
        b.rgBayar.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                b.rbTunai.id -> metodeBayar = "Tunai"
                b.rbQris.id -> metodeBayar = "QRIS"
            }
        }

        // 5. Simpan Pesanan ke Cloud Firestore sebagai Basis Data Utama
        b.btnSimpanPesanan.setOnClickListener {
            val nama = b.autoNamaPelanggan.text.toString().trim()
            val hp = b.edtHpPelanggan.text.toString().trim()
            val catatan = b.edtCatatan.text.toString().trim()

            if (nama.isEmpty()) {
                b.autoNamaPelanggan.error = "Nama pelanggan wajib diisi"
                return@setOnClickListener
            }

            var total = hargaDasar
            val topping = ArrayList<String>()
            if (b.cbKeju.isChecked) { total += 5000; topping.add("Keju") }
            if (b.cbCoklat.isChecked) { total += 4000; topping.add("Coklat") }
            if (b.cbPedas.isChecked) { topping.add("Pedas Lvl 2") }

            val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            val idPesanan = "ORD-${sdf.format(Date())}"
            val detailItem = "$menuTerpilih (${topping.joinToString(", ")})"

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

            // Simpan langsung ke Firestore sebagai primary database
            dbFirestore.collection("orders").document(idPesanan)
                .set(dataFirestore)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Pesanan $idPesanan berhasil tersimpan di Firestore!", Toast.LENGTH_SHORT).show()
                    b.autoNamaPelanggan.setText("")
                    b.edtHpPelanggan.setText("")
                    b.edtCatatan.setText("")
                    b.cbKeju.isChecked = false
                    b.cbCoklat.isChecked = false
                    b.cbPedas.isChecked = false
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal simpan ke Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun muatMenuDariFirestore() {
        // Realtime Listener koleksi menus di Firestore
        dbFirestore.collection("menus")
            .addSnapshotListener { snapshot, e ->
                listMenu.clear()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val nama = doc.getString("name") ?: "Menu"
                        val harga = doc.getLong("price") ?: 0L
                        listMenu.add("$nama - Rp $harga")
                    }
                } else {
                    // Fallback default bila menu Firestore belum diisi
                    listMenu.add("Martabak Telur Spesial - Rp 35000")
                    listMenu.add("Terang Bulan Coklat Keju - Rp 30000")
                    listMenu.add("Es Teh Manis Jumbo - Rp 5000")
                }

                if (isAdded) {
                    val adapterList = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listMenu)
                    b.lsMenu.adapter = adapterList
                }
            }
    }
}
