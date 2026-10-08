package com.pesenhub.jenggirat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper(context: Context) :
    SQLiteOpenHelper(context, name, null, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        // Tabel Riwayat Transaksi (Hanya menyimpan pesanan yang sudah COMPLETED/SELESAI - read only/arsip riwayat)
        val sqlRiwayat = "create table riwayat_transaksi(" +
                "id_pesanan text primary key, " +
                "nama_pelanggan text not null, " +
                "hp_pelanggan text, " +
                "detail_item text, " +
                "metode_bayar text, " +
                "total_bayar integer, " +
                "status_order text, " +
                "waktu_selesai text)"

        db?.execSQL(sqlRiwayat)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("drop table if exists riwayat_transaksi")
        onCreate(db)
    }

    companion object {
        private const val name = "pesenhub_riwayat.db"
        private const val version = 1
    }
}
