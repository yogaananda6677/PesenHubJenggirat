@extends('layouts.app')

@section('title', 'Kelola Menu & Harga Multi-Channel - PesenHub Jenggirat')

@section('content')
<div class="px-4 py-4 space-y-4">

    <!-- Header Panel -->
    <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div>
            <div class="flex items-center gap-2 mb-1">
                <span class="bg-slate-900 text-white text-[10px] font-bold px-2 py-0.5 rounded">KATALOG POS</span>
                <span class="bg-amber-100 text-amber-800 text-[10px] font-bold px-2 py-0.5 rounded border border-amber-200">5 Channel Penjualan</span>
            </div>
            <h1 class="text-base font-extrabold text-slate-900 tracking-tight">Kelola Menu &amp; Harga Multi-Channel</h1>
            <p class="text-xs text-slate-500">Atur ketersediaan menu dan harga dinamis untuk Offline, Web, GoFood, GrabFood, &amp; ShopeeFood</p>
        </div>
        <a href="{{ route('kasir.menu.create') }}" 
           class="bg-primary hover:bg-primary-dark text-white font-bold text-xs px-4 py-2.5 rounded-xl transition shadow flex items-center gap-1.5 flex-shrink-0">
            <span>+ Tambah Menu Baru</span>
        </a>
    </div>

    <!-- Filter & Search Toolbar -->
    <div class="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <!-- Search Form -->
        <form action="{{ route('kasir.menu.index') }}" method="GET" class="flex gap-2">
            <input type="hidden" name="kategori" value="{{ $kategoriAktif }}">
            <div class="relative flex-1">
                <input type="text" name="q" value="{{ $keyword }}" placeholder="Cari nama menu, SKU, atau deskripsi..." 
                       class="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-primary focus:bg-white">
                <svg class="w-4 h-4 text-slate-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                </svg>
            </div>
            <button type="submit" class="bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold px-4 py-2 rounded-xl transition">
                Cari
            </button>
            @if($keyword)
                <a href="{{ route('kasir.menu.index', ['kategori' => $kategoriAktif]) }}" class="bg-slate-100 text-slate-600 text-xs font-semibold px-3 py-2 rounded-xl flex items-center hover:bg-slate-200">
                    Reset
                </a>
            @endif
        </form>

        <!-- Category Chips -->
        <div class="flex items-center gap-2 overflow-x-auto no-scrollbar pt-1">
            @foreach(['Semua', 'Martabak Telur', 'Terang Bulan', 'Minuman'] as $kat)
                <a href="{{ route('kasir.menu.index', ['kategori' => $kat, 'q' => $keyword]) }}"
                   class="px-3.5 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition {{ $kategoriAktif === $kat ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200' }}">
                    {{ $kat }}
                </a>
            @endforeach
        </div>
    </div>

    <!-- Daftar Kartu Menu dengan Multi-Channel Pricing -->
    <div class="space-y-3">
        @if($menus->isEmpty())
            <div class="bg-white p-8 rounded-2xl border border-slate-200 text-center text-slate-400">
                <p class="text-3xl mb-2">🍽️</p>
                <p class="text-sm font-bold text-slate-700">Belum ada menu yang cocok</p>
                <p class="text-xs text-slate-400 mt-1">Silakan tambahkan menu baru atau ubah kata kunci filter</p>
            </div>
        @else
            @foreach($menus as $menu)
                <div class="bg-white p-4 rounded-2xl border {{ $menu->is_available ? 'border-slate-200 hover:border-amber-300' : 'border-slate-200 bg-slate-50/70 opacity-80' }} shadow-sm space-y-3 transition">
                    
                    <!-- Header Kartu Menu -->
                    <div class="flex items-start justify-between gap-3">
                        <div class="flex items-start gap-3 min-w-0 flex-1">
                            <!-- Thumbnail / Foto Menu -->
                            <div class="w-14 h-14 rounded-xl bg-amber-50 border border-amber-100 flex-shrink-0 flex items-center justify-center overflow-hidden">
                                @if($menu->image_url && !str_contains($menu->image_url, 'default_food_icon'))
                                    <img src="{{ $menu->image_url }}" alt="{{ $menu->name }}" class="w-full h-full object-cover">
                                @else
                                    <span class="text-2xl">🥞</span>
                                @endif
                            </div>

                            <div class="min-w-0 flex-1">
                                <div class="flex items-center gap-2 flex-wrap">
                                    <span class="font-mono text-[10px] font-extrabold bg-slate-100 text-slate-600 px-2 py-0.5 rounded">
                                        {{ $menu->sku }}
                                    </span>
                                    <span class="text-[10px] font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                                        {{ $menu->category }}
                                    </span>
                                    @if($menu->is_available)
                                        <span class="bg-emerald-50 text-emerald-700 text-[10px] font-bold px-2 py-0.5 rounded border border-emerald-200">
                                            ● Tersedia
                                        </span>
                                    @else
                                        <span class="bg-rose-50 text-rose-700 text-[10px] font-bold px-2 py-0.5 rounded border border-rose-200">
                                            ✕ Habis
                                        </span>
                                    @endif
                                </div>
                                <h3 class="font-extrabold text-slate-900 text-sm truncate mt-1">{{ $menu->name }}</h3>
                                @if($menu->description)
                                    <p class="text-xs text-slate-500 truncate mt-0.5">{{ $menu->description }}</p>
                                @endif
                                @if($menu->hpp_amount > 0)
                                    <p class="text-[10px] text-slate-400 mt-0.5">HPP: Rp {{ number_format($menu->hpp_amount, 0, ',', '.') }}</p>
                                @endif
                            </div>
                        </div>

                        <!-- Action Buttons -->
                        <div class="flex items-center gap-1.5 flex-shrink-0">
                            <!-- Toggle Ketersediaan -->
                            <form action="{{ route('kasir.menu.toggle', $menu->id) }}" method="POST">
                                @csrf
                                @method('PATCH')
                                <button type="submit" 
                                        title="{{ $menu->is_available ? 'Ubah menjadi Habis' : 'Ubah menjadi Tersedia' }}"
                                        class="p-2 rounded-xl text-xs font-bold transition {{ $menu->is_available ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200' : 'bg-slate-200 text-slate-700 hover:bg-slate-300' }}">
                                    {{ $menu->is_available ? 'Aktif' : 'Non-Aktif' }}
                                </button>
                            </form>

                            <a href="{{ route('kasir.menu.edit', $menu->id) }}" 
                               class="p-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition border border-slate-200">
                                Edit
                            </a>

                            <form action="{{ route('kasir.menu.destroy', $menu->id) }}" method="POST" onsubmit="return confirm('Yakin ingin menghapus menu {{ $menu->name }}?');">
                                @csrf
                                @method('DELETE')
                                <button type="submit" class="p-2 text-rose-600 hover:bg-rose-50 rounded-xl text-xs font-bold transition">
                                    Hapus
                                </button>
                            </form>
                        </div>
                    </div>

                    <!-- Breakdown Multi-Channel Prices Grid (Arsitektur PTT) -->
                    <div class="pt-2 border-t border-slate-100">
                        <p class="text-[10px] font-extrabold uppercase text-slate-400 tracking-wider mb-1.5">Harga per Saluran Penjualan (Multi-Channel):</p>
                        <div class="grid grid-cols-2 sm:grid-cols-5 gap-1.5 text-xs">
                            <!-- Offline (Kasir) -->
                            <div class="bg-amber-50/80 border border-amber-200 p-2 rounded-xl">
                                <p class="text-[10px] font-bold text-amber-800">Kasir (Offline)</p>
                                <p class="font-extrabold text-slate-900 mt-0.5">Rp {{ number_format($menu->priceForChannel('OFFLINE'), 0, ',', '.') }}</p>
                            </div>
                            <!-- Web Pelanggan -->
                            <div class="bg-blue-50/80 border border-blue-200 p-2 rounded-xl">
                                <p class="text-[10px] font-bold text-blue-800">Web Pelanggan</p>
                                <p class="font-extrabold text-slate-900 mt-0.5">Rp {{ number_format($menu->priceForChannel('CUSTOMER_WEB'), 0, ',', '.') }}</p>
                            </div>
                            <!-- GoFood -->
                            <div class="bg-emerald-50/80 border border-emerald-200 p-2 rounded-xl">
                                <p class="text-[10px] font-bold text-emerald-800">GoFood</p>
                                <p class="font-extrabold text-slate-900 mt-0.5">Rp {{ number_format($menu->priceForChannel('GOFOOD'), 0, ',', '.') }}</p>
                            </div>
                            <!-- GrabFood -->
                            <div class="bg-green-50/80 border border-green-200 p-2 rounded-xl">
                                <p class="text-[10px] font-bold text-green-800">GrabFood</p>
                                <p class="font-extrabold text-slate-900 mt-0.5">Rp {{ number_format($menu->priceForChannel('GRABFOOD'), 0, ',', '.') }}</p>
                            </div>
                            <!-- ShopeeFood -->
                            <div class="bg-orange-50/80 border border-orange-200 p-2 rounded-xl col-span-2 sm:col-span-1">
                                <p class="text-[10px] font-bold text-orange-800">ShopeeFood</p>
                                <p class="font-extrabold text-slate-900 mt-0.5">Rp {{ number_format($menu->priceForChannel('SHOPEEFOOD'), 0, ',', '.') }}</p>
                            </div>
                        </div>
                    </div>

                </div>
            @endforeach
        @endif
    </div>

</div>
@endsection
