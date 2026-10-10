package com.pesenhub.jenggirat

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView

class AntrianAdapter(
    private val context: Context,
    private var listOrder: List<Map<String, Any?>>,
    private val onMenuClick: (order: Map<String, Any?>, view: View) -> Unit
) : BaseAdapter() {

    fun updateData(newList: List<Map<String, Any?>>) {
        this.listOrder = newList
        notifyDataSetChanged()
    }

    override fun getCount(): Int = listOrder.size

    override fun getItem(position: Int): Any = listOrder[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_antrian, parent, false)
        val data = listOrder[position]

        val tvOrderNumber = view.findViewById<TextView>(R.id.tvOrderNumber)
        val tvCustomerName = view.findViewById<TextView>(R.id.tvCustomerName)
        val tvMenuDetail = view.findViewById<TextView>(R.id.tvMenuDetail)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvSource = view.findViewById<TextView>(R.id.tvSource)
        val btnOpsi = view.findViewById<ImageButton>(R.id.btnOpsi)

        val docId = data["idDoc"]?.toString() ?: ""
        val id = data["orderNumber"]?.toString() ?: docId
        val nama = data["customerName"]?.toString() ?: "Pelanggan"
        val menu = (data["menuItem"] ?: data["detailItem"])?.toString() ?: "-"
        val status = data["status"]?.toString() ?: "PENDING"
        val source = data["source"]?.toString() ?: "CASHIER"

        tvOrderNumber.text = "#$id"
        tvCustomerName.text = nama
        tvMenuDetail.text = menu
        tvSource.text = "Sumber: $source"

        when (status) {
            "PENDING" -> {
                tvStatus.text = "MASUK"
                tvStatus.setBackgroundColor(Color.parseColor("#FFE082")) // Kuning
                tvStatus.setTextColor(Color.parseColor("#E65100"))
            }
            "CONFIRMED" -> {
                tvStatus.text = "DITERIMA"
                tvStatus.setBackgroundColor(Color.parseColor("#E0F2F1")) // Teal muda
                tvStatus.setTextColor(Color.parseColor("#004D40"))
            }
            "PREPARING" -> {
                tvStatus.text = "DIPROSES"
                tvStatus.setBackgroundColor(Color.parseColor("#BBDEFB")) // Biru
                tvStatus.setTextColor(Color.parseColor("#0D47A1"))
            }
            "COMPLETED" -> {
                tvStatus.text = "SELESAI"
                tvStatus.setBackgroundColor(Color.parseColor("#C8E6C9")) // Hijau
                tvStatus.setTextColor(Color.parseColor("#1B5E20"))
            }
            "CANCELLED" -> {
                tvStatus.text = "DIBATALKAN"
                tvStatus.setBackgroundColor(Color.parseColor("#FFCDD2")) // Merah muda
                tvStatus.setTextColor(Color.parseColor("#B71C1C"))
            }
            else -> {
                tvStatus.text = status
                tvStatus.setBackgroundColor(Color.LTGRAY)
                tvStatus.setTextColor(Color.BLACK)
            }
        }

        // Klik di mana saja pada kartu item antrean langsung memicu PopupMenu
        view.setOnClickListener {
            onMenuClick(data, it)
        }

        btnOpsi.setOnClickListener {
            onMenuClick(data, it)
        }

        return view
    }
}

