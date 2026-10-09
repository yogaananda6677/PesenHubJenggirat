package com.pesenhub.jenggirat

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.pesenhub.jenggirat.databinding.FragmentRiwayatBinding
import java.util.Calendar

class RiwayatFragment : Fragment() {

    lateinit var b: FragmentRiwayatBinding
    lateinit var dbHelper: DBOpenHelper

    val listRiwayat = ArrayList<Map<String, Any?>>()
    var tanggalFilterAktif: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentRiwayatBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBOpenHelper(requireContext())

        // 1. Tombol DatePickerDialog (Bab III Modul PM Pak Benni)
        b.btnPilihTanggal.setOnClickListener {
            tampilkanDatePicker()
        }

        // 2. Tombol Reset Filter Tanggal
        b.btnResetTanggal.setOnClickListener {
            tanggalFilterAktif = null
            b.txFilterInfo.text = "Menampilkan: Semua Transaksi Tersimpan"
            muatRiwayatDariSQLite()
        }

        // 3. Muat Riwayat Awal
        muatRiwayatDariSQLite()

        // 4. Klik Item Riwayat
        b.lsRiwayat.setOnItemClickListener { _, _, position, _ ->
            if (position in listRiwayat.indices) {
                val item = listRiwayat[position]
                val id = item["id_pesanan"]
                val pelanggan = item["nama_pelanggan"]
                val total = item["total_bayar"]
                Toast.makeText(requireContext(), "Pesanan #$id - $pelanggan (Total: Rp $total)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        muatRiwayatDariSQLite()
    }

    private fun tampilkanDatePicker() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format dd/MM/yyyy sesuai penyimpanan di DB
                val strDay = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
                val bln = selectedMonth + 1
                val strMonth = if (bln < 10) "0$bln" else "$bln"
                val tanggalTerpilih = "$strDay/$strMonth/$selectedYear"

                tanggalFilterAktif = tanggalTerpilih
                b.txFilterInfo.text = "Menampilkan Transaksi: $tanggalTerpilih"
                muatRiwayatDariSQLite()
            },
            year,
            month,
            day
        )
        datePicker.show()
    }

    private fun muatRiwayatDariSQLite() {
        listRiwayat.clear()
        var totalPendapatan = 0

        try {
            val db = dbHelper.readableDatabase
            val cursor = if (tanggalFilterAktif != null) {
                db.rawQuery(
                    "select id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, waktu_selesai " +
                            "from riwayat_transaksi where waktu_selesai like ? order by rowid desc",
                    arrayOf("$tanggalFilterAktif%")
                )
            } else {
                db.rawQuery(
                    "select id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, waktu_selesai " +
                            "from riwayat_transaksi order by rowid desc",
                    null
                )
            }

            if (cursor.moveToFirst()) {
                do {
                    val map = HashMap<String, Any?>()
                    val id = cursor.getString(0)
                    val nama = cursor.getString(1)
                    val hp = cursor.getString(2)
                    val menu = cursor.getString(3)
                    val metode = cursor.getString(4)
                    val total = cursor.getInt(5)
                    val waktu = cursor.getString(6)

                    map["id_pesanan"] = id
                    map["nama_pelanggan"] = nama
                    map["hp_pelanggan"] = hp
                    map["detail_item"] = menu
                    map["metode_bayar"] = metode
                    map["total_bayar"] = total
                    map["waktu_selesai"] = waktu

                    totalPendapatan += total
                    listRiwayat.add(map)
                } while (cursor.moveToNext())
            }
            cursor.close()

            // Update UI Ringkasan
            b.txCountRiwayat.text = "${listRiwayat.size} Pesanan"
            b.txSumRiwayat.text = "Rp " + "%,d".format(totalPendapatan).replace(',', '.')

            if (listRiwayat.isEmpty()) {
                b.txRiwayatKosong.visibility = View.VISIBLE
                b.lsRiwayat.visibility = View.GONE
            } else {
                b.txRiwayatKosong.visibility = View.GONE
                b.lsRiwayat.visibility = View.VISIBLE
            }

            if (isAdded) {
                val adapter = RiwayatAdapter(requireContext(), listRiwayat)
                b.lsRiwayat.adapter = adapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
