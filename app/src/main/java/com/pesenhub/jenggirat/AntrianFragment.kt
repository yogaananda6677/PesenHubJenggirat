package com.pesenhub.jenggirat

import android.content.Intent
import android.graphics.Bitmap
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.pesenhub.jenggirat.databinding.FragmentAntrianBinding
import java.io.ByteArrayOutputStream
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

        // Klik Item membuka Detail Pesanan & QR Code (Bab 10 PM)
        b.lsAntrian.setOnItemClickListener { _, _, position, _ ->
            if (position in listOrders.indices) {
                val order = listOrders[position]
                val intent = Intent(requireContext(), OrderDetailActivity::class.java).apply {
                    putExtra("EXTRA_DOC_ID", (order["idDoc"] ?: order["orderNumber"]).toString())
                    putExtra("EXTRA_ORDER_NUMBER", (order["orderNumber"] ?: order["idDoc"]).toString())
                    putExtra("EXTRA_CUSTOMER_NAME", (order["customerName"] ?: "-").toString())
                    putExtra("EXTRA_CUSTOMER_PHONE", (order["customerPhone"] ?: "-").toString())
                    putExtra("EXTRA_MENU_ITEM", (order["menuItem"] ?: order["detailItem"] ?: "-").toString())
                    putExtra("EXTRA_PAYMENT_METHOD", (order["paymentMethod"] ?: "Tunai").toString())
                    putExtra("EXTRA_TOTAL", (order["total"] as? Number)?.toInt() ?: 0)
                    putExtra("EXTRA_STATUS", (order["status"] ?: "PENDING").toString())
                    putExtra("EXTRA_SOURCE", (order["source"] ?: "CASHIER").toString())
                    putExtra("EXTRA_BARCODE_URL", (order["barcodeUrl"] ?: order["barcode_url"] ?: "").toString())
                    putExtra("EXTRA_NOTES", (order["notes"] ?: "-").toString())
                }
                startActivity(intent)
            }
        }
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

        // Ambil pesanan yang aktif (MASUK / PENDING, DITERIMA / CONFIRMED, dan DIPROSES / PREPARING)
        orderListener = dbFirestore.collection("orders")
            .whereIn("status", listOf("PENDING", "CONFIRMED", "PREPARING"))
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
                    b.tvKosong.visibility = View.GONE
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

    // 3. PopupMenu (Bab V Modul PM Pak Benni & Checklist #12) — Klik tombol titik tiga per item
    private fun tampilkanPopupMenu(order: Map<String, Any?>, view: View) {
        val docId = order["idDoc"]?.toString() ?: return
        val popMenu = PopupMenu(requireContext(), view)
        popMenu.menuInflater.inflate(R.menu.menu_popup, popMenu.menu)

        popMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_terima -> {
                    updateStatusOrder(docId, order, "CONFIRMED")
                    true
                }
                R.id.menu_proses -> {
                    updateStatusOrder(docId, order, "PREPARING")
                    true
                }
                R.id.menu_selesai -> {
                    updateStatusOrder(docId, order, "COMPLETED")
                    true
                }
                R.id.menu_batal -> {
                    updateStatusOrder(docId, order, "CANCELLED")
                    true
                }
                else -> false
            }
        }
        popMenu.show()
    }

    // 4. ContextMenu (Bab V Modul PM Pak Benni & Checklist #11) — Saat item ditekan tahan (Long Click)
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as? AdapterView.AdapterContextMenuInfo
        val position = info?.position ?: -1

        val mnuInflater = requireActivity().menuInflater
        mnuInflater.inflate(R.menu.menu_context, menu)

        // 5. Kunci data pesanan yang dipilih agar tidak berubah saat realtime update
        if (position in listOrders.indices) {
            selectedOrderData = listOrders[position]
            val order = selectedOrderData ?: return
            val orderNumber = order["orderNumber"]?.toString() ?: order["idDoc"]?.toString() ?: "-"
            menu.setHeaderTitle("Pesanan #$orderNumber")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        // 5. Ambil dari selectedOrderData yang sudah terkunci
        val order = selectedOrderData ?: return super.onContextItemSelected(item)
        val docId = order["idDoc"]?.toString() ?: return super.onContextItemSelected(item)

        val res = when (item.itemId) {
            R.id.ctx_terima -> {
                updateStatusOrder(docId, order, "CONFIRMED")
                true
            }
            R.id.ctx_proses -> {
                updateStatusOrder(docId, order, "PREPARING")
                true
            }
            R.id.ctx_selesai -> {
                updateStatusOrder(docId, order, "COMPLETED")
                true
            }
            R.id.ctx_batal -> {
                updateStatusOrder(docId, order, "CANCELLED")
                true
            }
            else -> super.onContextItemSelected(item)
        }
        selectedOrderData = null
        return res
    }

    // 4. Update Status Firestore + 7. Arsipkan ke SQLite jika COMPLETED
    private fun updateStatusOrder(docId: String, order: Map<String, Any?>, statusBaru: String): Boolean {
        val orderNumber = (order["orderNumber"] ?: docId).toString()
        val source = order["source"]?.toString() ?: ""
        val isWebOrder = source == "CUSTOMER_WEB" || orderNumber.startsWith("ORD-WEB")
        val existingBarcodeUrl = order["barcodeUrl"]?.toString() ?: order["barcode_url"]?.toString()

        // Khusus pesanan website: jika status berubah ke DITERIMA/DIPROSES dan belum punya barcodeUrl, generate QR & upload ke Supabase
        if (isWebOrder && (statusBaru == "CONFIRMED" || statusBaru == "PREPARING") && existingBarcodeUrl.isNullOrEmpty()) {
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Membuat & mengunggah QR Code pesanan #$orderNumber ke Supabase...",
                    Toast.LENGTH_SHORT
                ).show()
            }

            try {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(orderNumber, BarcodeFormat.QR_CODE, 500, 500)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val qrBytes = stream.toByteArray()

                SupabaseStorageHelper.uploadOrderQrCode(orderNumber, qrBytes) { success, publicUrl, error ->
                    val updateData = mutableMapOf<String, Any>(
                        "status" to statusBaru,
                        "updatedAt" to Timestamp.now()
                    )
                    if (success && publicUrl != null) {
                        updateData["barcodeUrl"] = publicUrl
                        updateData["barcode_url"] = publicUrl
                        updateData["barcodeCode"] = orderNumber
                    }

                    dbFirestore.collection("orders").document(docId)
                        .update(updateData)
                        .addOnSuccessListener {
                            if (isAdded) {
                                val pesan = if (success) {
                                    "Pesanan #$orderNumber $statusBaru! QR Code aktif di Supabase."
                                } else {
                                    "Pesanan #$orderNumber $statusBaru (Gagal Supabase: $error)"
                                }
                                Toast.makeText(requireContext(), pesan, Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Gagal update Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback update status biasa
                dbFirestore.collection("orders").document(docId).update("status", statusBaru)
            }
            return true
        }

        // 5. Update Status Standar (Bukan Web Order atau QR sudah ada)
        val updateMap = mutableMapOf<String, Any>(
            "status" to statusBaru,
            "updatedAt" to Timestamp.now()
        )
        dbFirestore.collection("orders").document(docId)
            .update(updateMap)
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
                    SoundHelper.playSuccess()
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
