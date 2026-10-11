package com.pesenhub.jenggirat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pesenhub.jenggirat.databinding.ItemMenuPosBinding

class MenuPosAdapter(
    private var listMenu: List<KasirFragment.MenuModel>,
    private val onPilihClicked: (KasirFragment.MenuModel) -> Unit
) : RecyclerView.Adapter<MenuPosAdapter.MenuViewHolder>() {

    class MenuViewHolder(val binding: ItemMenuPosBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuPosBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = listMenu[position]
        with(holder.binding) {
            tvNamaMenuPos.text = item.nama
            tvTagKategori.text = item.kategori
            tvHargaMenuPos.text = "Rp " + formatRupiah(item.harga)

            tvDeskripsiMenuPos.text = when (item.kategori) {
                "Martabak Telur" -> "Kulit renyah gurih dengan isian telur & bumbu rempah spesial"
                "Terang Bulan" -> "Adonan lembut bersarang, mentega gurih & topping melimpah"
                "Minuman" -> "Penyegar dahaga nikmat pelengkap sajian martabak"
                else -> "Menu pilihan istimewa khas Jenggirat Kediri"
            }

            // Tampilkan foto dari Supabase jika ada, atau fallback ke logo Jenggirat
            if (item.imageUrl.startsWith("http")) {
                imgMenuPos.tag = item.imageUrl
                java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                    try {
                        val input = java.net.URL(item.imageUrl).openStream()
                        val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                        if (bitmap != null) {
                            imgMenuPos.post {
                                if (imgMenuPos.tag == item.imageUrl) {
                                    imgMenuPos.setPadding(0, 0, 0, 0)
                                    imgMenuPos.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                    imgMenuPos.setImageBitmap(bitmap)
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            } else {
                imgMenuPos.setPadding(6, 6, 6, 6)
                imgMenuPos.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                imgMenuPos.setImageResource(R.drawable.logo_jenggirat_transparent)
            }

            btnPilihMenuPos.setOnClickListener {
                onPilihClicked(item)
            }

            root.setOnClickListener {
                onPilihClicked(item)
            }
        }
    }

    override fun getItemCount(): Int = listMenu.size

    fun perbaruiData(newList: List<KasirFragment.MenuModel>) {
        this.listMenu = newList
        notifyDataSetChanged()
    }

    private fun formatRupiah(nominal: Int): String {
        return "%,d".format(nominal).replace(',', '.')
    }
}
