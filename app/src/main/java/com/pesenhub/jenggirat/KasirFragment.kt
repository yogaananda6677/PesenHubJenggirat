package com.pesenhub.jenggirat

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.pesenhub.jenggirat.databinding.FragmentKasirBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KasirFragment : Fragment() {

    lateinit var b: FragmentKasirBinding
    lateinit var dbFirestore: FirebaseFirestore

    // Data Menu Model
    data class MenuModel(
        val nama: String,
        val harga: Int,
        val kategori: String,
        val imageUrl: String = ""
    )

    private val listSemuaMenu = ArrayList<MenuModel>()
    private val listMenuTampil = ArrayList<MenuModel>()
    private lateinit var menuAdapter: MenuPosAdapter

    private var kategoriDipilih = "Semua"
    private var kataKunciPencarian = ""

    // Keranjang Pesanan Kasir
    private val listKeranjang = ArrayList<KeranjangItem>()
    private var estimasiJamAmbil = "Langsung (15-20 mnt)"
    private var metodeBayarTerpilih = "Tunai"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentKasirBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onResume() {
        super.onResume()
        // Sembunyikan ActionBar default agar header POS modern tampil maksimal
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbFirestore = FirebaseFirestore.getInstance()

        // 1. Inisialisasi RecyclerView Katalog Menu POS
        menuAdapter = MenuPosAdapter(listMenuTampil) { menu ->
            tampilkanBottomSheetPilihMenu(menu)
        }
        b.rvKatalogMenuPos.layoutManager = LinearLayoutManager(requireContext())
        b.rvKatalogMenuPos.adapter = menuAdapter

        // 2. Setup Pencarian Menu Real-time
        b.edtCariMenuPos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                kataKunciPencarian = s?.toString()?.trim() ?: ""
                b.btnClearSearch.visibility = if (kataKunciPencarian.isNotEmpty()) View.VISIBLE else View.GONE
                filterDanTampilkanMenu()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        b.btnClearSearch.setOnClickListener {
            b.edtCariMenuPos.setText("")
        }

        // 3. Setup Filter Kategori Horizontal Chips
        setupCategoryChips()

        // 4. Setup Floating Bottom Cart Bar Click
        b.btnFloatingLanjutBayar.setOnClickListener {
            if (listKeranjang.isNotEmpty()) {
                tampilkanBottomSheetKeranjang()
            } else {
                Toast.makeText(requireContext(), "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
            }
        }

        b.layoutFloatingCartBar.setOnClickListener {
            if (listKeranjang.isNotEmpty()) {
                tampilkanBottomSheetKeranjang()
            }
        }

        // Top Bar actions
        b.btnPosNotifikasi.setOnClickListener {
            Toast.makeText(requireContext(), "Tidak ada notifikasi baru.", Toast.LENGTH_SHORT).show()
        }

        b.btnPosProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Kasir Aktif: Jenggirat Kediri", Toast.LENGTH_SHORT).show()
        }

        // 5. Muat Data Menu dari Cloud Firestore
        muatMenuDariFirestore()

        // Perbarui Floating Bar awal
        perbaruiFloatingCartBar()
    }

    private fun setupCategoryChips() {
        b.chipKategoriSemua.setOnClickListener {
            pilihKategori("Semua")
        }
        b.chipKategoriMartabak.setOnClickListener {
            pilihKategori("Martabak Telur")
        }
        b.chipKategoriTerangBulan.setOnClickListener {
            pilihKategori("Terang Bulan")
        }
        b.chipKategoriMinuman.setOnClickListener {
            pilihKategori("Minuman")
        }
    }

    private fun pilihKategori(kategori: String) {
        kategoriDipilih = kategori

        val chips = listOf(
            b.chipKategoriSemua to "Semua",
            b.chipKategoriMartabak to "Martabak Telur",
            b.chipKategoriTerangBulan to "Terang Bulan",
            b.chipKategoriMinuman to "Minuman"
        )

        for ((chipView, namaKat) in chips) {
            if (namaKat == kategori) {
                chipView.setBackgroundResource(R.drawable.bg_chip_category_active)
                chipView.setTextColor(resources.getColor(R.color.white, null))
                chipView.paint.isFakeBoldText = true
            } else {
                chipView.setBackgroundResource(R.drawable.bg_chip_category_inactive)
                chipView.setTextColor(0xFF495057.toInt())
                chipView.paint.isFakeBoldText = false
            }
        }

        filterDanTampilkanMenu()
    }

    private fun filterDanTampilkanMenu() {
        listMenuTampil.clear()

        val keyword = kataKunciPencarian.lowercase(Locale.getDefault())

        for (menu in listSemuaMenu) {
            val lolosKategori = (kategoriDipilih == "Semua" || menu.kategori.equals(kategoriDipilih, ignoreCase = true))
            val lolosPencarian = keyword.isEmpty() || menu.nama.lowercase(Locale.getDefault()).contains(keyword)

            if (lolosKategori && lolosPencarian) {
                listMenuTampil.add(menu)
            }
        }

        menuAdapter.perbaruiData(listMenuTampil)

        if (listMenuTampil.isEmpty()) {
            b.layoutEmptyMenuPos.visibility = View.VISIBLE
            b.rvKatalogMenuPos.visibility = View.GONE
        } else {
            b.layoutEmptyMenuPos.visibility = View.GONE
            b.rvKatalogMenuPos.visibility = View.VISIBLE
        }
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
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        listSemuaMenu.add(MenuModel(nama, harga, kategori, imageUrl))
                    }
                }

                // Jika Firestore menus belum diisi, sediakan katalog otentik Jenggirat Kediri
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

                filterDanTampilkanMenu()
            }
    }

    private fun perbaruiFloatingCartBar() {
        if (!isAdded) return

        if (listKeranjang.isEmpty()) {
            b.layoutFloatingCartBar.visibility = View.GONE
        } else {
            b.layoutFloatingCartBar.visibility = View.VISIBLE
            val totalItem = listKeranjang.sumOf { it.qty }
            val totalHarga = listKeranjang.sumOf { it.subtotal }

            b.tvBadgeJumlahCart.text = "$totalItem"
            b.tvLabelItemTerpilih.text = "$totalItem item terpilih"
            b.tvTotalHargaFloating.text = "Rp " + formatRupiah(totalHarga)
        }
    }

    // =========================================================================
    // MODAL BOTTOMSHEET 1: KUSTOMISASI MENU & EXTRA ISIAN / TOPPING
    // =========================================================================
    private fun tampilkanBottomSheetPilihMenu(menu: MenuModel) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_pilih_menu, null)
        dialog.setContentView(sheetView)

        val tvNama = sheetView.findViewById<TextView>(R.id.tvSheetNamaMenu)
        val tvHargaDasar = sheetView.findViewById<TextView>(R.id.tvSheetHargaDasar)
        val btnTutup = sheetView.findViewById<ImageView>(R.id.btnSheetTutup)

        val tvQtyMozzarella = sheetView.findViewById<TextView>(R.id.tvQtyMozzarella)
        val btnMinusMozzarella = sheetView.findViewById<ImageButton>(R.id.btnMinusMozzarella)
        val btnPlusMozzarella = sheetView.findViewById<ImageButton>(R.id.btnPlusMozzarella)

        val tvQtySapi = sheetView.findViewById<TextView>(R.id.tvQtySapi)
        val btnMinusSapi = sheetView.findViewById<ImageButton>(R.id.btnMinusSapi)
        val btnPlusSapi = sheetView.findViewById<ImageButton>(R.id.btnPlusSapi)

        val tvQtyAyam = sheetView.findViewById<TextView>(R.id.tvQtyAyam)
        val btnMinusAyam = sheetView.findViewById<ImageButton>(R.id.btnMinusAyam)
        val btnPlusAyam = sheetView.findViewById<ImageButton>(R.id.btnPlusAyam)

        val tvQtyJamur = sheetView.findViewById<TextView>(R.id.tvQtyJamur)
        val btnMinusJamur = sheetView.findViewById<ImageButton>(R.id.btnMinusJamur)
        val btnPlusJamur = sheetView.findViewById<ImageButton>(R.id.btnPlusJamur)

        val tvQtySosis = sheetView.findViewById<TextView>(R.id.tvQtySosis)
        val btnMinusSosis = sheetView.findViewById<ImageButton>(R.id.btnMinusSosis)
        val btnPlusSosis = sheetView.findViewById<ImageButton>(R.id.btnPlusSosis)

        val tvQtyCoklat = sheetView.findViewById<TextView>(R.id.tvQtyCoklat)
        val btnMinusCoklat = sheetView.findViewById<ImageButton>(R.id.btnMinusCoklat)
        val btnPlusCoklat = sheetView.findViewById<ImageButton>(R.id.btnPlusCoklat)

        val tvQtySambal = sheetView.findViewById<TextView>(R.id.tvQtySambal)
        val btnMinusSambal = sheetView.findViewById<ImageButton>(R.id.btnMinusSambal)
        val btnPlusSambal = sheetView.findViewById<ImageButton>(R.id.btnPlusSambal)

        val tvQtyPorsi = sheetView.findViewById<TextView>(R.id.tvQtyPorsi)
        val btnMinusPorsi = sheetView.findViewById<ImageButton>(R.id.btnMinusPorsi)
        val btnPlusPorsi = sheetView.findViewById<ImageButton>(R.id.btnPlusPorsi)

        val tvTotalDinamis = sheetView.findViewById<TextView>(R.id.tvSheetTotalDinamis)
        val btnTambahPesanan = sheetView.findViewById<MaterialButton>(R.id.btnSheetTambahPesanan)

        tvNama.text = menu.nama
        tvHargaDasar.text = "Harga Dasar: Rp " + formatRupiah(menu.harga)

        var qMozzarella = 0
        var qSapi = 0
        var qAyam = 0
        var qJamur = 0
        var qSosis = 0
        var qCoklat = 0
        var qSambal = 0
        var porsiUtama = 1

        fun kalkulasiDanTampilkanTotal() {
            val totalExtraSatuan = (qMozzarella * 15000) +
                    (qSapi * 7000) +
                    (qAyam * 5000) +
                    (qJamur * 5000) +
                    (qSosis * 5000) +
                    (qCoklat * 4000) +
                    (qSambal * 4000)

            val totalPerPorsi = menu.harga + totalExtraSatuan
            val grandTotalItem = totalPerPorsi * porsiUtama
            tvTotalDinamis.text = "Rp " + formatRupiah(grandTotalItem)
        }

        kalkulasiDanTampilkanTotal()

        btnTutup.setOnClickListener { dialog.dismiss() }

        // Setup Stepper Toppings
        btnMinusMozzarella.setOnClickListener {
            if (qMozzarella > 0) { qMozzarella--; tvQtyMozzarella.text = "$qMozzarella"; kalkulasiDanTampilkanTotal() }
        }
        btnPlusMozzarella.setOnClickListener {
            qMozzarella++; tvQtyMozzarella.text = "$qMozzarella"; kalkulasiDanTampilkanTotal()
        }

        btnMinusSapi.setOnClickListener {
            if (qSapi > 0) { qSapi--; tvQtySapi.text = "$qSapi"; kalkulasiDanTampilkanTotal() }
        }
        btnPlusSapi.setOnClickListener {
            qSapi++; tvQtySapi.text = "$qSapi"; kalkulasiDanTampilkanTotal()
        }

        btnMinusAyam.setOnClickListener {
            if (qAyam > 0) { qAyam--; tvQtyAyam.text = "$qAyam"; kalkulasiDanTampilkanTotal() }
        }
        btnPlusAyam.setOnClickListener {
            qAyam++; tvQtyAyam.text = "$qAyam"; kalkulasiDanTampilkanTotal()
        }

        btnMinusJamur.setOnClickListener {
            if (qJamur > 0) { qJamur--; tvQtyJamur.text = "$qJamur"; kalkulasiDanTampilkanTotal() }
        }
        btnPlusJamur.setOnClickListener {
            qJamur++; tvQtyJamur.text = "$qJamur"; kalkulasiDanTampilkanTotal()
        }

        btnMinusSosis.setOnClickListener {
            if (qSosis > 0) { qSosis--; tvQtySosis.text = "$qSosis"; kalkulasiDanTampilkanTotal() }
        }
        btnPlusSosis.setOnClickListener {
            qSosis++; tvQtySosis.text = "$qSosis"; kalkulasiDanTampilkanTotal()
        }

        btnMinusCoklat.setOnClickListener {
            if (qCoklat > 0) { qCoklat--; tvQtyCoklat.text = "$qCoklat"; kalkulasiDanTampilkanTotal() }
        }
        btnPlusCoklat.setOnClickListener {
            qCoklat++; tvQtyCoklat.text = "$qCoklat"; kalkulasiDanTampilkanTotal()
        }

        btnMinusSambal.setOnClickListener {
            if (qSambal > 0) { qSambal--; tvQtySambal.text = "$qSambal"; kalkulasiDanTampilkanTotal() }
        }
        btnPlusSambal.setOnClickListener {
            qSambal++; tvQtySambal.text = "$qSambal"; kalkulasiDanTampilkanTotal()
        }

        // Stepper Porsi Menu Utama
        btnMinusPorsi.setOnClickListener {
            if (porsiUtama > 1) {
                porsiUtama--
                tvQtyPorsi.text = "$porsiUtama"
                kalkulasiDanTampilkanTotal()
            }
        }
        btnPlusPorsi.setOnClickListener {
            porsiUtama++
            tvQtyPorsi.text = "$porsiUtama"
            kalkulasiDanTampilkanTotal()
        }

        // Tombol Tambah ke Pesanan
        btnTambahPesanan.setOnClickListener {
            val listToppingItem = ArrayList<String>()
            var extraHargaSatuan = 0

            if (qMozzarella > 0) {
                listToppingItem.add(if (qMozzarella == 1) "Keju Mozzarella" else "Keju Mozzarella ($qMozzarella)")
                extraHargaSatuan += (qMozzarella * 15000)
            }
            if (qSapi > 0) {
                listToppingItem.add(if (qSapi == 1) "Daging Sapi" else "Daging Sapi ($qSapi)")
                extraHargaSatuan += (qSapi * 7000)
            }
            if (qAyam > 0) {
                listToppingItem.add(if (qAyam == 1) "Daging Ayam" else "Daging Ayam ($qAyam)")
                extraHargaSatuan += (qAyam * 5000)
            }
            if (qJamur > 0) {
                listToppingItem.add(if (qJamur == 1) "Jamur Tiram" else "Jamur Tiram ($qJamur)")
                extraHargaSatuan += (qJamur * 5000)
            }
            if (qSosis > 0) {
                listToppingItem.add(if (qSosis == 1) "Sosis Sapi" else "Sosis Sapi ($qSosis)")
                extraHargaSatuan += (qSosis * 5000)
            }
            if (qCoklat > 0) {
                listToppingItem.add(if (qCoklat == 1) "Coklat Meses" else "Coklat Meses ($qCoklat)")
                extraHargaSatuan += (qCoklat * 4000)
            }
            if (qSambal > 0) {
                listToppingItem.add(if (qSambal == 1) "Sambal Uleg" else "Sambal Uleg ($qSambal)")
                extraHargaSatuan += (qSambal * 4000)
            }

            // Cek apakah item dengan komposisi persis sama sudah ada di keranjang
            val itemEksis = listKeranjang.find {
                it.namaMenu == menu.nama && it.topping == listToppingItem
            }

            if (itemEksis != null) {
                itemEksis.qty += porsiUtama
            } else {
                listKeranjang.add(
                    KeranjangItem(
                        namaMenu = menu.nama,
                        hargaDasar = menu.harga,
                        topping = listToppingItem,
                        hargaTopping = extraHargaSatuan,
                        qty = porsiUtama
                    )
                )
            }

            perbaruiFloatingCartBar()
            Toast.makeText(
                requireContext(),
                "$porsiUtama x ${menu.nama} ditambahkan ke keranjang",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // =========================================================================
    // MODAL BOTTOMSHEET 2: CHECKOUT & PROSES PEMBAYARAN PESANAN
    // =========================================================================
    private fun tampilkanBottomSheetKeranjang() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_keranjang, null)
        dialog.setContentView(sheetView)

        val tvJudul = sheetView.findViewById<TextView>(R.id.tvCheckoutJudulKeranjang)
        val btnTutup = sheetView.findViewById<ImageView>(R.id.btnCheckoutTutup)
        val btnKosongkan = sheetView.findViewById<ImageButton>(R.id.btnCheckoutKosongkan)

        val edtNama = sheetView.findViewById<EditText>(R.id.edtCheckoutNama)
        val edtHp = sheetView.findViewById<EditText>(R.id.edtCheckoutHp)

        val containerItems = sheetView.findViewById<ViewGroup>(R.id.containerCheckoutItems)
        val tvKosongInfo = sheetView.findViewById<TextView>(R.id.tvCheckoutKosongInfo)

        val rgBayar = sheetView.findViewById<RadioGroup>(R.id.rgCheckoutBayar)
        val btnAturJam = sheetView.findViewById<android.widget.Button>(R.id.btnCheckoutJam)
        val tvJamInfo = sheetView.findViewById<TextView>(R.id.tvCheckoutJamInfo)
        val edtCatatan = sheetView.findViewById<EditText>(R.id.edtCheckoutCatatan)

        val tvTotalNominal = sheetView.findViewById<TextView>(R.id.tvCheckoutTotalNominal)
        val btnProses = sheetView.findViewById<MaterialButton>(R.id.btnCheckoutProses)

        tvJamInfo.text = estimasiJamAmbil

        fun muatDaftarItemKeranjangModal() {
            containerItems.removeAllViews()

            if (listKeranjang.isEmpty()) {
                tvKosongInfo.visibility = View.VISIBLE
                tvJudul.text = "Keranjang Pesanan (0)"
                tvTotalNominal.text = "Rp 0"
                perbaruiFloatingCartBar()
                return
            }

            tvKosongInfo.visibility = View.GONE
            val totalItem = listKeranjang.sumOf { it.qty }
            val grandTotal = listKeranjang.sumOf { it.subtotal }

            tvJudul.text = "Keranjang Pesanan ($totalItem)"
            tvTotalNominal.text = "Rp " + formatRupiah(grandTotal)
            perbaruiFloatingCartBar()

            val inflater = LayoutInflater.from(requireContext())

            for ((index, item) in listKeranjang.withIndex()) {
                val itemView = inflater.inflate(R.layout.item_keranjang, containerItems, false)

                val tvNamaItem = itemView.findViewById<TextView>(R.id.tvNamaItemKeranjang)
                val tvHargaSatuan = itemView.findViewById<TextView>(R.id.tvHargaSatuanKeranjang)
                val tvSubtotal = itemView.findViewById<TextView>(R.id.tvSubtotalKeranjang)
                val tvQty = itemView.findViewById<TextView>(R.id.tvQtyItemKeranjang)
                val btnMinus = itemView.findViewById<ImageButton>(R.id.btnMinusQty)
                val btnPlus = itemView.findViewById<ImageButton>(R.id.btnPlusQty)
                val btnHapus = itemView.findViewById<ImageButton>(R.id.btnHapusItemKeranjang)

                tvNamaItem.text = item.getDeskripsiLengkap()
                tvHargaSatuan.text = "@ Rp ${formatRupiah(item.hargaSatuan)}"
                tvSubtotal.text = "Rp ${formatRupiah(item.subtotal)}"
                tvQty.text = "${item.qty}"

                btnMinus.setOnClickListener {
                    if (item.qty > 1) {
                        item.qty--
                    } else {
                        listKeranjang.removeAt(index)
                    }
                    muatDaftarItemKeranjangModal()
                }

                btnPlus.setOnClickListener {
                    item.qty++
                    muatDaftarItemKeranjangModal()
                }

                btnHapus.setOnClickListener {
                    listKeranjang.removeAt(index)
                    muatDaftarItemKeranjangModal()
                }

                containerItems.addView(itemView)
            }
        }

        muatDaftarItemKeranjangModal()

        btnTutup.setOnClickListener { dialog.dismiss() }

        btnKosongkan.setOnClickListener {
            listKeranjang.clear()
            muatDaftarItemKeranjangModal()
            perbaruiFloatingCartBar()
            Toast.makeText(requireContext(), "Keranjang berhasil dikosongkan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Metode Pembayaran
        rgBayar.setOnCheckedChangeListener { _, checkedId ->
            metodeBayarTerpilih = if (checkedId == R.id.rbCheckoutQris) "QRIS" else "Tunai"
        }

        // TimePickerDialog Jam Ambil
        btnAturJam.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val strH = if (hourOfDay < 10) "0$hourOfDay" else "$hourOfDay"
                    val strM = if (minute < 10) "0$minute" else "$minute"
                    estimasiJamAmbil = "Jam $strH:$strM"
                    tvJamInfo.text = estimasiJamAmbil
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Tombol Review & Proses Pesanan
        btnProses.setOnClickListener {
            val nama = edtNama.text.toString().trim()
            val hp = edtHp.text.toString().trim()
            val catatan = edtCatatan.text.toString().trim()

            if (nama.isEmpty()) {
                edtNama.error = "Nama pelanggan wajib diisi"
                edtNama.requestFocus()
                return@setOnClickListener
            }

            if (listKeranjang.isEmpty()) {
                Toast.makeText(requireContext(), "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val total = listKeranjang.sumOf { it.subtotal }
            val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            val idPesanan = "ORD-${sdf.format(Date())}"

            // Ringkasan detail teks
            val detailItem = listKeranjang.joinToString("\n") {
                "${it.qty}x ${it.getDeskripsiLengkap()} - Rp ${formatRupiah(it.subtotal)}"
            }

            // Struktur items JSON untuk Firestore
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
                "paymentMethod" to metodeBayarTerpilih,
                "total" to total,
                "notes" to if (catatan.isNotEmpty()) "$catatan (Siap: $estimasiJamAmbil)" else "(Siap: $estimasiJamAmbil)",
                "status" to "PENDING",
                "source" to "CASHIER",
                "branchName" to "Jenggirat Kediri",
                "branchId" to "kediri",
                "createdAt" to Timestamp.now()
            )

            // Simpan langsung ke Firestore
            dbFirestore.collection("orders").document(idPesanan)
                .set(dataFirestore)
                .addOnSuccessListener {
                    SoundHelper.playBell()
                    Toast.makeText(requireContext(), "Pesanan $idPesanan berhasil diproses!", Toast.LENGTH_SHORT).show()

                    // Buka OrderDetailActivity untuk menampilkan QR Code struk pesanan
                    val intentDetail = Intent(requireContext(), OrderDetailActivity::class.java).apply {
                        putExtra("EXTRA_ORDER_NUMBER", idPesanan)
                        putExtra("EXTRA_CUSTOMER_NAME", nama)
                        putExtra("EXTRA_CUSTOMER_PHONE", hp)
                        putExtra("EXTRA_MENU_ITEM", detailItem)
                        putExtra("EXTRA_PAYMENT_METHOD", metodeBayarTerpilih)
                        putExtra("EXTRA_TOTAL", total)
                        putExtra("EXTRA_STATUS", "PENDING")
                        putExtra("EXTRA_NOTES", if (catatan.isNotEmpty()) "$catatan (Siap: $estimasiJamAmbil)" else "(Siap: $estimasiJamAmbil)")
                    }
                    startActivity(intentDetail)

                    // Reset keranjang
                    listKeranjang.clear()
                    perbaruiFloatingCartBar()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal simpan pesanan: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        dialog.show()
    }

    private fun formatRupiah(nominal: Int): String {
        return "%,d".format(nominal).replace(',', '.')
    }
}
