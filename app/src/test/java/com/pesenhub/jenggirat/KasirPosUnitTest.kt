package com.pesenhub.jenggirat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KasirPosUnitTest {

    @Test
    fun testKeranjangItem_hargaSatuanDanSubtotalDasar() {
        val item = KeranjangItem(
            namaMenu = "Martabak Sosis/Jamur Biasa",
            hargaDasar = 20000,
            topping = emptyList(),
            hargaTopping = 0,
            qty = 2
        )

        assertEquals(20000, item.hargaSatuan)
        assertEquals(40000, item.subtotal)
        assertEquals("Martabak Sosis/Jamur Biasa", item.getDeskripsiLengkap())
    }

    @Test
    fun testKeranjangItem_denganExtraIsianDanTopping() {
        val toppings = listOf("Keju Mozzarella", "Daging Sapi")
        val hargaExtra = 15000 + 7000 // 22000

        val item = KeranjangItem(
            namaMenu = "Martabak Sosis/Jamur Biasa",
            hargaDasar = 20000,
            topping = toppings,
            hargaTopping = hargaExtra,
            qty = 3
        )

        assertEquals(42000, item.hargaSatuan)
        assertEquals(126000, item.subtotal)
        assertEquals("Martabak Sosis/Jamur Biasa (Keju Mozzarella, Daging Sapi)", item.getDeskripsiLengkap())
    }

    @Test
    fun testKeranjangAgregasi_itemSamaDitambahkan() {
        val listKeranjang = ArrayList<KeranjangItem>()

        val toppings = listOf("Keju Mozzarella")
        val item1 = KeranjangItem(
            namaMenu = "Martabak Sosis/Jamur Biasa",
            hargaDasar = 20000,
            topping = toppings,
            hargaTopping = 15000,
            qty = 1
        )
        listKeranjang.add(item1)

        // Tambah item kedua dengan menu dan topping yang sama persis
        val itemEksis = listKeranjang.find {
            it.namaMenu == "Martabak Sosis/Jamur Biasa" && it.topping == toppings
        }

        if (itemEksis != null) {
            itemEksis.qty += 2
        } else {
            listKeranjang.add(item1.copy(qty = 2))
        }

        assertEquals(1, listKeranjang.size)
        assertEquals(3, listKeranjang[0].qty)
        assertEquals(105000, listKeranjang[0].subtotal)
    }

    @Test
    fun testKeranjangAgregasi_itemToppingBerbeda_menjadiDuaBaris() {
        val listKeranjang = ArrayList<KeranjangItem>()

        val item1 = KeranjangItem(
            namaMenu = "Martabak Sosis/Jamur Biasa",
            hargaDasar = 20000,
            topping = listOf("Keju Mozzarella"),
            hargaTopping = 15000,
            qty = 1
        )
        listKeranjang.add(item1)

        val toppingsBaru = listOf("Daging Sapi")
        val itemEksis = listKeranjang.find {
            it.namaMenu == "Martabak Sosis/Jamur Biasa" && it.topping == toppingsBaru
        }

        if (itemEksis != null) {
            itemEksis.qty += 1
        } else {
            listKeranjang.add(
                KeranjangItem(
                    namaMenu = "Martabak Sosis/Jamur Biasa",
                    hargaDasar = 20000,
                    topping = toppingsBaru,
                    hargaTopping = 7000,
                    qty = 1
                )
            )
        }

        assertEquals(2, listKeranjang.size)
        assertEquals(2, listKeranjang.sumOf { it.qty })
        assertEquals(35000 + 27000, listKeranjang.sumOf { it.subtotal })
    }
}
