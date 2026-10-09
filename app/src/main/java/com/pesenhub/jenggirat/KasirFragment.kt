package com.pesenhub.jenggirat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
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

    var menuTerpilih = "Martabak Sosis/Jamur Biasa"
    var hargaDasar = 20000
    var qtyPilih = 1
    var metodeBayar = "Tunai"

    // Keranjang Belanja (Order Cart)
    val listKeranjang = ArrayList<KeranjangItem>()

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

        // 5. Kontrol Kuantitas Pemilihan Menu
        b.btnKurangQtyPilih.setOnClickListener {
            if (qtyPilih > 1) {
                qtyPilih--
                b.txQtyPilih.text = "$qtyPilih"
            }
        }

        b.btnTambahQtyPilih.setOnClickListener {
            qtyPilih++
            b.txQtyPilih.text = "$qtyPilih"
        }

        // 6. Tombol Tambah ke Keranjang
        b.btnTambahKeKeranjang.setOnClickListener {
            tambahMenuKeKeranjang()
        }

        // 7. Tombol Kosongkan Keranjang
        b.btnKosongkanKeranjang.setOnClickListener {
            listKeranjang.clear()
            perbaruiTampilanKeranjang()
            Toast.makeText(requireContext(), "Keranjang berhasil dikosongkan", Toast.LENGTH_SHORT).show()
        }

        // 8. RadioButton Pembayaran (Bab II Modul PM Pak Benni)
        b.rgBayar.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                b.rbTunai.id -> metodeBayar = "Tunai"
                b.rbQris.id -> metodeBayar = "QRIS"
            }
        }

        // 9. TimePickerDialog Estimasi Jam Ambil (Bab III Modul PM Pak Benni)
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

        // 10. Simpan Pesanan ke Cloud Firestore sebagai Basis Data Utama
        b.btnSimpanPesanan.setOnClickListener {
            val nama = b.edtNamaPelanggan.text.toString().trim()
            val hp = b.edtHpPelanggan.text.toString().trim()
            val catatan = b.edtCatatan.text.toString().trim()

            if (nama.isEmpty()) {
                b.edtNamaPelanggan.error = "Nama pelanggan wajib diisi"
                b.edtNamaPelanggan.requestFocus()
                return@setOnClickListener
            }

            if (listKeranjang.isEmpty()) {
                Toast.makeText(requireContext(), "Keranjang masih kosong! Silakan tambah menu ke keranjang terlebih dahulu.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val total = listKeranjang.sumOf { it.subtotal }
            val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            val idPesanan = "ORD-${sdf.format(Date())}"

            // Ringkasan multi-item untuk tampilan struk QR dan antrean
            val detailItem = listKeranjang.joinToString("\n") {
                "${it.qty}x ${it.getDeskripsiLengkap()} - Rp ${formatRupiah(it.subtotal)}"
            }

            // Data item terstruktur untuk Firestore
            val itemsFirestore = listKeranjang.map {
                hashMapOf(
                    "name" to it.namaMenu,
                    "unitPrice" to it.hargaSatuan,
                    "quantity" to it.qty,
                    "subtotal" to it.subtotal,
                    "toppings" to it.topping
                )
            }

            val dataFirestore = hashMapOf(
                "orderNumber" to idPesanan,
                "customerName" to nama,
                "customerPhone" to hp,
                "menuItem" to detailItem,
                "detailItem" to detailItem,
                "items" to itemsFirestore,
                "totalItems" to listKeranjang.sumOf { it.qty },
                "paymentMethod" to metodeBayar,
                "total" to total,
                "notes" to "$catatan (Siap: $estimasiJamAmbil)",
                "status" to "PENDING",
                "source" to "CASHIER",
                "branchName" to "Jenggirat Kediri",
                "branchId" to "kediri",
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

                    // Reset form dan kosongkan keranjang
                    b.edtNamaPelanggan.setText("")
                    b.edtHpPelanggan.setText("")
                    b.edtCatatan.setText("")
                    b.autoCariMenu.setText("")
                    b.cbKeju.isChecked = false
                    b.cbCoklat.isChecked = false
                    b.cbPedas.isChecked = false
                    qtyPilih = 1
                    b.txQtyPilih.text = "1"
                    listKeranjang.clear()
                    perbaruiTampilanKeranjang()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal simpan ke Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Tampilan keranjang awal
        perbaruiTampilanKeranjang()
    }

    private fun tambahMenuKeKeranjang() {
        val topping = ArrayList<String>()
        var hargaTopping = 0
        if (b.cbKeju.isChecked) { hargaTopping += 5000; topping.add("Keju") }
        if (b.cbCoklat.isChecked) { hargaTopping += 4000; topping.add("Coklat") }
        if (b.cbPedas.isChecked) { topping.add("Pedas Lvl 2") }

        // Cek apakah item dengan nama dan kombinasi topping yang sama sudah ada di keranjang
        val itemSama = listKeranjang.find {
            it.namaMenu == menuTerpilih && it.topping == topping
        }

        if (itemSama != null) {
            itemSama.qty += qtyPilih
        } else {
            listKeranjang.add(KeranjangItem(menuTerpilih, hargaDasar, topping, hargaTopping, qtyPilih))
        }

        Toast.makeText(
            requireContext(),
            "$qtyPilih x $menuTerpilih ditambahkan ke keranjang",
            Toast.LENGTH_SHORT
        ).show()

        // Reset kontrol topping dan qty
        qtyPilih = 1
        b.txQtyPilih.text = "1"
        b.cbKeju.isChecked = false
        b.cbCoklat.isChecked = false
        b.cbPedas.isChecked = false

        perbaruiTampilanKeranjang()
    }

    private fun perbaruiTampilanKeranjang() {
        if (!isAdded) return

        b.containerKeranjang.removeAllViews()

        if (listKeranjang.isEmpty()) {
            b.txKeranjangKosong.visibility = View.VISIBLE
            b.containerKeranjang.visibility = View.GONE
            b.layoutTotalKeranjang.visibility = View.GONE
            b.btnKosongkanKeranjang.visibility = View.GONE
            b.txJudulKeranjang.text = "Keranjang Pesanan (0 item)"
        } else {
            b.txKeranjangKosong.visibility = View.GONE
            b.containerKeranjang.visibility = View.VISIBLE
            b.layoutTotalKeranjang.visibility = View.VISIBLE
            b.btnKosongkanKeranjang.visibility = View.VISIBLE

            val totalItem = listKeranjang.sumOf { it.qty }
            val totalBelanja = listKeranjang.sumOf { it.subtotal }

            b.txJudulKeranjang.text = "Keranjang Pesanan ($totalItem item)"
            b.txTotalBelanja.text = "Rp " + formatRupiah(totalBelanja)

            val inflater = LayoutInflater.from(requireContext())

            for ((index, item) in listKeranjang.withIndex()) {
                val itemView = inflater.inflate(R.layout.item_keranjang, b.containerKeranjang, false)

                val tvNama = itemView.findViewById<TextView>(R.id.tvNamaItemKeranjang)
                val tvHargaSatuan = itemView.findViewById<TextView>(R.id.tvHargaSatuanKeranjang)
                val tvSubtotal = itemView.findViewById<TextView>(R.id.tvSubtotalKeranjang)
                val tvQty = itemView.findViewById<TextView>(R.id.tvQtyItemKeranjang)
                val btnMinus = itemView.findViewById<Button>(R.id.btnMinusQty)
                val btnPlus = itemView.findViewById<Button>(R.id.btnPlusQty)
                val btnHapus = itemView.findViewById<Button>(R.id.btnHapusItemKeranjang)

                tvNama.text = item.getDeskripsiLengkap()
                tvHargaSatuan.text = "@ Rp ${formatRupiah(item.hargaSatuan)}"
                tvSubtotal.text = "Subtotal: Rp ${formatRupiah(item.subtotal)}"
                tvQty.text = "${item.qty}"

                btnMinus.setOnClickListener {
                    if (item.qty > 1) {
                        item.qty--
                    } else {
                        listKeranjang.removeAt(index)
                    }
                    perbaruiTampilanKeranjang()
                }

                btnPlus.setOnClickListener {
                    item.qty++
                    perbaruiTampilanKeranjang()
                }

                btnHapus.setOnClickListener {
                    listKeranjang.removeAt(index)
                    perbaruiTampilanKeranjang()
                }

                b.containerKeranjang.addView(itemView)
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

                // Jika Firestore menus masih kosong, sediakan menu default otentik Jenggirat dari PTT
                if (listSemuaMenu.isEmpty()) {
                    listSemuaMenu.add(MenuModel("Martabak Sosis/Jamur Biasa", 20000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Sosis/Jamur Spesial", 30000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Daging Ayam Biasa", 25000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Daging Ayam Spesial", 35000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Daging Sapi Biasa", 30000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Daging Sapi Spesial", 40000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Daging Sapi Istimewa", 50000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Mozarella 1 Isian", 50000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Martabak Mozarella Mix 2", 55000, "Martabak Telur"))
                    listSemuaMenu.add(MenuModel("Terang Bulan 1 Toping Biasa", 18000, "Terang Bulan"))
                    listSemuaMenu.add(MenuModel("Terang Bulan 1 Toping Besar", 25000, "Terang Bulan"))
                    listSemuaMenu.add(MenuModel("Terang Bulan 2 Toping Biasa", 23000, "Terang Bulan"))
                    listSemuaMenu.add(MenuModel("Terang Bulan 2 Toping Besar", 30000, "Terang Bulan"))
                    listSemuaMenu.add(MenuModel("Terang Bulan Cut Pizza All In One", 45000, "Terang Bulan"))
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
