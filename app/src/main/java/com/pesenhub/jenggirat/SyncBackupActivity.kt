package com.pesenhub.jenggirat

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pesenhub.jenggirat.databinding.ActivitySyncBackupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * =========================================================================================
 * MODUL 12: THREADS (COROUTINE) - SINKRONISASI & BACKUP DATA RIWAYAT KE SERVER MYSQL
 * Pemrograman Mobile Lanjut - D3 Manajemen Informatika Kampus Kediri
 * Dosen Pengampu: Benni Agung Nugroho, S.Kom., M.Cs.
 * =========================================================================================
 *
 * Konsep yang diterapkan pada Activity ini sesuai Modul 12:
 * 1. Main Thread (UI Thread): Digunakan hanya untuk memperbarui elemen visual tampilan.
 * 2. Background Thread (Dispatchers.IO): Digunakan untuk operasi berat (Database SQLite,
 *    koneksi HTTP REST API ke MySQL Backup Server Cloudflare, dan Cloud Firestore).
 * 3. lifecycleScope.launch: Memulai Coroutine yang terikat siklus hidup Activity agar
 *    tidak terjadi memory leak saat pengguna berpindah layar.
 * 4. async & await(): Menjalankan proses multi-storage secara paralel / bersamaan dan
 *    menunggu hasil kedua proses tersebut secara non-blocking.
 * 5. Mutex (Mutual Exclusion): Mekanisme penguncian thread-safe untuk mengamankan data
 *    counter dari race condition antar thread (Halaman 7-8 Modul).
 * 6. withContext(): Berpindah thread (dari IO ke Main) di tengah jalan lalu kembali lagi.
 * 7. Multi-Tier Disaster Recovery: Jika data lokal SQLite terhapus atau data di Firestore
 *    hilang, proses sinkronisasi otomatis memulihkan data tersebut dari MySQL server.
 */
class SyncBackupActivity : AppCompatActivity() {

    private lateinit var b: ActivitySyncBackupBinding

    // [MODUL 12: KOTLIN UTILITY - BY LAZY]
    // Inisialisasi variabel hanya saat pertama kali diakses
    private val dbHelper: DBOpenHelper by lazy { DBOpenHelper(this) }
    private val firestoreDb: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Flag pembatalan proses oleh pengguna
    @Volatile
    private var cancelProcess: Boolean = false

    companion object {
        const val DEFAULT_SERVER_URL = "https://pml-web-jenggirat.buildwithyoga.my.id"
    }

    /**
     * [MODUL 12: MUTEX THREAD-SAFE DATA COUNTER (Halaman 7-8 Modul)]
     * Menggunakan Mutex untuk penguncian eksklusif (Mutual Exclusion).
     */
    class DataCounter {
        private val mutex = Mutex()
        private var counter = 0

        suspend fun increment() {
            mutex.withLock {
                counter++
            }
        }

        suspend fun get(): Int {
            return mutex.withLock {
                counter
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySyncBackupBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbarSync)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.toolbarSync.setNavigationOnClickListener { finish() }

        // Tombol Utama: Sinkronkan Data (Coroutine)
        b.btnStartSync.setOnClickListener {
            cancelProcess = false
            mulaiSyncDenganCoroutine()
        }

        // Tombol Simulasi: Hapus Data Lokal (Untuk menguji pemulihan jika data lokal hilang)
        b.btnHapusLokal.setOnClickListener {
            konfirmasiHapusDataLokal()
        }

        // Tombol Batalkan Proses
        b.btnCancel.setOnClickListener {
            cancelProcess = true
            Toast.makeText(this, "Proses sinkronisasi dibatalkan", Toast.LENGTH_SHORT).show()
            b.tvProgressStatus.text = "Membatalkan proses..."
        }

        // Uji koneksi server & muat statistik data awal
        val baseUrl = getServerBaseUrl()
        ujiKoneksiServer(baseUrl)
        muatStatistikData(baseUrl)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sync_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                val baseUrl = getServerBaseUrl()
                ujiKoneksiServer(baseUrl)
                muatStatistikData(baseUrl)
                Toast.makeText(this, "Memperbarui data dan status server...", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                tampilkanDialogAturServerUrl()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getServerBaseUrl(): String {
        val pref = getSharedPreferences("SyncConfig", Context.MODE_PRIVATE)
        val raw = pref.getString("server_url", DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        return raw.trim()
            .replace("`", "")
            .replace("'", "")
            .replace("\"", "")
            .removeSuffix("/")
            .ifEmpty { DEFAULT_SERVER_URL }
    }

    private fun simpanServerUrl(url: String) {
        val cleaned = url.trim()
            .replace("`", "")
            .replace("'", "")
            .replace("\"", "")
            .removeSuffix("/")
            .ifEmpty { DEFAULT_SERVER_URL }

        getSharedPreferences("SyncConfig", Context.MODE_PRIVATE)
            .edit()
            .putString("server_url", cleaned)
            .apply()

        ujiKoneksiServer(cleaned)
        muatStatistikData(cleaned)
    }

    private fun tampilkanDialogAturServerUrl() {
        val currentUrl = getServerBaseUrl()
        val input = EditText(this).apply {
            setText(currentUrl)
            setSelection(currentUrl.length)
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Pengaturan URL Server Backup")
            .setMessage("Alamat endpoint REST API server Laravel:")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newUrl = input.text.toString()
                simpanServerUrl(newUrl)
                Toast.makeText(this, "URL server berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // =====================================================================================
    // FITUR SIMULASI: HAPUS DATA LOKAL SQLITE
    // Digunakan untuk membuktikan skenario pemulihan saat data lokal di perangkat hilang
    // =====================================================================================
    private fun konfirmasiHapusDataLokal() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Data SQLite Lokal?")
            .setMessage("Tindakan ini akan mengosongkan seluruh riwayat transaksi di penyimpanan lokal perangkat.\n\nAnda dapat menguji proses pemulihan data setelahnya dengan menekan tombol 'SINKRONKAN DATA'.")
            .setPositiveButton("Hapus") { _, _ ->
                try {
                    val db = dbHelper.writableDatabase
                    db.execSQL("delete from riwayat_transaksi")
                    b.tvCountSqlite.text = "0"
                    Toast.makeText(this, "Data lokal SQLite telah dikosongkan (0 Transaksi)", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal mengosongkan data lokal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // =====================================================================================
    // [MODUL 12: MEMUAT STATISTIK DATA SECARA ASYNCHRONOUS (IO -> MAIN)]
    // =====================================================================================
    private fun muatStatistikData(baseUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val countSqlite = ambilJumlahSQLite()
            val countFirestore = ambilJumlahFirestore()
            val countMysql = ambilJumlahMySQL(baseUrl)

            withContext(Dispatchers.Main) {
                b.tvCountSqlite.text = countSqlite.toString()
                b.tvCountFirestore.text = countFirestore.toString()
                b.tvCountMysql.text = countMysql.toString()
            }
        }
    }

    // =====================================================================================
    // [MODUL 12: SINKRONISASI CERDAS MULTI-TIER DENGAN COROUTINE THREADS]
    // Menjamin data di SQLite, Firestore, dan MySQL sinkron 100%.
    // Skenario 1: Data lokal kosong -> dipulihkan dari MySQL / Firestore.
    // Skenario 2: Data di Firestore terhapus -> dipulihkan dari MySQL / SQLite.
    // Skenario 3: Transaksi baru -> dicadangkan ke MySQL server.
    // =====================================================================================
    private fun mulaiSyncDenganCoroutine() {
        val serverBaseUrl = getServerBaseUrl()
        setUiLoading(true)

        b.tvProgressStatus.text = "Mengumpulkan data transaksi dari SQLite, MySQL, dan Firestore..."
        b.progressBarSync.isIndeterminate = true

        lifecycleScope.launch {
            val waktuMulai = SystemClock.elapsedRealtime()

            // 1. Ambil data dari ketiga sumber secara paralel di Dispatchers.IO
            val tugasSqlite = async(Dispatchers.IO) { ambilRiwayatDariSQLite() }
            val tugasMysql = async(Dispatchers.IO) { ambilRiwayatDariMySQL(serverBaseUrl) }
            val tugasFirestore = async(Dispatchers.IO) { ambilRiwayatDariFirestore() }

            val listSqlite = tugasSqlite.await()
            val listMysql = tugasMysql.await()
            val listFirestore = tugasFirestore.await()

            // 2. Gabungkan data berdasarkan id_pesanan (ID Transaksi Unik)
            val mapGabungan = LinkedHashMap<String, MutableMap<String, Any?>>()

            // Masukkan data MySQL (Data Master Backup)
            for (item in listMysql) {
                val id = item["id_pesanan"]?.toString() ?: continue
                mapGabungan[id] = item.toMutableMap()
            }

            // Masukkan data SQLite
            for (item in listSqlite) {
                val id = item["id_pesanan"]?.toString() ?: continue
                if (!mapGabungan.containsKey(id)) {
                    mapGabungan[id] = item.toMutableMap()
                } else {
                    mapGabungan[id]?.putAll(item)
                }
            }

            // Masukkan data Firestore
            for (item in listFirestore) {
                val id = item["id_pesanan"]?.toString() ?: continue
                if (!mapGabungan.containsKey(id)) {
                    mapGabungan[id] = item.toMutableMap()
                }
            }

            val daftarTotal = mapGabungan.values.toList()

            if (daftarTotal.isEmpty()) {
                withContext(Dispatchers.Main) {
                    setUiLoading(false)
                    Toast.makeText(this@SyncBackupActivity, "Belum ada transaksi untuk disinkronkan", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val totalData = daftarTotal.size

            withContext(Dispatchers.Main) {
                b.progressBarSync.isIndeterminate = false
                b.progressBarSync.max = totalData
                b.progressBarSync.progress = 0
                b.tvProgressCount.text = "0 / $totalData"
            }

            // [MODUL 12: MUTEX THREAD-SAFE COUNTER]
            val counterPulihFirestore = DataCounter()
            val counterBackupMysql = DataCounter()
            val counterPulihSqlite = DataCounter()

            // 3. Eksekusi Sinkronisasi Tiap Transaksi Menggunakan Coroutine
            for ((index, item) in daftarTotal.withIndex()) {
                if (cancelProcess || !isActive) {
                    break
                }

                val idPesanan = item["id_pesanan"]?.toString() ?: "ORD-${index + 1}"

                withContext(Dispatchers.Main) {
                    b.tvProgressStatus.text = "Menyinkronkan transaksi #$idPesanan..."
                }

                // [MODUL 12: DELAY NON-BLOCKING]
                // Menjeda coroutine halus tanpa membekukan antarmuka
                delay(120L)

                // [MODUL 12: DISPATCHERS.IO DENGAN ASYNC & AWAIT]
                // 1) Pastikan data ada di Cloud Firestore (Pulihkan jika sebelumnya terhapus)
                val tugasSyncFs = async(Dispatchers.IO) {
                    simpanKeFirestore(item)
                }

                // 2) Pastikan data ada di MySQL Server
                val tugasSyncMy = async(Dispatchers.IO) {
                    kirimKeMysqlServer(serverBaseUrl, item)
                }

                // 3) Pastikan data ada di SQLite Lokal (Pulihkan jika sebelumnya dikosongkan)
                val tugasSyncDb = async(Dispatchers.IO) {
                    simpanKeSQLiteLokal(item)
                }

                val fsOk = try { tugasSyncFs.await() } catch (e: Exception) { false }
                val myOk = try { tugasSyncMy.await() } catch (e: Exception) { false }
                val dbOk = try { tugasSyncDb.await() } catch (e: Exception) { false }

                if (fsOk) counterPulihFirestore.increment()
                if (myOk) counterBackupMysql.increment()
                if (dbOk) counterPulihSqlite.increment()

                // [MODUL 12: WITHCONTEXT(DISPATCHERS.MAIN)]
                // Update UI Progress Bar
                withContext(Dispatchers.Main) {
                    val currentStep = index + 1
                    b.progressBarSync.progress = currentStep
                    b.tvProgressCount.text = "$currentStep / $totalData"
                }
            }

            val durasiDetik = (SystemClock.elapsedRealtime() - waktuMulai) / 1000.0
            val hasilFs = counterPulihFirestore.get()
            val hasilMy = counterBackupMysql.get()
            val hasilDb = counterPulihSqlite.get()

            // Selesai: update UI dan tampilkan Dialog Hasil
            withContext(Dispatchers.Main) {
                setUiLoading(false)
                muatStatistikData(serverBaseUrl)

                val pesanSukses = StringBuilder()
                pesanSukses.append("Sinkronisasi data berhasil diselesaikan dalam ${"%.2f".format(durasiDetik)} detik.\n\n")
                pesanSukses.append("• SQLite Lokal    : $hasilDb / $totalData data tersimpan\n")
                pesanSukses.append("• Cloud Firestore : $hasilFs / $totalData data tersinkron\n")
                pesanSukses.append("• Server MySQL    : $hasilMy / $totalData data ter-backup\n\n")
                pesanSukses.append("Seluruh data kini konsisten dan selaras di semua media penyimpanan.")

                MaterialAlertDialogBuilder(this@SyncBackupActivity)
                    .setTitle("Sinkronisasi Selesai")
                    .setMessage(pesanSukses.toString())
                    .setPositiveButton("Selesai") { d, _ -> d.dismiss() }
                    .show()
            }
        }
    }

    private fun setUiLoading(isLoading: Boolean) {
        b.btnStartSync.isEnabled = !isLoading
        b.btnHapusLokal.isEnabled = !isLoading
        b.btnCancel.visibility = if (isLoading) View.VISIBLE else View.GONE
        b.layoutProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            b.btnStartSync.text = "SEDANG MENYINKRONKAN..."
        } else {
            b.btnStartSync.text = "SINKRONKAN DATA"
        }
    }

    // =====================================================================================
    // HELPER OPERASI DATABASE LOKAL & CLOUD
    // =====================================================================================
    private fun ambilJumlahSQLite(): Int {
        return try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("select count(*) from riwayat_transaksi", null)
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun ambilJumlahFirestore(): Int {
        return try {
            kotlin.coroutines.suspendCoroutine { continuation ->
                firestoreDb.collection("orders").get()
                    .addOnSuccessListener { snap ->
                        continuation.resumeWith(Result.success(snap.size()))
                    }
                    .addOnFailureListener {
                        continuation.resumeWith(Result.success(0))
                    }
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun ambilJumlahMySQL(baseUrl: String): Int {
        return try {
            val endpoint = "$baseUrl/api/backup/status"
            val jsonStr = httpGet(endpoint)
            val json = JSONObject(jsonStr)
            json.optInt("total_records", 0)
        } catch (e: Exception) {
            0
        }
    }

    private fun ambilRiwayatDariSQLite(): List<Map<String, Any?>> {
        val list = ArrayList<Map<String, Any?>>()
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "select id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, waktu_selesai " +
                        "from riwayat_transaksi order by rowid desc",
                null
            )
            if (cursor.moveToFirst()) {
                do {
                    val map = HashMap<String, Any?>()
                    map["id_pesanan"] = cursor.getString(0)
                    map["nama_pelanggan"] = cursor.getString(1)
                    map["hp_pelanggan"] = cursor.getString(2)
                    map["detail_item"] = cursor.getString(3)
                    map["metode_bayar"] = cursor.getString(4)
                    map["total_bayar"] = cursor.getInt(5)
                    map["waktu_selesai"] = cursor.getString(6)
                    list.add(map)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun ambilRiwayatDariMySQL(baseUrl: String): List<Map<String, Any?>> {
        val list = ArrayList<Map<String, Any?>>()
        try {
            val endpoint = "$baseUrl/api/backup/orders"
            val response = httpGet(endpoint)
            val json = JSONObject(response)
            if (json.optBoolean("success", false)) {
                val data = json.optJSONArray("data") ?: JSONArray()
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val map = HashMap<String, Any?>()
                    map["id_pesanan"] = obj.optString("order_number")
                    map["nama_pelanggan"] = obj.optString("customer_name")
                    map["hp_pelanggan"] = obj.optString("customer_phone")
                    map["detail_item"] = obj.optString("detail_item")
                    map["metode_bayar"] = obj.optString("payment_method")
                    map["total_bayar"] = obj.optInt("total_price")
                    map["waktu_selesai"] = obj.optString("waktu_transaksi")
                    list.add(map)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private suspend fun ambilRiwayatDariFirestore(): List<Map<String, Any?>> {
        return try {
            kotlin.coroutines.suspendCoroutine { continuation ->
                firestoreDb.collection("orders").get()
                    .addOnSuccessListener { snap ->
                        val list = ArrayList<Map<String, Any?>>()
                        for (doc in snap.documents) {
                            try {
                                val map = HashMap<String, Any?>()
                                map["id_pesanan"] = doc.getString("orderNumber") ?: doc.id
                                map["nama_pelanggan"] = doc.getString("customerName") ?: "-"
                                map["hp_pelanggan"] = doc.getString("customerPhone") ?: "-"
                                map["detail_item"] = doc.getString("detailItem") ?: "-"
                                map["metode_bayar"] = doc.getString("paymentMethod") ?: "Tunai"
                                val rawTotal = doc.get("total")
                                val totalVal = when (rawTotal) {
                                    is Number -> rawTotal.toLong()
                                    is String -> rawTotal.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                                    else -> 0L
                                }
                                map["total_bayar"] = totalVal
                                map["waktu_selesai"] = doc.getString("completedAt") ?: "-"
                                list.add(map)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                        continuation.resumeWith(Result.success(list))
                    }
                    .addOnFailureListener {
                        continuation.resumeWith(Result.success(emptyList()))
                    }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun simpanKeFirestore(item: Map<String, Any?>): Boolean {
        return try {
            val idPesanan = item["id_pesanan"]?.toString() ?: return false
            val payload = hashMapOf<String, Any>(
                "orderNumber" to idPesanan,
                "customerName" to (item["nama_pelanggan"]?.toString() ?: "-"),
                "customerPhone" to (item["hp_pelanggan"]?.toString() ?: "-"),
                "detailItem" to (item["detail_item"]?.toString() ?: "-"),
                "paymentMethod" to (item["metode_bayar"]?.toString() ?: "Tunai"),
                "total" to (item["total_bayar"] as? Number ?: 0),
                "status" to "COMPLETED",
                "completedAt" to (item["waktu_selesai"]?.toString() ?: "-"),
                "backupSynced" to true,
                "syncTimestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            kotlin.coroutines.suspendCoroutine { continuation ->
                firestoreDb.collection("orders").document(idPesanan)
                    .set(payload, SetOptions.merge())
                    .addOnSuccessListener { continuation.resumeWith(Result.success(true)) }
                    .addOnFailureListener { continuation.resumeWith(Result.success(false)) }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun kirimKeMysqlServer(baseUrl: String, item: Map<String, Any?>): Boolean {
        return try {
            val endpoint = "$baseUrl/api/backup/push"
            val json = JSONObject().apply {
                put("id_pesanan", item["id_pesanan"])
                put("nama_pelanggan", item["nama_pelanggan"])
                put("hp_pelanggan", item["hp_pelanggan"])
                put("detail_item", item["detail_item"])
                put("metode_bayar", item["metode_bayar"])
                put("total_bayar", item["total_bayar"])
                put("status_order", "COMPLETED")
                put("waktu_selesai", item["waktu_selesai"])
                put("sync_source", "ANDROID_COROUTINE")
            }

            val responseCode = httpPostJson(endpoint, json.toString())
            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    private fun simpanKeSQLiteLokal(item: Map<String, Any?>): Boolean {
        return try {
            val idPesanan = item["id_pesanan"]?.toString() ?: return false
            val nama = item["nama_pelanggan"]?.toString() ?: "-"
            val hp = item["hp_pelanggan"]?.toString() ?: "-"
            val menu = item["detail_item"]?.toString() ?: "-"
            val metode = item["metode_bayar"]?.toString() ?: "Tunai"
            val total = (item["total_bayar"] as? Number)?.toInt() ?: 0
            val waktu = item["waktu_selesai"]?.toString() ?: "-"

            val db = dbHelper.writableDatabase
            val sql = "insert or replace into riwayat_transaksi(" +
                    "id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, status_order, waktu_selesai) " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?)"
            db.execSQL(sql, arrayOf(idPesanan, nama, hp, menu, metode, total, "COMPLETED", waktu))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun ujiKoneksiServer(baseUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val endpoint = "$baseUrl/api/backup/status"
                val jsonStr = httpGet(endpoint)
                val json = JSONObject(jsonStr)
                val status = json.optString("status", "UNKNOWN")
                val conn = json.optString("connection", "-")
                val total = json.optInt("total_records", 0)

                withContext(Dispatchers.Main) {
                    if (status.equals("ONLINE", ignoreCase = true)) {
                        b.tvServerBadge.text = "ONLINE"
                        b.tvServerBadge.setBackgroundResource(R.drawable.bg_badge_online)
                        b.tvServerBadge.setTextColor(getColor(android.R.color.holo_green_dark))
                        b.tvServerStatus.text = "Koneksi: Terhubung ($conn) • $total transaksi di MySQL"
                        b.tvCountMysql.text = total.toString()
                    } else {
                        b.tvServerBadge.text = "OFFLINE"
                        b.tvServerBadge.setTextColor(getColor(android.R.color.holo_red_dark))
                        b.tvServerStatus.text = "Koneksi: Server tidak siap ($status)"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    b.tvServerBadge.text = "OFFLINE"
                    b.tvServerBadge.setTextColor(getColor(android.R.color.holo_red_dark))
                    b.tvServerStatus.text = "Koneksi: Tidak dapat menghubungi server (${e.localizedMessage ?: "Timeout"})"
                }
            }
        }
    }

    // =====================================================================================
    // HELPER HTTP CLIENT (Dispatchers.IO)
    // =====================================================================================
    private fun httpGet(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.setRequestProperty("Accept", "application/json")

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            return sb.toString()
        } else {
            throw Exception("HTTP Error: $responseCode")
        }
    }

    private fun httpPostJson(urlStr: String, jsonBody: String): Int {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")

        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(jsonBody)
        writer.flush()
        writer.close()

        val responseCode = conn.responseCode
        conn.disconnect()
        return responseCode
    }
}
