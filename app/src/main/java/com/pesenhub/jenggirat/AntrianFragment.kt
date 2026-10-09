package com.pesenhub.jenggirat

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pesenhub.jenggirat.databinding.FragmentAntrianBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AntrianFragment : Fragment() {

    lateinit var b: FragmentAntrianBinding
    lateinit var dbFirestore: FirebaseFirestore
    lateinit var dbHelper: DBOpenHelper

    private var adapter: AntrianAdapter? = null
    private val listOrders = ArrayList<Map<String, Any?>>()
    private var orderListener: ListenerRegistration? = null

    // Menyimpan snapshot pesanan yang dipilih agar tidak berubah saat Firestore update realtime
    private var selectedOrderData: Map<String, Any?>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentAntrianBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbFirestore = FirebaseFirestore.getInstance()
        dbHelper = DBOpenHelper(requireContext())

        // 1. Daftarkan ContextMenu pada ListView (Bab 05 & Bab 08 PM)
        registerForContextMenu(b.lsAntrian)
    }

    override fun onStart() {
        super.onStart()
        // 2. Pantau Antrean Realtime dari Cloud Firestore saat halaman aktif
        pantauAntrianRealtime()
    }

    override fun onStop() {
        super.onStop()
        // 6. Lepaskan listener Firestore saat halaman tidak aktif
        orderListener?.remove()
        orderListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orderListener?.remove()
        orderListener = null
        adapter = null
    }

    private fun pantauAntrianRealtime() {
        // Hapus listener sebelumnya jika ada
        orderListener?.remove()

        // Ambil pesanan yang aktif (MASUK / PENDING dan DIPROSES / PREPARING)
        orderListener = dbFirestore.collection("orders")
            .whereIn("status", listOf("PENDING", "PREPARING"))
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener

                if (e != null) {
                    b.tvInfoAntrian.text = "Gagal memuat antrean: ${e.message}"
                    return@addSnapshotListener
                }

                val orders = ArrayList<Map<String, Any?>>()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        // 5. Simpan ID Dokumen Firestore untuk update
                        data["idDoc"] = doc.id
                        orders.add(data)
                    }

                    // Urutkan berdasarkan waktu buat (FIFO dapur: pesanan terlama diproses lebih awal)
                    orders.sortBy {
                        (it["createdAt"] as? Timestamp)?.toDate()?.time ?: Long.MAX_VALUE
                    }

                    listOrders.clear()
                    listOrders.addAll(orders)

                    b.tvKosong.visibility = View.GONE
                    b.lsAntrian.visibility = View.VISIBLE
                    b.tvInfoAntrian.text = "Antrean Aktif Dapur: ${listOrders.size} Pesanan"
                } else {
                    listOrders.clear()
                    b.tvKosong.visibility = View.VISIBLE
                    b.lsAntrian.visibility = View.GONE
                    b.tvInfoAntrian.text = "Antrean Aktif Dapur: 0 Pesanan"
                }

                if (adapter == null) {
                    adapter = AntrianAdapter(requireContext(), listOrders) { order, btnView ->
                        // 3. Buka PopupMenu saat tombol opsi per item diklik
                        tampilkanPopupMenu(order, btnView)
                    }
                    b.lsAntrian.adapter = adapter
                } else {
                    adapter?.updateData(listOrders)
                }
            }
    }

    // 3. PopupMenu (Checklist #12) — Klik tombol opsi (titik tiga) per pesanan
    private fun tampilkanPopupMenu(order: Map<String, Any?>, view: View) {
        val docId = order["idDoc"]?.toString() ?: return
        val orderNumber = order["orderNumber"]?.toString() ?: docId

        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "👨‍🍳 Mulai Proses (PREPARING)")
        popup.menu.add(0, 2, 1, "✅ Selesai (COMPLETED)")
        popup.menu.add(0, 3, 2, "❌ Batalkan Pesanan (CANCELLED)")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> updateStatusOrder(docId, order, "PREPARING")
                2 -> updateStatusOrder(docId, order, "COMPLETED")
                3 -> updateStatusOrder(docId, order, "CANCELLED")
                else -> false
            }
        }
        popup.show()
    }

    // 2. ContextMenu (Checklist #11) — Saat item ditekan lama (Long Click)
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as? AdapterView.AdapterContextMenuInfo
        val position = info?.position ?: -1

        // 5. Kunci data pesanan yang dipilih agar tidak berubah saat realtime update
        if (position in listOrders.indices) {
            selectedOrderData = listOrders[position]
            val order = selectedOrderData ?: return
            val orderNumber = order["orderNumber"]?.toString() ?: order["idDoc"]?.toString() ?: "-"
            menu.setHeaderTitle("Pesanan #$orderNumber")
            menu.add(0, 101, 0, "👨‍🍳 Mulai Proses (PREPARING)")
            menu.add(0, 102, 1, "✅ Selesai (COMPLETED)")
            menu.add(0, 103, 2, "❌ Batalkan Pesanan (CANCELLED)")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        // 5. Ambil dari selectedOrderData yang sudah terkunci
        val order = selectedOrderData ?: return super.onContextItemSelected(item)
        val docId = order["idDoc"]?.toString() ?: return super.onContextItemSelected(item)

        val res = when (item.itemId) {
            101 -> updateStatusOrder(docId, order, "PREPARING")
            102 -> updateStatusOrder(docId, order, "COMPLETED")
            103 -> updateStatusOrder(docId, order, "CANCELLED")
            else -> super.onContextItemSelected(item)
        }
        selectedOrderData = null
        return res
    }


    // 4. Update Status Firestore + 7. Arsipkan ke SQLite jika COMPLETED
    private fun updateStatusOrder(docId: String, order: Map<String, Any?>, statusBaru: String): Boolean {
        val orderNumber = order["orderNumber"]?.toString() ?: docId

        // 5. Gunakan ID dokumen Firestore untuk update
        dbFirestore.collection("orders").document(docId)
            .update("status", statusBaru)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Pesanan #$orderNumber diubah menjadi: $statusBaru",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 7. Jika status pesanan SELESAI (COMPLETED), simpan ke arsip riwayat SQLite (Checklist #17)
                if (statusBaru == "COMPLETED") {
                    arsipKeSQLite(order, docId)
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal mengubah status: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        return true
    }

    private fun arsipKeSQLite(order: Map<String, Any?>, docId: String) {
        val idPesanan = (order["orderNumber"] ?: docId).toString()
        val nama = order["customerName"]?.toString() ?: "-"
        val hp = order["customerPhone"]?.toString() ?: "-"
        val menu = (order["menuItem"] ?: order["detailItem"])?.toString() ?: "-"
        val metode = order["paymentMethod"]?.toString() ?: "Tunai"
        val total = (order["total"] as? Number)?.toLong() ?: 0L
        val waktuSelesai = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        try {
            val dbLocal = dbHelper.writableDatabase
            val sqlInsert = "insert or replace into riwayat_transaksi(" +
                    "id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, status_order, waktu_selesai) " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?)"
            dbLocal.execSQL(
                sqlInsert,
                arrayOf(idPesanan, nama, hp, menu, metode, total, "COMPLETED", waktuSelesai)
            )
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Pesanan #$idPesanan telah diarsipkan ke riwayat SQLite lokal!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

