package com.pesenhub.jenggirat

data class MenuKelolaModel(
    val id: String = "",
    val sku: String = "",
    val name: String = "",
    val category: String = "Martabak Telur",
    val description: String = "",
    val price: Long = 0L,
    val hppAmount: Long = 0L,
    val available: Boolean = true,
    val imageUrl: String = "default_food_icon",
    val channelPrices: Map<String, Long> = emptyMap()
) {
    fun priceForChannel(channel: String): Long {
        return channelPrices[channel.uppercase()] ?: price
    }
}
