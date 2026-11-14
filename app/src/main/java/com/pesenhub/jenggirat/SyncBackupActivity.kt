package com.pesenhub.jenggirat

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
 *    koneksi HTTP REST API ke MySQL Backup Server, dan Cloud Firestore).
 * 3. lifecycleScope.launch: Memulai Coroutine yang terikat siklus hidup Activity agar
 *    tidak terjadi memory leak.
 * 4. async & await(): Menjalankan dua proses asinkron secara paralel / bersamaan dan
 *    menunggu hasil kedua proses tersebut.
 * 5. Mutex (Mutual Exclusion): Mekanisme penguncian thread-safe untuk mengamankan data
 *    counter dari race condition antar thread.
 * 6. withContext(): Berpindah thread (dari IO ke Main) di tengah jalan lalu kembali lagi.
 */
class SyncBackupActivity : AppCompatActivity() {

    private lateinit var b: ActivitySyncBackupBinding

    // [MODUL 12: KOTLIN UTILITY - BY LAZY]
    // Inisialisasi variabel hanya saat pertama kali diakses
    private val dbHelper: DBOpenHelper by lazy { DBOpenHelper(this) }
    private val firestoreDb: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var cancelProcess = false

    // =====================================================================================
    // [MODUL 12: MUTEX THREAD-SAFE DATA COUNTER (Halaman 7 Modul)]
    // =====================================================================================
    class DataCounter {
        var value = 0
        val mutex = Mutex()

        suspend fun increment() {
            mutex.withLock {
                value++
            }
        }

        suspend fun get(): Int {
            return mutex.withLock {
                value
            }
        }

        suspend fun reset() {
            mutex.withLock {
                value = 0
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

        // Load saved server URL from SharedPreferences
        val pref = getSharedPreferences("SyncConfig", Context.MODE_PRIVATE)
        val savedUrl = (pref.getString("server_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000")
            .replace("`", "")
            .trim()
        b.edServerUrl.setText(savedUrl)

        // Cek status server awal di background
        ujiKoneksiServer(getServerBaseUrl())

        // 1. Tombol Mulai Kirim / Sync dengan Thread (Coroutine)
        b.btnStart.setOnClickListener {
            cancelProcess = false
            simpanServerUrl()
            mulaiSyncDenganCoroutine()
        }

        // 2. Tombol Pulihkan Data dari MySQL Server (Restore)
        b.btnRestore.setOnClickListener {
            cancelProcess = false
            simpanServerUrl()
            mulaiRestoreDariServerMySQL()
        }

        // 3. Tombol Batal (Cancel)
        b.btnCancel.setOnClickListener {
            cancelProcess = true
            tambahLog("[User] Proses dibatalkan oleh pengguna (cancelProcess = true)")
            Toast.makeText(this, "Proses sinkronisasi dibatalkan!", Toast.LENGTH_SHORT).show()
        }

        // 5. Tombol Info Modul Threads
        b.btnInfo.setOnClickListener {
            tampilkanDialogInfoModul()
        }
    }

    private fun getServerBaseUrl(): String {
        val raw = b.edServerUrl.text?.toString() ?: ""
        return raw.trim()
            .replace("`", "")
            .replace("'", "")
            .replace("\"", "")
            .removeSuffix("/")
            .ifEmpty { "http://10.0.2.2:8000" }
    }

    private fun simpanServerUrl() {
        val url = getServerBaseUrl()
        b.edServerUrl.setText(url)
        getSharedPreferences("SyncConfig", Context.MODE_PRIVATE)
            .edit()
            .putString("server_url", url)
            .apply()
    }

    // =====================================================================================
    // [MODUL 12: EKSEKUSI SINKRONISASI MENGGUNAKAN COROUTINE THREAD (Halaman 8 Modul)]
    // =====================================================================================
    private fun mulaiSyncDenganCoroutine() {
        // Ambil data riwayat transaksi dari SQLite lokal
        val daftarRiwayat = ambilRiwayatDariSQLite()

        if (daftarRiwayat.isEmpty()) {
            Toast.makeText(this, "Tidak ada data riwayat transaksi lokal untuk di-backup!", Toast.LENGTH_SHORT).show()
            tambahLog("[Sync] SQLite lokal kosong. Silakan selesaikan pesanan kasir terlebih dahulu.")
            return
        }

        val totalData = daftarRiwayat.size
        b.progressFirebase.max = totalData
        b.progressSqlite.max = totalData
        b.progressFirebase.progress = 0
        b.progressSqlite.progress = 0
        b.tvFirebaseCount.text = "0 / $totalData"
        b.tvSqliteCount.text = "0 / $totalData"

        setUiLoading(true)
        tambahLog("==================================================")
        tambahLog("[Coroutine] Memulai sinkronisasi $totalData data ke MySQL & Firestore...")
        tambahLog("[Thread Active] Thread: ${Thread.currentThread().name}")

        val serverBaseUrl = getServerBaseUrl()

        // [MODUL 12: LIFECYCLE SCOPE LAUNCH]
        // Menjalankan coroutine yang otomatis dibatalkan jika Activity ditutup
        lifecycleScope.launch {
            val firebaseCounter = DataCounter()
            val mysqlCounter = DataCounter()

            val waktuMulai = SystemClock.elapsedRealtime()

            for ((index, item) in daftarRiwayat.withIndex()) {
                if (cancelProcess || !isActive) {
                    tambahLog("[Coroutine] Loop dihentikan karena pembatalan.")
                    break
                }

                // [MODUL 12: DELAY NON-BLOCKING]
                // Menjeda coroutine tanpa membekukan UI Thread
                delay(150L)

                // [MODUL 12: DISPATCHERS.IO DENGAN ASYNC & AWAIT]
                // Operasi berat dijalankan secara paralel di IO Thread
                val idPesanan = item["id_pesanan"]?.toString() ?: "ORD-${index + 1}"

                // 1. Paralel Task 1: Pastikan data tersimpan di Cloud Firestore
                val firestoreDeferred = async(Dispatchers.IO) {
                    simpanKeFirestore(item)
                }

                // 2. Paralel Task 2: Kirim data riwayat ke MySQL Web Backup Server via REST API
                val mysqlDeferred = async(Dispatchers.IO) {
                    kirimKeMysqlServer(serverBaseUrl, item)
                }

                // [MODUL 12: AWAIT RESULTS]
                val firestoreBerhasil = try { firestoreDeferred.await() } catch (e: Exception) { false }
                val mysqlBerhasil = try { mysqlDeferred.await() } catch (e: Exception) { false }

                if (firestoreBerhasil) {
                    firebaseCounter.increment()
                }

                if (mysqlBerhasil) {
                    mysqlCounter.increment()
                }

                // [MODUL 12: WITHCONTEXT(DISPATCHERS.MAIN)]
                // Berpindah kembali ke Main Thread untuk memperbarui elemen antarmuka pengguna
                withContext(Dispatchers.Main) {
                    val countFs = firebaseCounter.get()
                    val countMy = mysqlCounter.get()

                    b.progressFirebase.progress = countFs
                    b.tvFirebaseCount.text = "$countFs / $totalData"

                    b.progressSqlite.progress = countMy
                    b.tvSqliteCount.text = "$countMy / $totalData"

                    tambahLog("[Data #${index + 1}] #$idPesanan -> Firestore: ${if (firestoreBerhasil) "OK" else "FAIL"}, MySQL: ${if (mysqlBerhasil) "OK" else "FAIL"}")
                }
            }

            val durasiDetik = (SystemClock.elapsedRealtime() - waktuMulai) / 1000.0

            // Selesai: kembali ke UI Thread
            withContext(Dispatchers.Main) {
                setUiLoading(false)
                val finalMysql = mysqlCounter.get()
                val finalFs = firebaseCounter.get()
                tambahLog("[Selesai] Sinkronisasi Coroutine tuntas dalam ${"%.2f".format(durasiDetik)} detik!")
                tambahLog("[Hasil] Firestore: $finalFs / $totalData | MySQL Server: $finalMysql / $totalData")
                Toast.makeText(
                    this@SyncBackupActivity,
                    "Sukses! $finalMysql transaksi tersinkronisasi ke MySQL Backup Server",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =====================================================================================
    // FITUR SYNC RESTORE: Mengunduh backup dari MySQL dan pulihkan ke SQLite / Firestore
    // =====================================================================================
    private fun mulaiRestoreDariServerMySQL() {
        val serverBaseUrl = getServerBaseUrl()
        setUiLoading(true)
        tambahLog("==================================================")
        tambahLog("[Restore] Menghubungi server MySQL ($serverBaseUrl/api/backup/orders)...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonString = httpGet("$serverBaseUrl/api/backup/orders")
                val rootJson = JSONObject(jsonString)
                val isSuccess = rootJson.optBoolean("success", false)
                val dataArray = rootJson.optJSONArray("data") ?: JSONArray()
                val totalServer = dataArray.length()

                withContext(Dispatchers.Main) {
                    tambahLog("[Restore] Ditemukan $totalServer data riwayat di server backup MySQL.")
                    b.progressSqlite.max = totalServer
                    b.progressSqlite.progress = 0
                }

                var restoredCount = 0
                val db = dbHelper.writableDatabase

                for (i in 0 until totalServer) {
                    if (cancelProcess || !isActive) break
                    delay(100L)

                    val obj = dataArray.getJSONObject(i)
                    val idPesanan = obj.getString("order_number")
                    val nama = obj.optString("customer_name", "Pelanggan")
                    val hp = obj.optString("customer_phone", "-")
                    val menu = obj.optString("detail_item", "-")
                    val metode = obj.optString("payment_method", "Tunai")
                    val total = obj.optLong("total_price", 0L)
                    val waktu = obj.optString("waktu_transaksi", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))

                    // Insert or replace ke SQLite lokal
                    val sql = "insert or replace into riwayat_transaksi(" +
                            "id_pesanan, nama_pelanggan, hp_pelanggan, detail_item, metode_bayar, total_bayar, status_order, waktu_selesai) " +
                            "values (?, ?, ?, ?, ?, ?, ?, ?)"
                    db.execSQL(sql, arrayOf(idPesanan, nama, hp, menu, metode, total, "COMPLETED", waktu))
                    restoredCount++

                    withContext(Dispatchers.Main) {
                        b.progressSqlite.progress = restoredCount
                        b.tvSqliteCount.text = "$restoredCount / $totalServer"
                        tambahLog("[Pulihkan #${i + 1}] #$idPesanan ($nama) berhasil di-restore ke SQLite lokal.")
                    }
                }

                withContext(Dispatchers.Main) {
                    setUiLoading(false)
                    tambahLog("[Restore Selesai] Total $restoredCount transaksi telah dipulihkan ke SQLite lokal.")
                    Toast.makeText(this@SyncBackupActivity, "Berhasil memulihkan $restoredCount data dari MySQL!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setUiLoading(false)
                    tambahLog("[Restore Gagal] Terjadi kesalahan: ${e.message}")
                    Toast.makeText(this@SyncBackupActivity, "Gagal memulihkan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // =====================================================================================
    // HELPER OPERASI JARINGAN & DATABASE
    // =====================================================================================
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

            // Menggunakan coroutine suspend extension untuk Firestore
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
                    b.tvServerStatus.text = "Status Server: $status ($conn) • $total data tersimpan di MySQL"
                    b.tvServerStatus.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    b.tvServerStatus.text = "Status Server: Offline / Tidak Terhubung (${e.localizedMessage ?: "Cek URL"})"
                    b.tvServerStatus.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                }
            }
        }
    }

    private fun httpPostJson(urlStr: String, jsonBody: String): Int {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 4000
            readTimeout = 4000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        return conn.responseCode
    }

    private fun httpGet(urlStr: String): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("Accept", "application/json")
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        return response.toString()
    }

    private fun setUiLoading(isLoading: Boolean) {
        b.btnStart.isEnabled = !isLoading
        b.btnRestore.isEnabled = !isLoading
    }

    private fun tambahLog(pesan: String) {
        val waktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logBaru = "[$waktu] $pesan\n${b.tvConsoleLog.text}"
        b.tvConsoleLog.text = logBaru
    }

    private fun tampilkanDialogInfoModul() {
        AlertDialog.Builder(this)
            .setTitle("Info Modul 12: Threads (Coroutine)")
            .setMessage(
                "Materi Praktikum Pemrograman Mobile Lanjut:\n\n" +
                        "1. Mengapa Menggunakan Threads?\n" +
                        "Untuk menghindari ANR (Application Not Responding) dan menjaga UI tetap responsif saat melakukan operasi berat (akses jaringan, database, IO).\n\n" +
                        "2. Coroutine (Lightweight Thread):\n" +
                        "Eksekusi asinkron yang dapat di-suspend dan di-resume tanpa memblokir thread sistem.\n\n" +
                        "3. Dispatchers:\n" +
                        "• Dispatchers.IO: Operasi database, jaringan, dan berkas.\n" +
                        "• Dispatchers.Main: Operasi antarmuka/UI.\n\n" +
                        "4. async & await():\n" +
                        "Menjalankan beberapa background task secara paralel (concurrent) lalu menunggu hasilnya terkumpul.\n\n" +
                        "5. Mutex:\n" +
                        "Penguncian aman thread (thread-safety) pada data counter bersama."
            )
            .setPositiveButton("Tutup", null)
            .show()
    }
}
