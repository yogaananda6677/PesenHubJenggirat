@extends('layouts.app')

@section('title', 'Status Pesanan #' . $order->order_number . ' - PesenHub Jenggirat')

@section('content')
<div class="px-4 py-4 space-y-4">

    <!-- Card 1: Status Pesanan Realtime -->
    <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        
        <div class="flex items-center justify-between pb-3 border-b border-slate-100">
            <div>
                <p class="text-xs text-slate-500 font-medium">Nomor Pesanan</p>
                <h1 class="text-sm font-extrabold text-slate-900 font-mono tracking-tight">{{ $order->order_number }}</h1>
            </div>
            <div id="statusBadgeContainer">
                @if($order->status === 'PENDING')
                    <span class="bg-amber-100 text-amber-800 text-xs font-bold px-3 py-1 rounded-full border border-amber-200 flex items-center gap-1.5 animate-pulse">
                        <span class="w-2 h-2 rounded-full bg-amber-500"></span> Menunggu Konfirmasi
                    </span>
                @elseif($order->status === 'CONFIRMED' || $order->status === 'PREPARING')
                    <span class="bg-emerald-100 text-emerald-800 text-xs font-bold px-3 py-1 rounded-full border border-emerald-200 flex items-center gap-1.5">
                        <span class="w-2 h-2 rounded-full bg-emerald-500"></span> Pesanan Dikonfirmasi
                    </span>
                @elseif($order->status === 'COMPLETED')
                    <span class="bg-blue-100 text-blue-800 text-xs font-bold px-3 py-1 rounded-full border border-blue-200 flex items-center gap-1.5">
                        <span class="w-2 h-2 rounded-full bg-blue-500"></span> Selesai / Diambil
                    </span>
                @else
                    <span class="bg-slate-100 text-slate-800 text-xs font-bold px-3 py-1 rounded-full">
                        {{ $order->status }}
                    </span>
                @endif
            </div>
        </div>

        <!-- Info Card Banner Berdasarkan Status -->
        <div id="statusInfoBanner">
            @if($order->status === 'PENDING')
                <div class="bg-amber-50/80 border border-amber-200 rounded-xl p-4 text-center space-y-2">
                    <div class="inline-flex w-10 h-10 rounded-full bg-amber-100 text-amber-600 items-center justify-center text-lg animate-bounce">
                        ⏳
                    </div>
                    <h3 class="text-sm font-bold text-amber-900">Pesanan Sedang Menunggu Konfirmasi</h3>
                    <p class="text-xs text-amber-700 leading-relaxed max-w-sm mx-auto">
                        Outlet Jenggirat Kediri sedang memeriksa dan menyiapkan pesanan Anda. 
                        <strong>Barcode pengambilan akan otomatis muncul di layar ini begitu pesanan dikonfirmasi.</strong>
                    </p>
                    <p class="text-[11px] text-amber-600/80 pt-1">
                        Halaman ini memantau status secara otomatis setiap 3 detik...
                    </p>
                </div>
            @else
                <div class="bg-emerald-50/80 border border-emerald-200 rounded-xl p-3.5 text-center">
                    <p class="text-xs font-bold text-emerald-800">Pesanan Anda telah dikonfirmasi dan siap diambil!</p>
                    <p class="text-[11px] text-emerald-600 mt-0.5">Tunjukkan Barcode di bawah ini saat pengambilan pesanan di outlet.</p>
                </div>
            @endif
        </div>

        <!-- Card Barcode Pengambilan (Muncul Otomatis Saat CONFIRMED) -->
        <div id="barcodeSection" class="{{ ($order->status === 'PENDING') ? 'hidden' : '' }} space-y-3 pt-2 border-t border-slate-100">
            <div class="bg-slate-900 text-white rounded-2xl p-6 text-center shadow-lg border border-slate-800 space-y-4">
                <div>
                    <span class="inline-block bg-primary text-white text-[10px] font-extrabold uppercase px-3 py-0.5 rounded-full mb-1">
                        Tiket Barcode Pengambilan
                    </span>
                    <h2 class="text-base font-extrabold text-white">Barcode Pengambilan Pesanan</h2>
                    <p class="text-xs text-slate-400">Tunjukkan barcode ini saat mengambil pesanan di outlet Martabak Jenggirat</p>
                </div>

                <!-- Gambar Barcode / QR Code -->
                <div class="bg-white p-4 rounded-xl inline-block mx-auto shadow-inner border border-slate-200">
                    <img id="barcodeImage" 
                         src="{{ $order->barcode_url ?: asset('storage/barcodes/barcode_' . preg_replace('/[^A-Za-z0-9_\-]/', '_', $order->order_number) . '.png') }}" 
                         alt="Barcode Pesanan {{ $order->order_number }}" 
                         class="w-48 h-48 mx-auto object-contain">
                    <p class="text-[11px] font-mono font-bold text-slate-700 mt-2 tracking-wider">{{ $order->order_number }}</p>
                </div>

                <!-- Tombol Unduh Barcode -->
                <div class="pt-1 flex flex-col sm:flex-row items-center justify-center gap-2">
                    <a id="btnDownloadBarcode" 
                       href="{{ $order->barcode_url ?: asset('storage/barcodes/barcode_' . preg_replace('/[^A-Za-z0-9_\-]/', '_', $order->order_number) . '.png') }}" 
                       download="barcode_{{ $order->order_number }}.png"
                       class="w-full sm:w-auto bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs px-5 py-2.5 rounded-xl transition flex items-center justify-center gap-1.5 shadow">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                        </svg>
                        <span>Unduh Barcode (Simpan di HP)</span>
                    </a>
                </div>

                <!-- Instruksi Pembayaran & Pick-Up -->
                <div class="bg-slate-800/80 p-3 rounded-xl text-left border border-slate-700/60 space-y-1">
                    <div class="flex items-center justify-between text-xs">
                        <span class="text-slate-400">Status Pembayaran:</span>
                        @if($order->payment_status === 'PAID')
                            <span class="font-bold text-emerald-400">LUNAS (QRIS)</span>
                        @else
                            <span class="font-bold text-amber-400">Bayar di Tempat (Saat Ambil)</span>
                        @endif
                    </div>
                    <div class="flex items-center justify-between text-xs">
                        <span class="text-slate-400">Estimasi Siap:</span>
                        <span class="font-bold text-white">{{ $order->pickup_time }}</span>
                    </div>
                    <div class="flex items-center justify-between text-xs">
                        <span class="text-slate-400">Lokasi Ambil:</span>
                        <span class="font-bold text-white">Outlet Jenggirat Kediri</span>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <!-- Card 2: Rincian Pesanan Pelanggan -->
    <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <h3 class="font-extrabold text-sm text-slate-900 border-b border-slate-100 pb-2">Rincian Menu Pesanan</h3>

        <div class="space-y-3">
            @foreach($order->items as $item)
                <div class="flex items-start justify-between text-xs">
                    <div>
                        <p class="font-bold text-slate-800">{{ $item->quantity }}x {{ $item->menu_name }}</p>
                        @if(!empty($item->toppings_json))
                            <p class="text-[11px] text-slate-500 mt-0.5">+ Topping: {{ implode(', ', $item->toppings_json) }}</p>
                        @endif
                        @if($item->notes)
                            <p class="text-[10px] text-amber-600 italic mt-0.5">Catatan: {{ $item->notes }}</p>
                        @endif
                        <p class="text-[11px] text-slate-400 mt-0.5">@ Rp {{ number_format($item->unit_price, 0, ',', '.') }}</p>
                    </div>
                    <p class="font-extrabold text-slate-900">Rp {{ number_format($item->subtotal, 0, ',', '.') }}</p>
                </div>
            @endforeach
        </div>

        <div class="border-t border-slate-100 pt-3 space-y-1.5">
            <div class="flex items-center justify-between text-xs text-slate-600">
                <span>Total Item</span>
                <span class="font-bold">{{ $order->total_items }} item</span>
            </div>
            <div class="flex items-center justify-between text-sm font-extrabold text-slate-900 pt-1 border-t border-slate-100">
                <span>Total Pembayaran</span>
                <span class="text-primary text-base">Rp {{ number_format($order->total_price, 0, ',', '.') }}</span>
            </div>
        </div>

        <!-- Identitas Pemesan -->
        <div class="bg-slate-50 p-3 rounded-xl border border-slate-100 text-xs space-y-1 text-slate-600">
            <p><strong class="text-slate-800">Nama Pemesan:</strong> {{ $order->customer_name }}</p>
            <p><strong class="text-slate-800">WhatsApp:</strong> {{ $order->customer_phone }}</p>
            @if($order->notes)
                <p><strong class="text-slate-800">Catatan Khusus:</strong> {{ $order->notes }}</p>
            @endif
        </div>

        <div class="text-center pt-2">
            <a href="{{ route('order.menu') }}" class="text-xs text-slate-500 hover:text-primary font-semibold underline">
                ← Kembali ke Halaman Menu
            </a>
        </div>
    </div>

</div>
@endsection

@push('scripts')
<script>
    const orderNumber = "{{ $order->order_number }}";
    let currentStatus = "{{ $order->status }}";

    // Polling realtime status pesanan setiap 3 detik
    const pollInterval = setInterval(() => {
        fetch(`/api/pesanan/${orderNumber}/status`)
            .then(res => res.json())
            .then(data => {
                if (data.status && data.status !== currentStatus) {
                    currentStatus = data.status;
                    // Reload halaman jika status berubah agar barcode dan status card ter-render sempurna
                    window.location.reload();
                }
            })
            .catch(err => console.log('Polling check:', err));
    }, 3000);
</script>
@endpush
