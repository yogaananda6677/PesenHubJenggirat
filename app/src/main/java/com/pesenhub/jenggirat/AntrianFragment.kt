package com.pesenhub.jenggirat

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.pesenhub.jenggirat.databinding.FragmentAntrianBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AntrianFragment : Fragment() {

    lateinit var b: FragmentAntrianBinding
    lateinit var dbFirestore: FirebaseFirestore
    lateinit var dbHelper: DBOpenHelper

    val listOrders = ArrayList<Map<String, Any?>>()
    var selectedOrderPos: Int = -1

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

        // Klik Item membuka Detail Pesanan & QR Code (Bab 10 PM)
        b.lsAntrian.setOnItemClickListener { _, _, position, _ ->
            if (position in listOrders.indices) {
                val order = listOrders[position]
                val intent = android.content.Intent(requireContext(), OrderDetailActivity::class.java).apply {
                    putExtra("EXTRA_ORDER_NUMBER", (order["orderNumber"] ?: order["idDoc"]).toString())
                    putExtra("EXTRA_CUSTOMER_NAME", (order["customerName"] ?: "-").toString())
                    putExtra("EXTRA_CUSTOMER_PHONE", (order["customerPhone"] ?: "-").toString())
                    putExtra("EXTRA_MENU_ITEM", (order["menuItem"] ?: "-").toString())
                    putExtra("EXTRA_PAYMENT_METHOD", (order["paymentMethod"] ?: "Tunai").toString())
                    putExtra("EXTRA_TOTAL", (order["total"] as? Number)?.toInt() ?: 0)
                    putExtra("EXTRA_STATUS", (order["status"] ?: "PENDING").toString())
                    putExtra("EXTRA_NOTES", (order["notes"] ?: "-").toString())
                }
                startActivity(intent)
            }
        }

        // 2. Pantau Antrean Realtime dari Cloud Firestore (PML Pertemuan 03)
        pantauAntrianRealtime()
    }

    private fun pantauAntrianRealtime() {
        // Ambil pesanan yang aktif (MASUK / PENDING dan DIPROSES / PREPARING)
        dbFirestore.collection("orders")
            .whereIn("status", listOf("PENDING", "PREPARING"))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    b.txInfoAntrian.text = "Gagal memuat antrean: ${e.message}"
                    return@addSnapshotListener
                }

                listOrders.clear()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["idDoc"] = doc.id
                        listOrders.add(data)
                    }
                    b.txKosong.visibility = View.GONE
                    b.lsAntrian.visibility = View.VISIBLE
                    b.txInfoAntrian.text = "Antrean Aktif Dapur: ${listOrders.size} Pesanan"
                } else {
                    b.txKosong.visibility = View.VISIBLE
                    b.lsAntrian.visibility = View.GONE
                    b.txInfoAntrian.text = "Antrean Aktif Dapur: 0 Pesanan"
                }

                if (isAdded) {
                    val adapter = AntrianAdapter(requireContext(), listOrders) { orderId, btnView ->
                        // Buka PopupMenu saat ikon titik tiga diklik
                        tampilkanPopupMenu(orderId, btnView)
                    }
                    b.lsAntrian.adapter = adapter
                }
            }
    }

    // 3. PopupMenu (Checklist #12) — Klik tombol titik tiga per item
    private fun tampilkanPopupMenu(orderId: String, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "👨‍🍳 Mulai Proses (DIPROSES)")
        popup.menu.add(0, 2, 1, "✅ Selesai Masak (SELESAI)")
        popup.menu.add(0, 3, 2, "❌ Batalkan Pesanan")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> updateStatusOrder(orderId, "PREPARING")
                2 -> updateStatusOrder(orderId, "COMPLETED")
                3 -> updateStatusOrder(orderId, "CANCELLED")
                else -> false
            }
        }
        popup.show()
    }

    // 4. ContextMenu (Checklist #11) — Saat item ditekan tahan (Long Click)
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as? AdapterView.AdapterContextMenuInfo
        selectedOrderPos = info?.position ?: -1

        if (selectedOrderPos in listOrders.indices) {
            val order = listOrders[selectedOrderPos]
            val orderNumber = order["orderNumber"] ?: order["idDoc"]
            menu.setHeaderTitle("Pesanan #$orderNumber")
            menu.add(0, 101, 0, "Mulai Proses (DIPROSES)")
            menu.add(0, 102, 1, "Selesai & Arsipkan (SELESAI)")
            menu.add(0, 103, 2, "Batalkan Pesanan")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (selectedOrderPos !in listOrders.indices) return super.onContextItemSelected(item)
        val order = listOrders[selectedOrderPos]
        val orderId = (order["orderNumber"] ?: order["idDoc"]).toString()

        return when (item.itemId) {
            101 -> updateStatusOrder(orderId, "PREPARING")
            102 -> updateStatusOrder(orderId, "COMPLETED")
            103 -> updateStatusOrder(orderId, "CANCELLED")
            else -> super.onContextItemSelected(item)
        }
    }

    // 5. Update Status Firestore + Arsipkan ke SQLite jika COMPLETED
    private fun updateStatusOrder(orderId: String, statusBaru: String): Boolean {
        val docRef = dbFirestore.collection("orders").document(orderId)

        docRef.update("status", statusBaru)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pesanan #$orderId diubah menjadi: $statusBaru", Toast.LENGTH_SHORT).show()

                // Jika status pesanan SELESAI (COMPLETED), simpan ke arsip riwayat SQLite (Checklist #17)
                if (statusBaru == "COMPLETED") {
                    SoundHelper.playSuccess()
                    arsipKeSQLite(orderId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengubah status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        return true
    }

    private fun arsipKeSQLite(orderId: String) {
        // Ambil data dari cache listOrders
        val order = listOrders.find { (it["orderNumber"] ?: it["idDoc"]) == orderId } ?: return
        val nama = order["customerName"]?.toString() ?: "-"
        val hp = order["customerPhone"]?.toString() ?: "-"
        val menu = order["menuItem"]?.toString() ?: "-"
        val metode = order["paymentMethod"]?.toString() ?: "Tunai"
        val total = (order["total"] as? Number)?.toInt() ?: 0
        val waktuSelesai = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        try {
            val dbLocal = dbHelper.writableDatabase
            val sqlInsert = "insert or replace into riwayat_transaksi(id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, status_order, waktu_selesai) " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?)"
            dbLocal.execSQL(sqlInsert, arrayOf(orderId, nama, hp, menu, metode, total, "COMPLETED", waktuSelesai))
            Toast.makeText(requireContext(), "Pesanan #$orderId telah diarsipkan ke riwayat SQLite lokal!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
