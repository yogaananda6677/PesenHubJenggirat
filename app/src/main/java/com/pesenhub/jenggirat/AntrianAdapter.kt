package com.pesenhub.jenggirat

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu

class AntrianAdapter(
    private val context: Context,
    private val listOrder: List<Map<String, Any?>>,
    private val onMenuClick: (orderId: String, view: View) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = listOrder.size

    override fun getItem(position: Int): Any = listOrder[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_antrian, parent, false)
        val data = listOrder[position]

        val txOrderNumber = view.findViewById<TextView>(R.id.txOrderNumber)
        val txCustomerName = view.findViewById<TextView>(R.id.txCustomerName)
        val txMenuDetail = view.findViewById<TextView>(R.id.txMenuDetail)
        val txStatus = view.findViewById<TextView>(R.id.txStatus)
        val txSource = view.findViewById<TextView>(R.id.txSource)
        val btnOpsi = view.findViewById<ImageButton>(R.id.btnOpsi)

        val id = data["orderNumber"]?.toString() ?: "-"
        val nama = data["customerName"]?.toString() ?: "Pelanggan"
        val menu = data["menuItem"]?.toString() ?: "-"
        val status = data["status"]?.toString() ?: "PENDING"
        val source = data["source"]?.toString() ?: "CASHIER"

        txOrderNumber.text = "#$id"
        txCustomerName.text = nama
        txMenuDetail.text = menu
        txSource.text = "Sumber: $source"

        when (status) {
            "PENDING" -> {
                txStatus.text = "MASUK"
                txStatus.setBackgroundColor(Color.parseColor("#FFE082")) // Kuning
                txStatus.setTextColor(Color.parseColor("#E65100"))
            }
            "PREPARING" -> {
                txStatus.text = "DIPROSES"
                txStatus.setBackgroundColor(Color.parseColor("#BBDEFB")) // Biru
                txStatus.setTextColor(Color.parseColor("#0D47A1"))
            }
            "COMPLETED" -> {
                txStatus.text = "SELESAI"
                txStatus.setBackgroundColor(Color.parseColor("#C8E6C9")) // Hijau
                txStatus.setTextColor(Color.parseColor("#1B5E20"))
            }
            else -> {
                txStatus.text = status
                txStatus.setBackgroundColor(Color.LTGRAY)
                txStatus.setTextColor(Color.BLACK)
            }
        }

        btnOpsi.setOnClickListener {
            onMenuClick(id, it)
        }

        return view
    }
}
