@extends('layouts.app')

@section('title', 'Panel Antrean & Konfirmasi Kasir - PesenHub Jenggirat')

@section('content')
<div class="px-4 py-4 space-y-5">

    <!-- Header Panel Kasir -->
    <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div>
            <span class="inline-block bg-slate-900 text-white text-[10px] font-bold px-2 py-0.5 rounded mb-1">
                KASIR POS WEB
            </span>
            <h1 class="text-base font-extrabold text-slate-900">Konfirmasi Pesanan Pelanggan</h1>
            <p class="text-xs text-slate-500">Konfirmasi pesanan masuk web, generate barcode &amp; upload Supabase</p>
        </div>
        <a href="{{ route('order.menu') }}" class="text-xs bg-amber-50 text-amber-800 border border-amber-200 px-3 py-1.5 rounded-lg font-bold hover:bg-amber-100 transition">
            + Menu Web
        </a>
    </div>

    <!-- Section 1: Pesanan Menunggu Konfirmasi (PENDING) -->
    <div class="space-y-3">
        <div class="flex items-center justify-between">
            <h2 class="text-xs font-extrabold text-slate-900 uppercase tracking-wider flex items-center gap-2">
                <span>Menunggu Konfirmasi Kasir</span>
                <span class="bg-amber-100 text-amber-800 text-[11px] font-extrabold px-2 py-0.5 rounded-full border border-amber-200">
                    {{ $ordersPending->count() }}
                </span>
            </h2>
            <button onclick="window.location.reload()" class="text-xs text-primary font-bold hover:underline">
                Refresh ⟳
            </button>
        </div>

        @if($ordersPending->isEmpty())
            <div class="bg-white p-6 rounded-2xl border border-slate-200 text-center text-slate-400">
                <p class="text-2xl mb-1">☕</p>
                <p class="text-xs font-semibold">Tidak ada pesanan web baru yang menunggu konfirmasi.</p>
            </div>
        @else
            <div class="space-y-3">
                @foreach($ordersPending as $order)
                    <div class="bg-white p-4 rounded-2xl border-2 border-amber-300 shadow-sm space-y-3">
                        <div class="flex items-start justify-between gap-2">
                            <div>
                                <span class="bg-amber-100 text-amber-800 text-[10px] font-extrabold px-2 py-0.5 rounded-full border border-amber-200">
                                    PENDING WEB
                                </span>
                                <h3 class="font-extrabold text-slate-900 text-sm mt-1 font-mono">{{ $order->order_number }}</h3>
                                <p class="text-xs text-slate-700 font-bold mt-0.5">{{ $order->customer_name }} • {{ $order->customer_phone }}</p>
                            </div>
                            <div class="text-right">
                                <p class="text-sm font-extrabold text-primary">Rp {{ number_format($order->total_price, 0, ',', '.') }}</p>
                                <p class="text-[10px] text-slate-400">{{ $order->created_at->diffForHumans() }}</p>
                            </div>
                        </div>

                        <!-- Daftar Item Pesanan -->
                        <div class="bg-slate-50 p-2.5 rounded-xl border border-slate-100 space-y-1 text-xs">
                            @foreach($order->items as $it)
                                <div class="flex justify-between text-slate-700">
                                    <span>
                                        <strong>{{ $it->quantity }}x</strong> {{ $it->menu_name }}
                                        @if(!empty($it->toppings_json))
                                            <span class="text-slate-500">({{ implode(', ', $it->toppings_json) }})</span>
                                        @endif
                                    </span>
                                    <span class="font-semibold text-slate-900">Rp {{ number_format($it->subtotal, 0, ',', '.') }}</span>
                                </div>
                            @endforeach
                            @if($order->notes)
                                <p class="text-[11px] text-amber-700 pt-1 border-t border-slate-200 italic">
                                    Catatan: {{ $order->notes }}
                                </p>
                            @endif
                        </div>

                        <!-- Info Bayar & Estimasi -->
                        <div class="flex items-center justify-between text-xs text-slate-500 px-1">
                            <span>Metode: <strong class="text-slate-800">{{ $order->payment_method }}</strong></span>
                            <span>Estimasi: <strong class="text-slate-800">{{ $order->pickup_time }}</strong></span>
                        </div>

                        <!-- Tombol Konfirmasi & Upload Barcode -->
                        <form action="{{ route('kasir.order.confirm', $order->order_number) }}" method="POST">
                            @csrf
                            <button type="submit" 
                                    class="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-xs py-3 rounded-xl transition shadow flex items-center justify-center gap-2">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                                </svg>
                                <span>Konfirmasi &amp; Generate Barcode (Upload Supabase)</span>
                            </button>
                        </form>
                    </div>
                @endforeach
            </div>
        @endif
    </div>

    <!-- Section 2: Pesanan Dikonfirmasi / Siap Ambil (CONFIRMED) -->
    <div class="space-y-3 pt-3 border-t border-slate-200">
        <h2 class="text-xs font-extrabold text-slate-900 uppercase tracking-wider flex items-center gap-2">
            <span>Sedang Disiapkan / Siap Ambil</span>
            <span class="bg-emerald-100 text-emerald-800 text-[11px] font-extrabold px-2 py-0.5 rounded-full border border-emerald-200">
                {{ $ordersActive->count() }}
            </span>
        </h2>

        @if($ordersActive->isEmpty())
            <div class="bg-white p-4 rounded-2xl border border-slate-200 text-center text-slate-400 text-xs">
                Tidak ada pesanan yang sedang dipersiapkan.
            </div>
        @else
            <div class="space-y-3">
                @foreach($ordersActive as $order)
                    <div class="bg-white p-4 rounded-2xl border border-emerald-200 shadow-sm space-y-3">
                        <div class="flex items-start justify-between gap-2">
                            <div>
                                <span class="bg-emerald-100 text-emerald-800 text-[10px] font-extrabold px-2 py-0.5 rounded-full">
                                    BARCODE SIAP • CONFIRMED
                                </span>
                                <h3 class="font-extrabold text-slate-900 text-sm mt-1 font-mono">{{ $order->order_number }}</h3>
                                <p class="text-xs text-slate-700 font-bold mt-0.5">{{ $order->customer_name }} • {{ $order->customer_phone }}</p>
                            </div>
                            <!-- Barcode Thumbnail -->
                            @if($order->barcode_url)
                                <a href="{{ $order->barcode_url }}" target="_blank" class="block border border-slate-200 rounded-lg p-1 bg-white hover:border-primary">
                                    <img src="{{ $order->barcode_url }}" alt="Barcode" class="w-12 h-12 object-contain">
                                </a>
                            @endif
                        </div>

                        <!-- Detail Item Ringkas -->
                        <div class="text-xs text-slate-600 bg-slate-50 p-2.5 rounded-xl">
                            <p class="font-semibold text-slate-800">{{ $order->total_items }} item • Total: Rp {{ number_format($order->total_price, 0, ',', '.') }}</p>
                            <p class="text-[11px] text-slate-500 mt-0.5">Status Bayar: <strong>{{ $order->payment_status }}</strong> ({{ $order->payment_method }})</p>
                        </div>

                        <!-- Tombol Selesaikan Pesanan (bisa juga via scanner kamera Android) -->
                        <form action="{{ route('kasir.order.complete', $order->order_number) }}" method="POST">
                            @csrf
                            <button type="submit" 
                                    class="w-full bg-slate-800 hover:bg-slate-900 text-white font-bold text-xs py-2.5 rounded-xl transition flex items-center justify-center gap-1.5">
                                <span>Verifikasi Selesai &amp; Tandai Lunas</span>
                            </button>
                        </form>
                    </div>
                @endforeach
            </div>
        @endif
    </div>

</div>
@endsection
