package com.pesenhub.jenggirat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper(context: Context) :
    SQLiteOpenHelper(context, name, null, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        val sqlPesanan = "create table pesanan_offline(" +
                "id_pesanan text primary key, " +
                "nama_pelanggan text not null, " +
                "hp_pelanggan text, " +
                "detail_item text, " +
                "metode_bayar text, " +
                "total_bayar integer, " +
                "status_order text, " +
                "waktu_dibuat text, " +
                "is_synced integer default 0)"

        val sqlMasterMenu = "create table master_menu(" +
                "id_menu text primary key, " +
                "nama_menu text not null, " +
                "kategori text, " +
                "harga integer, " +
                "tersedia integer default 1)"

        db?.execSQL(sqlPesanan)
        db?.execSQL(sqlMasterMenu)

        // Seed data menu awal martabak
        db?.execSQL("insert into master_menu(id_menu, nama_menu, kategori, harga, tersedia) " +
                "values ('m1', 'Martabak Telur Spesial', 'Martabak Telur', 35000, 1)")
        db?.execSQL("insert into master_menu(id_menu, nama_menu, kategori, harga, tersedia) " +
                "values ('m2', 'Terang Bulan Coklat Keju', 'Terang Bulan', 30000, 1)")
        db?.execSQL("insert into master_menu(id_menu, nama_menu, kategori, harga, tersedia) " +
                "values ('m3', 'Es Teh Manis', 'Minuman', 5000, 1)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("drop table if exists pesanan_offline")
        db?.execSQL("drop table if exists master_menu")
        onCreate(db)
    }

    companion object {
        private const val name = "pesenhub_local.db"
        private const val version = 1
    }
}
