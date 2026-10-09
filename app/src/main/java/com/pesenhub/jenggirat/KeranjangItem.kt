package com.pesenhub.jenggirat

data class KeranjangItem(
    val namaMenu: String,
    val hargaDasar: Int,
    val topping: List<String>,
    val hargaTopping: Int,
    var qty: Int
) {
    val hargaSatuan: Int get() = hargaDasar + hargaTopping
    val subtotal: Int get() = hargaSatuan * qty

    fun getDeskripsiLengkap(): String {
        return if (topping.isNotEmpty()) {
            "$namaMenu (${topping.joinToString(", ")})"
        } else {
            namaMenu
        }
    }
}
