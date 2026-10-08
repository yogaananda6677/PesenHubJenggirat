package com.pesenhub.jenggirat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pesenhub.jenggirat.databinding.FragmentDashboardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    lateinit var b: FragmentDashboardBinding
    lateinit var auth: FirebaseAuth
    lateinit var dbFirestore: FirebaseFirestore
    lateinit var dbHelper: DBOpenHelper

    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentDashboardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        dbFirestore = FirebaseFirestore.getInstance()
        dbHelper = DBOpenHelper(requireContext())

        // 1. Tampilkan Info Kasir & Tanggal
        val userEmail = auth.currentUser?.email ?: "Kasir Outlet"
        b.txKasirEmail.text = "Kasir Aktif: $userEmail"

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        b.txTanggalHariIni.text = "Hari ini: ${sdf.format(Date())}"

        // 2. Pantau Metrik Realtime dari Firestore (Pesanan Masuk & Sedang Dimasak)
        pantauMetrikFirestore()

        // 3. Muat Metrik dari Arsip SQLite (Pesanan Selesai & Total Omzet)
        muatMetrikDariSQLite()

        // 4. Navigasi Aksi Cepat
        b.btnAksiKasir.setOnClickListener {
            (activity as? MainActivity)?.navigasiKeTab(R.id.nav_kasir)
        }

        b.btnAksiAntrian.setOnClickListener {
            (activity as? MainActivity)?.navigasiKeTab(R.id.nav_antrian)
        }

        b.btnAksiRiwayat.setOnClickListener {
            (activity as? MainActivity)?.navigasiKeTab(R.id.nav_laporan)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh metrik SQLite saat kembali ke Dashboard
        muatMetrikDariSQLite()
    }

    private fun pantauMetrikFirestore() {
        firestoreListener = dbFirestore.collection("orders")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && isAdded) {
                    var pendingCount = 0
                    var preparingCount = 0

                    for (doc in snapshot.documents) {
                        val status = doc.getString("status") ?: ""
                        when (status) {
                            "PENDING" -> pendingCount++
                            "PREPARING" -> preparingCount++
                        }
                    }

                    b.txJumlahPending.text = pendingCount.toString()
                    b.txJumlahProses.text = preparingCount.toString()
                }
            }
    }

    private fun muatMetrikDariSQLite() {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("select count(*), sum(total_bayar) from riwayat_transaksi", null)
            if (cursor.moveToFirst()) {
                val jumlahSelesai = cursor.getInt(0)
                val totalOmzet = cursor.getInt(1)

                b.txJumlahSelesai.text = jumlahSelesai.toString()
                b.txTotalOmzet.text = "Rp " + "%,d".format(totalOmzet).replace(',', '.')
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }
}
