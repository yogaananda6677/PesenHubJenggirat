package com.pesenhub.jenggirat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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

    // Data Menu
    data class MenuModel(val nama: String, val harga: Int, val kategori: String)

    val listSemuaMenu = ArrayList<MenuModel>()
    val listMenuTampil = ArrayList<String>()
    val listNamaMenu = ArrayList<String>()

    val listKategori = arrayOf("Semua Kategori", "Martabak Telur", "Terang Bulan", "Minuman")

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

        // 1. Inisialisasi Spinner Kategori (Bab II Modul PM Pak Benni)
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listKategori)
        b.spKategori.adapter = adapterSpinner

        b.spKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterMenuBerdasarkanKategori(listKategori[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Muat Data Menu dari Cloud Firestore (Penyimpanan Utama) & Fallback Default
        muatMenuDariFirestore()

        // 3. AutoCompleteTextView untuk Pencarian Item Menu (Sesuai Bab II Modul PM Pak Benni)
        b.autoCariMenu.setOnItemClickListener { parent, _, position, _ ->
            val namaDipilih = parent.getItemAtPosition(position).toString()
            pilihMenuBerdasarkanNama(namaDipilih)
        }

        // 4. Klik Item pada ListView Menu
        b.lsMenu.setOnItemClickListener { _, _, position, _ ->
            if (position in listMenuTampil.indices) {
                val itemText = listMenuTampil[position]
                val nama = itemText.substringBefore(" - Rp").trim()
                pilihMenuBerdasarkanNama(nama)
                b.autoCariMenu.setText(nama, false)
            }
        }

        // 5. RadioButton Pembayaran (Bab II Modul PM Pak Benni)
        b.rgBayar.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                b.rbTunai.id -> metodeBayar = "Tunai"
                b.rbQris.id -> metodeBayar = "QRIS"
            }
        }

        // 6. TimePickerDialog Estimasi Jam Ambil (Bab III Modul PM Pak Benni)
        var estimasiJamAmbil = "Langsung (15-20 mnt)"
        b.btnPilihJamAmbil.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            val jamSekarang = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val menitSekarang = cal.get(java.util.Calendar.MINUTE)

            android.app.TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val strH = if (hourOfDay < 10) "0$hourOfDay" else "$hourOfDay"
                    val strM = if (minute < 10) "0$minute" else "$minute"
                    estimasiJamAmbil = "$strH:$strM"
                    b.txJamAmbilInfo.text = "Jam: $estimasiJamAmbil"
                },
                jamSekarang,
                menitSekarang,
                true
            ).show()
        }

        // 7. Simpan Pesanan ke Cloud Firestore sebagai Basis Data Utama
        b.btnSimpanPesanan.setOnClickListener {
            val nama = b.edtNamaPelanggan.text.toString().trim()
            val hp = b.edtHpPelanggan.text.toString().trim()
            val catatan = b.edtCatatan.text.toString().trim()

            if (nama.isEmpty()) {
                b.edtNamaPelanggan.error = "Nama pelanggan wajib diisi"
                b.edtNamaPelanggan.requestFocus()
                return@setOnClickListener
            }

            var total = hargaDasar
            val topping = ArrayList<String>()
            if (b.cbKeju.isChecked) { total += 5000; topping.add("Keju") }
            if (b.cbCoklat.isChecked) { total += 4000; topping.add("Coklat") }
            if (b.cbPedas.isChecked) { topping.add("Pedas Lvl 2") }

            val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            val idPesanan = "ORD-${sdf.format(Date())}"
            val detailItem = if (topping.isNotEmpty()) {
                "$menuTerpilih (${topping.joinToString(", ")})"
            } else {
                menuTerpilih
            }

            val dataFirestore = hashMapOf(
                "orderNumber" to idPesanan,
                "customerName" to nama,
                "customerPhone" to hp,
                "menuItem" to detailItem,
                "paymentMethod" to metodeBayar,
                "total" to total,
                "notes" to "$catatan (Siap: $estimasiJamAmbil)",
                "status" to "PENDING",
                "source" to "CASHIER",
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            // Simpan langsung ke Firestore sebagai primary database
            dbFirestore.collection("orders").document(idPesanan)
                .set(dataFirestore)
                .addOnSuccessListener {
                    SoundHelper.playBell()
                    Toast.makeText(requireContext(), "Pesanan $idPesanan berhasil tersimpan di Firestore!", Toast.LENGTH_SHORT).show()

                    // Buka OrderDetailActivity untuk menampilkan QR Code struk pesanan (Bab 10 PM)
                    val intentDetail = android.content.Intent(requireContext(), OrderDetailActivity::class.java).apply {
                        putExtra("EXTRA_ORDER_NUMBER", idPesanan)
                        putExtra("EXTRA_CUSTOMER_NAME", nama)
                        putExtra("EXTRA_CUSTOMER_PHONE", hp)
                        putExtra("EXTRA_MENU_ITEM", detailItem)
                        putExtra("EXTRA_PAYMENT_METHOD", metodeBayar)
                        putExtra("EXTRA_TOTAL", total)
                        putExtra("EXTRA_STATUS", "PENDING")
                        putExtra("EXTRA_NOTES", "$catatan (Siap: $estimasiJamAmbil)")
                    }
                    startActivity(intentDetail)

                    b.edtNamaPelanggan.setText("")
                    b.edtHpPelanggan.setText("")
                    b.edtCatatan.setText("")
                    b.autoCariMenu.setText("")
                    b.cbKeju.isChecked = false
                    b.cbCoklat.isChecked = false
                    b.cbPedas.isChecked = false
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal simpan ke Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun pilihMenuBerdasarkanNama(nama: String) {
        val menu = listSemuaMenu.find { it.nama.equals(nama, ignoreCase = true) }
        if (menu != null) {
            menuTerpilih = menu.nama
            hargaDasar = menu.harga
            b.txMenuTerpilih.text = "Dipilih: ${menu.nama} (Rp ${formatRupiah(menu.harga)})"
            Toast.makeText(requireContext(), "Menu dipilih: ${menu.nama}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatRupiah(nominal: Int): String {
        return "%,d".format(nominal).replace(',', '.')
    }

    private fun muatMenuDariFirestore() {
        dbFirestore.collection("menus")
            .addSnapshotListener { snapshot, _ ->
                listSemuaMenu.clear()

                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val nama = doc.getString("name") ?: "Menu"
                        val harga = (doc.getLong("price") ?: 0L).toInt()
                        val kategori = doc.getString("category") ?: "Martabak Telur"
                        listSemuaMenu.add(MenuModel(nama, harga, kategori))
                    }
                }

                // Jika Firestore menus masih kosong, sediakan menu default
                if (listSemuaMenu.isEmpty()) {
                    listSemuaMenu.add(MenuModel("Martabak Telur Spesial", 35000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Telur Daging Sapi", 40000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Terang Bulan Coklat Keju", 30000, "Terang Bulan"))
                    listSemuaMenu.add(MenuModel("Terang Bulan Red Velvet", 35000, "Terang Bulan"))
                    listSemuaMenu.add(MenuModel("Terang Bulan Pandan Jagung", 28000, "Terang Bulan"))
                    listSemuaMenu.add(MenuModel("Es Teh Manis Jumbo", 5000, "Minuman"))
                    listSemuaMenu.add(MenuModel("Es Jeruk Peras", 7000, "Minuman"))
                }

                perbaruiDataTampilan()
            }
    }

    private fun perbaruiDataTampilan() {
        if (!isAdded) return

        // Perbarui list nama untuk AutoCompleteTextView
        listNamaMenu.clear()
        for (m in listSemuaMenu) {
            listNamaMenu.add(m.nama)
        }

        val adapterAuto = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listNamaMenu)
        b.autoCariMenu.setAdapter(adapterAuto)

        // Filter listview sesuai spinner aktif
        val katTerpilih = b.spKategori.selectedItem?.toString() ?: "Semua Kategori"
        filterMenuBerdasarkanKategori(katTerpilih)
    }

    private fun filterMenuBerdasarkanKategori(kategori: String) {
        listMenuTampil.clear()
        for (m in listSemuaMenu) {
            if (kategori == "Semua Kategori" || m.kategori.equals(kategori, ignoreCase = true)) {
                listMenuTampil.add("${m.nama} - Rp ${formatRupiah(m.harga)}")
            }
        }

        if (isAdded) {
            val adapterList = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listMenuTampil)
            b.lsMenu.adapter = adapterList
        }
    }
}
