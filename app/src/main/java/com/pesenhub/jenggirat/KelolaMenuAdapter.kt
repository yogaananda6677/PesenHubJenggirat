package com.pesenhub.jenggirat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pesenhub.jenggirat.databinding.ItemKelolaMenuBinding
import java.text.NumberFormat
import java.util.Locale

class KelolaMenuAdapter(
    private val context: Context,
    private var listMenu: List<MenuKelolaModel>,
    private val onToggleAvailable: (menu: MenuKelolaModel, isAvailable: Boolean) -> Unit,
    private val onEditClick: (menu: MenuKelolaModel) -> Unit,
    private val onDeleteClick: (menu: MenuKelolaModel) -> Unit
) : RecyclerView.Adapter<KelolaMenuAdapter.KelolaViewHolder>() {

    fun perbaruiData(newList: List<MenuKelolaModel>) {
        this.listMenu = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KelolaViewHolder {
        val b = ItemKelolaMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KelolaViewHolder(b)
    }

    override fun getItemCount(): Int = listMenu.size

    override fun onBindViewHolder(holder: KelolaViewHolder, position: Int) {
        holder.bind(listMenu[position])
    }

    inner class KelolaViewHolder(val b: ItemKelolaMenuBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MenuKelolaModel) {
            b.tvKelolaSku.text = item.sku.ifEmpty { item.id }
            b.tvKelolaKategori.text = item.category
            b.tvKelolaNama.text = item.name
            b.tvKelolaDeskripsi.text = item.description.ifEmpty { "Menu Martabak & Terang Bulan Jenggirat" }

            // Switch Ketersediaan (avoid firing listener during bind)
            b.swKelolaKetersediaan.setOnCheckedChangeListener(null)
            b.swKelolaKetersediaan.isChecked = item.available
            b.swKelolaKetersediaan.setOnCheckedChangeListener { _, isChecked ->
                onToggleAvailable(item, isChecked)
            }

            // Format Harga Multi-Channel
            b.tvHargaOffline.text = formatRupiah(item.priceForChannel("OFFLINE"))
            b.tvHargaWeb.text = formatRupiah(item.priceForChannel("CUSTOMER_WEB"))
            b.tvHargaGofood.text = formatRupiah(item.priceForChannel("GOFOOD"))
            b.tvHargaGrabfood.text = formatRupiah(item.priceForChannel("GRABFOOD"))
            b.tvHargaShopeefood.text = formatRupiah(item.priceForChannel("SHOPEEFOOD"))

            // Tombol Edit & Hapus
            b.btnEditKelolaMenu.setOnClickListener {
                onEditClick(item)
            }

            b.btnHapusKelolaMenu.setOnClickListener {
                onDeleteClick(item)
            }
        }

        private fun formatRupiah(angka: Long): String {
            val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
            return format.format(angka)
        }
    }
}
