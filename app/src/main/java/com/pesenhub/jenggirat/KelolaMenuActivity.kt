package com.pesenhub.jenggirat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pesenhub.jenggirat.databinding.ActivityKelolaMenuBinding

class KelolaMenuActivity : AppCompatActivity() {

    lateinit var b: ActivityKelolaMenuBinding
    lateinit var dbFirestore: FirebaseFirestore
    private var menuListener: ListenerRegistration? = null

    private val listSemuaMenu = ArrayList<MenuKelolaModel>()
    private var kategoriTerpilih = "Semua"
    private var keywordCari = ""
    private var adapter: KelolaMenuAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityKelolaMenuBinding.inflate(layoutInflater)
        setContentView(b.root)

        dbFirestore = FirebaseFirestore.getInstance()

        b.toolbarKelolaMenu.setNavigationOnClickListener {
            finish()
        }

        // Setup RecyclerView
        b.rvKelolaMenu.layoutManager = LinearLayoutManager(this)
        adapter = KelolaMenuAdapter(
            context = this,
            listMenu = emptyList(),
            onToggleAvailable = { menu, isAvailable ->
                toggleKetersediaan(menu, isAvailable)
            },
            onEditClick = { menu ->
                val intent = Intent(this, FormMenuActivity::class.java).apply {
                    putExtra("IS_EDIT", true)
                    putExtra("SKU", menu.sku.ifEmpty { menu.id })
                    putExtra("NAMA", menu.name)
                    putExtra("KATEGORI", menu.category)
                    putExtra("DESKRIPSI", menu.description)
                    putExtra("HPP", menu.hppAmount)
                    putExtra("PRICE_OFFLINE", menu.priceForChannel("OFFLINE"))
                    putExtra("PRICE_WEB", menu.priceForChannel("CUSTOMER_WEB"))
                    putExtra("PRICE_GOFOOD", menu.priceForChannel("GOFOOD"))
                    putExtra("PRICE_GRABFOOD", menu.priceForChannel("GRABFOOD"))
                    putExtra("PRICE_SHOPEEFOOD", menu.priceForChannel("SHOPEEFOOD"))
                    putExtra("AVAILABLE", menu.available)
                }
                startActivity(intent)
            },
            onDeleteClick = { menu ->
                konfirmasiHapus(menu)
            }
        )
        b.rvKelolaMenu.adapter = adapter

        // Tombol Tambah Menu Header
        b.btnTambahMenuHeader.setOnClickListener {
            val intent = Intent(this, FormMenuActivity::class.java)
            startActivity(intent)
        }

        // Search Listener
        b.etSearchKelolaMenu.doAfterTextChanged { text ->
            keywordCari = text?.toString()?.trim() ?: ""
            filterDanTampilkan()
        }

        // Chips Kategori
        setupFilterChips()

        // Muat Data Realtime dari Firestore
        muatDataFirestore()
    }

    private fun setupFilterChips() {
        val chips = listOf(
            b.chipKatSemua to "Semua",
            b.chipKatMartabak to "Martabak Telur",
            b.chipKatTerangBulan to "Terang Bulan",
            b.chipKatMinuman to "Minuman"
        )

        chips.forEach { (view, kat) ->
            view.setOnClickListener {
                kategoriTerpilih = kat
                updateChipStyles(view, chips.map { it.first })
                filterDanTampilkan()
            }
        }
    }

    private fun updateChipStyles(activeView: TextView, allViews: List<TextView>) {
        allViews.forEach { v ->
            if (v == activeView) {
                v.setBackgroundResource(R.drawable.bg_chip_active)
                v.setTextColor(getColor(R.color.white))
            } else {
                v.setBackgroundResource(R.drawable.bg_chip_inactive)
                v.setTextColor(android.graphics.Color.parseColor("#475569"))
            }
        }
    }

    private fun muatDataFirestore() {
        b.pbKelolaMenu.visibility = View.VISIBLE

        menuListener = dbFirestore.collection("menus")
            .addSnapshotListener { snapshot, e ->
                b.pbKelolaMenu.visibility = View.GONE

                if (e != null) {
                    Toast.makeText(this, "Gagal memuat menu Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                listSemuaMenu.clear()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val sku = doc.getString("sku") ?: doc.id
                        val name = doc.getString("name") ?: "Menu"
                        val category = doc.getString("category") ?: "Martabak Telur"
                        val description = doc.getString("description") ?: ""
                        val price = doc.getLong("price") ?: 0L
                        val hpp = doc.getLong("hppAmount") ?: 0L
                        val available = doc.getBoolean("available") ?: true
                        val imageUrl = doc.getString("imageUrl") ?: "default_food_icon"

                        // Map channel prices
                        val rawChannels = doc.get("channelPrices") as? Map<*, *>
                        val channelMap = mutableMapOf<String, Long>()
                        if (rawChannels != null) {
                            for ((k, v) in rawChannels) {
                                val amount = (v as? Number)?.toLong() ?: price
                                channelMap[k.toString().uppercase()] = amount
                            }
                        }
                        if (!channelMap.containsKey("OFFLINE")) channelMap["OFFLINE"] = price
                        if (!channelMap.containsKey("CUSTOMER_WEB")) channelMap["CUSTOMER_WEB"] = price

                        listSemuaMenu.add(
                            MenuKelolaModel(
                                id = doc.id,
                                sku = sku,
                                name = name,
                                category = category,
                                description = description,
                                price = price,
                                hppAmount = hpp,
                                available = available,
                                imageUrl = imageUrl,
                                channelPrices = channelMap
                            )
                        )
                    }
                }

                filterDanTampilkan()
            }
    }

    private fun filterDanTampilkan() {
        var hasil = listSemuaMenu.toList()

        if (kategoriTerpilih != "Semua") {
            hasil = hasil.filter { it.category == kategoriTerpilih }
        }

        if (keywordCari.isNotEmpty()) {
            val kw = keywordCari.lowercase()
            hasil = hasil.filter {
                it.name.lowercase().contains(kw) ||
                it.sku.lowercase().contains(kw) ||
                it.description.lowercase().contains(kw)
            }
        }

        // Urutkan berdasarkan Kategori lalu Nama
        hasil = hasil.sortedWith(compareBy({ it.category }, { it.name }))

        adapter?.perbaruiData(hasil)

        if (hasil.isEmpty()) {
            b.layoutEmptyKelolaMenu.visibility = View.VISIBLE
            b.rvKelolaMenu.visibility = View.GONE
        } else {
            b.layoutEmptyKelolaMenu.visibility = View.GONE
            b.rvKelolaMenu.visibility = View.VISIBLE
        }
    }

    private fun toggleKetersediaan(menu: MenuKelolaModel, isAvailable: Boolean) {
        val docId = menu.sku.ifEmpty { menu.id }
        dbFirestore.collection("menus").document(docId)
            .update("available", isAvailable)
            .addOnSuccessListener {
                val statusText = if (isAvailable) "Tersedia" else "Habis"
                Toast.makeText(this, "Status '${menu.name}' diubah ke: $statusText", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengubah status: ${e.message}", Toast.LENGTH_SHORT).show()
                // Refresh list jika gagal
                filterDanTampilkan()
            }
    }

    private fun konfirmasiHapus(menu: MenuKelolaModel) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Menu")
            .setMessage("Yakin ingin menghapus menu '${menu.name}' (${menu.sku}) dari katalog Firestore?")
            .setPositiveButton("Hapus") { _, _ ->
                val docId = menu.sku.ifEmpty { menu.id }
                dbFirestore.collection("menus").document(docId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Menu '${menu.name}' berhasil dihapus dari Cloud Firestore!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        menuListener?.remove()
    }
}
