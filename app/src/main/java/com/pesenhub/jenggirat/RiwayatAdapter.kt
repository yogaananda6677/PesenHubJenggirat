package com.pesenhub.jenggirat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class RiwayatAdapter(
    private val context: Context,
    private val data: List<Map<String, Any?>>
) : BaseAdapter() {

    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Any = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_riwayat, parent, false)

        val txOrderId = view.findViewById<TextView>(R.id.txItemOrderId)
        val txWaktu = view.findViewById<TextView>(R.id.txItemWaktu)
        val txPelanggan = view.findViewById<TextView>(R.id.txItemPelanggan)
        val txMenu = view.findViewById<TextView>(R.id.txItemMenu)
        val txMetode = view.findViewById<TextView>(R.id.txItemMetode)
        val txTotal = view.findViewById<TextView>(R.id.txItemTotal)

        val item = data[position]
        val orderId = item["id_pesanan"]?.toString() ?: "-"
        val waktu = item["waktu_selesai"]?.toString() ?: "-"
        val pelanggan = item["nama_pelanggan"]?.toString() ?: "-"
        val hp = item["hp_pelanggan"]?.toString() ?: ""
        val menu = item["detail_item"]?.toString() ?: "-"
        val metode = item["metode_bayar"]?.toString() ?: "Tunai"
        val total = (item["total_bayar"] as? Number)?.toInt() ?: 0

        txOrderId.text = "#$orderId"
        txWaktu.text = waktu
        txPelanggan.text = if (hp.isNotEmpty()) "Pelanggan: $pelanggan ($hp)" else "Pelanggan: $pelanggan"
        txMenu.text = menu
        txMetode.text = "Metode: $metode"
        txTotal.text = "Rp " + "%,d".format(total).replace(',', '.')

        return view
    }
}
