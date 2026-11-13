@extends('layouts.app')

@section('title', 'Backup & Sync Server (MySQL) - Modul 12 Threads Coroutine')

@section('content')
<main class="flex-1 max-w-4xl mx-auto px-4 py-8 w-full">
    <!-- Header Banner Modul 12 -->
    <div class="bg-gradient-to-r from-orange-600 via-amber-600 to-amber-500 rounded-3xl p-6 text-white shadow-xl mb-8 relative overflow-hidden">
        <div class="relative z-10">
            <div class="inline-flex items-center gap-2 bg-white/20 backdrop-blur-md px-3 py-1 rounded-full text-xs font-semibold mb-3">
                <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
                Tugas Praktikum Modul 12: Threads (Coroutine)
            </div>
            <h1 class="text-2xl sm:text-3xl font-extrabold tracking-tight mb-2">
                Server Backup MySQL &amp; Sinkronisasi Data
            </h1>
            <p class="text-orange-100 text-sm max-w-2xl leading-relaxed">
                Menyimpan salinan riwayat transaksi dari Cloud Firestore &amp; SQLite Android ke Server MySQL.
                Proses sinkronisasi di aplikasi Mobile dijalankan pada <strong>Background Thread (Dispatchers.IO)</strong> menggunakan <strong>Kotlin Coroutines</strong> agar antarmuka (UI Thread) tetap responsif tanpa ANR.
            </p>
        </div>
        <div class="absolute -right-8 -bottom-10 w-44 h-44 bg-white/10 rounded-full blur-2xl"></div>
    </div>

    <!-- Statistik Server Backup -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center font-bold text-2xl">
                🗄️
            </div>
            <div>
                <span class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Status Database</span>
                <div class="flex items-center gap-2 mt-0.5">
                    <span class="font-extrabold text-slate-800 text-lg uppercase">{{ $dbDriver }}</span>
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                        {{ $dbName }}
                    </span>
                </div>
            </div>
        </div>

        <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-orange-100 text-primary flex items-center justify-center font-bold text-2xl">
                📦
            </div>
            <div>
                <span class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Riwayat Backup</span>
                <p class="font-extrabold text-slate-800 text-xl mt-0.5">{{ $totalOrders }} <span class="text-xs font-normal text-slate-500">transaksi</span></p>
            </div>
        </div>

        <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-2xl">
                💰
            </div>
            <div>
                <span class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Nilai Transaksi</span>
                <p class="font-extrabold text-slate-800 text-xl mt-0.5">Rp {{ number_format($totalRevenue, 0, ',', '.') }}</p>
            </div>
        </div>
    </div>

    <!-- Tabel Data Riwayat di Server Backup MySQL -->
    <div class="bg-white rounded-3xl border border-slate-200/80 shadow-sm overflow-hidden mb-10">
        <div class="px-6 py-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-50/50">
            <div>
                <h2 class="font-bold text-slate-900 text-base">Daftar Transaksi Tersimpan di Server MySQL</h2>
                <p class="text-xs text-slate-500">Tersinkronisasi dari aplikasi mobile PesenHub Jenggirat</p>
            </div>
            <div class="flex items-center gap-2">
                <a href="{{ route('backup.index') }}" class="px-3 py-1.5 bg-white border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-sm flex items-center gap-1.5">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                    Segarkan Data
                </a>
            </div>
        </div>

        @if($backupOrders->isEmpty())
            <div class="text-center py-12 px-4">
                <div class="w-16 h-16 mx-auto mb-3 rounded-full bg-slate-100 flex items-center justify-center text-3xl">📭</div>
                <h3 class="font-bold text-slate-700 text-base">Belum Ada Data di Server Backup MySQL</h3>
                <p class="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                    Buka menu <strong>Pengaturan &gt; Sinkronisasi Data</strong> pada aplikasi Android, lalu tekan <strong>"Backup ke MySQL (Coroutine Thread)"</strong> untuk mengirim data.
                </p>
            </div>
        @else
            <div class="overflow-x-auto">
                <table class="w-full text-left text-sm text-slate-700">
                    <thead class="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500 border-b border-slate-100">
                        <tr>
                            <th class="px-5 py-3">No. Pesanan</th>
                            <th class="px-5 py-3">Pelanggan</th>
                            <th class="px-5 py-3">Detail Menu</th>
                            <th class="px-5 py-3">Total</th>
                            <th class="px-5 py-3">Waktu Transaksi</th>
                            <th class="px-5 py-3">Metode Sync</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-100 font-medium text-xs">
                        @foreach($backupOrders as $order)
                        <tr class="hover:bg-slate-50/70 transition">
                            <td class="px-5 py-3.5 font-bold text-primary">#{{ $order->order_number }}</td>
                            <td class="px-5 py-3.5">
                                <div class="font-bold text-slate-800">{{ $order->customer_name }}</div>
                                <div class="text-[11px] text-slate-400">{{ $order->customer_phone ?? '-' }}</div>
                            </td>
                            <td class="px-5 py-3.5 max-w-xs truncate text-slate-600" title="{{ $order->detail_item }}">
                                {{ $order->detail_item }}
                            </td>
                            <td class="px-5 py-3.5 font-bold text-slate-900">
                                Rp {{ number_format($order->total_price, 0, ',', '.') }}
                            </td>
                            <td class="px-5 py-3.5 text-slate-500">
                                {{ $order->waktu_transaksi ?? $order->created_at->format('d/m/Y H:i') }}
                            </td>
                            <td class="px-5 py-3.5">
                                <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
                                    ⚡ {{ $order->sync_source }}
                                </span>
                            </td>
                        </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>

            <div class="px-6 py-4 border-t border-slate-100">
                {{ $backupOrders->links() }}
            </div>
        @endif
    </div>

    <!-- PANEL PENJELASAN CODING THREADING COROUTINE (UNTUK PRESENTASI DOSEN) -->
    <div class="bg-slate-900 text-white rounded-3xl p-6 sm:p-8 shadow-xl border border-slate-800">
        <div class="flex items-center gap-3 mb-4">
            <span class="px-3 py-1 rounded-full bg-primary/20 text-primary border border-primary/30 text-xs font-bold uppercase tracking-wider">
                Dokumentasi &amp; Penjelasan Kode
            </span>
            <span class="text-xs text-slate-400">Modul 12 Pemrograman Mobile Lanjut</span>
        </div>
        <h2 class="text-xl sm:text-2xl font-black mb-3 text-white">
            Penjelasan Implementasi Kotlin Coroutines &amp; Threading
        </h2>
        <p class="text-slate-300 text-sm mb-6 leading-relaxed">
            Berikut adalah poin-poin penting kode Android di file <code class="bg-slate-800 px-2 py-0.5 rounded text-amber-400 font-mono text-xs">SyncBackupActivity.kt</code> yang digunakan saat presentasi tugas kelompok:
        </p>

        <div class="space-y-5">
            <!-- 1. Dispatchers.IO vs Main -->
            <div class="bg-slate-800/80 rounded-2xl p-4 border border-slate-700">
                <h3 class="font-bold text-amber-400 text-sm mb-1">1. Dispatchers.IO vs Dispatchers.Main (Pemisahan Thread)</h3>
                <p class="text-xs text-slate-300 mb-2">
                    Operasi jaringan (HTTP ke MySQL API dan Firestore) dan disk database dijalankan di <code class="text-primary font-mono">Dispatchers.IO</code>. Pembaruan ProgressBar dan teks UI dialihkan kembali ke <code class="text-primary font-mono">Dispatchers.Main</code> menggunakan <code class="text-primary font-mono">withContext(Dispatchers.Main)</code> agar tidak terjadi ANR.
                </p>
                <pre class="bg-slate-950 p-3 rounded-xl text-[11px] font-mono text-slate-300 overflow-x-auto leading-relaxed"><code>// Dijalankan di background thread (IO)
lifecycleScope.launch(Dispatchers.IO) {
    val orders = dbLocal.query(...) // Baca database lokal
    val result = apiService.pushBackup(orders) // Kirim ke MySQL Web API

    // Kembali ke Main Thread (UI Thread) untuk update tampilan
    withContext(Dispatchers.Main) {
        b.progressMysql.progress = count
        b.tvMysqlCount.text = "$count / $total"
    }
}</code></pre>
            </div>

            <!-- 2. async & await -->
            <div class="bg-slate-800/80 rounded-2xl p-4 border border-slate-700">
                <h3 class="font-bold text-amber-400 text-sm mb-1">2. async &amp; await() (Eksekusi Paralel / Concurrent)</h3>
                <p class="text-xs text-slate-300 mb-2">
                    Sesuai Halaman 8 Modul Pak Benni, <code class="text-primary font-mono">async</code> digunakan untuk menjalankan dua tugas secara paralel (misal: cek data di Firestore dan simpan ke MySQL), lalu hasilnya ditunggu dengan <code class="text-primary font-mono">await()</code>.
                </p>
                <pre class="bg-slate-950 p-3 rounded-xl text-[11px] font-mono text-slate-300 overflow-x-auto leading-relaxed"><code>val firestoreDeferred = async { simpanKeFirestore(item) }
val mysqlDeferred = async { kirimKeMySQLServer(item) }

// Menunggu hasil kedua thread paralel selesai
val isFsOk = firestoreDeferred.await()
val isMyOk = mysqlDeferred.await()</code></pre>
            </div>

            <!-- 3. Mutex -->
            <div class="bg-slate-800/80 rounded-2xl p-4 border border-slate-700">
                <h3 class="font-bold text-amber-400 text-sm mb-1">3. Mutex (Thread-Safe Synchronization)</h3>
                <p class="text-xs text-slate-300 mb-2">
                    Digunakan untuk mengunci variable counter agar aman dari <em>race condition</em> saat banyak thread memproses data secara bersamaan (Modul Halaman 7).
                </p>
                <pre class="bg-slate-950 p-3 rounded-xl text-[11px] font-mono text-slate-300 overflow-x-auto leading-relaxed"><code>class DataCounter {
    var value = 0
    val mutex = Mutex()

    suspend fun increment() = mutex.withLock { value++ }
    suspend fun get(): Int = mutex.withLock { return value }
}</code></pre>
            </div>

            <!-- 4. Mengapa Threads Coroutine Sangat Penting? (Bebas ANR) -->
            <div class="bg-slate-800/80 rounded-2xl p-4 border border-slate-700">
                <h3 class="font-bold text-emerald-400 text-sm mb-1">4. Menghindari ANR (Application Not Responding)</h3>
                <p class="text-xs text-slate-300">
                    Semua operasi berat (baca/tulis SQLite lokal, sinkronisasi Firestore, dan HTTP REST API ke Server MySQL) dieksekusi di background thread (<code class="text-primary font-mono">Dispatchers.IO</code>). Hal ini menjaga Main Thread (UI Thread) tetap bebas untuk merender tampilan dan menerima interaksi pengguna, sehingga aplikasi tidak pernah macet (*freeze*) atau mengalami <em>Application Not Responding (ANR)</em>.
                </p>
            </div>
        </div>
    </div>
</main>
@endsection
