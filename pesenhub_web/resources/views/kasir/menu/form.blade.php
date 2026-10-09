@extends('layouts.app')

@section('title', ($isEdit ? 'Edit Menu' : 'Tambah Menu Baru') . ' - PesenHub Jenggirat')

@section('content')
<div class="px-4 py-4 space-y-4">

    <!-- Header Form -->
    <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div>
            <a href="{{ route('kasir.menu.index') }}" class="text-xs text-primary font-bold hover:underline mb-1 inline-block">
                ← Kembali ke Daftar Menu
            </a>
            <h1 class="text-base font-extrabold text-slate-900 tracking-tight">
                {{ $isEdit ? 'Edit Menu: ' . $menu->name : 'Tambah Menu Baru & Harga Multi-Channel' }}
            </h1>
            <p class="text-xs text-slate-500">Lengkapi informasi menu dan atur tarif harga per saluran penjualan</p>
        </div>
    </div>

    <!-- Main Form -->
    <form action="{{ $isEdit ? route('kasir.menu.update', $menu->id) : route('kasir.menu.store') }}" 
          method="POST" 
          enctype="multipart/form-data" 
          class="space-y-4">
        @csrf
        @if($isEdit)
            @method('PUT')
        @endif

        <!-- Card 1: Informasi Dasar Menu -->
        <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <h2 class="text-xs font-extrabold text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
                Informasi Dasar Menu
            </h2>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                    <label class="block text-xs font-bold text-slate-700 mb-1">Nama Menu *</label>
                    <input type="text" name="name" value="{{ old('name', $menu->name) }}" required placeholder="Contoh: Martabak Mozarella Spesial"
                           class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
                </div>

                <div>
                    <label class="block text-xs font-bold text-slate-700 mb-1">Kode SKU Menu *</label>
                    <input type="text" name="sku" value="{{ old('sku', $menu->sku) }}" required placeholder="Contoh: MRT-008"
                           class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-mono uppercase focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
                </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                    <label class="block text-xs font-bold text-slate-700 mb-1">Kategori Menu *</label>
                    <select name="category" required class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-primary">
                        <option value="Martabak Telur" {{ old('category', $menu->category) === 'Martabak Telur' ? 'selected' : '' }}>Martabak Telur</option>
                        <option value="Terang Bulan" {{ old('category', $menu->category) === 'Terang Bulan' ? 'selected' : '' }}>Terang Bulan</option>
                        <option value="Minuman" {{ old('category', $menu->category) === 'Minuman' ? 'selected' : '' }}>Minuman</option>
                    </select>
                </div>

                <div>
                    <label class="block text-xs font-bold text-slate-700 mb-1">Harga Pokok Penjualan (HPP)</label>
                    <input type="number" name="hpp_amount" value="{{ old('hpp_amount', $menu->hpp_amount) }}" placeholder="Modal bahan baku (Rp)"
                           class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
                </div>
            </div>

            <div>
                <label class="block text-xs font-bold text-slate-700 mb-1">Deskripsi Menu</label>
                <textarea name="description" rows="2" placeholder="Deskripsi rasa, porsi, dan kelezatan menu..."
                          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">{{ old('description', $menu->description) }}</textarea>
            </div>

            <!-- Upload Foto & Preview -->
            <div>
                <label class="block text-xs font-bold text-slate-700 mb-1">Foto Menu (Upload ke Supabase Storage)</label>
                <input type="file" name="image" accept="image/*"
                       class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-bold file:bg-primary file:text-white hover:file:bg-primary-dark cursor-pointer">
                @if($menu->image_url && !str_contains($menu->image_url, 'default_food_icon'))
                    <div class="mt-2 flex items-center gap-2">
                        <img src="{{ $menu->image_url }}" alt="Preview" class="w-12 h-12 rounded-lg object-cover border border-slate-200">
                        <span class="text-[11px] text-slate-500">Foto saat ini tersimpan di Supabase Storage / Lokal</span>
                    </div>
                @endif
            </div>

            <!-- Toggle Ketersediaan Stok -->
            <div class="pt-2 flex items-center gap-3">
                <input type="checkbox" name="is_available" id="checkAvailable" value="1" 
                       {{ old('is_available', $menu->is_available ?? true) ? 'checked' : '' }}
                       class="w-4 h-4 text-primary rounded border-slate-300 focus:ring-primary">
                <label for="checkAvailable" class="text-xs font-bold text-slate-800 cursor-pointer">
                    Menu Tersedia untuk Dijual (Aktif di Kasir &amp; Web)
                </label>
            </div>
        </div>

        <!-- Card 2: Pengaturan Harga Multi-Channel (Arsitektur PTT) -->
        <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 border-b border-slate-100 pb-2">
                <div>
                    <h2 class="text-xs font-extrabold text-slate-900 uppercase tracking-wider">
                        Harga Multi-Channel Penjualan
                    </h2>
                    <p class="text-[11px] text-slate-500">Tentukan harga berbeda untuk setiap saluran pesanan</p>
                </div>
                <!-- Tombol Bantuan Hitung Otomatis -->
                <button type="button" onclick="hitungMarkupOjol()" 
                        class="bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 text-[11px] font-bold px-3 py-1.5 rounded-lg transition">
                    ⚡ Set Otomatis Ojol (+20%)
                </button>
            </div>

            <!-- Harga Dasar / Offline -->
            <div>
                <label class="block text-xs font-bold text-slate-800 mb-1">
                    1. Harga Kasir Offline (Dine-In / Takeaway) *
                </label>
                <div class="relative">
                    <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-xs text-slate-500 font-bold">Rp</span>
                    <input type="number" id="inputPriceOffline" name="base_price" required min="0" 
                           value="{{ old('base_price', $menu->priceForChannel('OFFLINE')) }}"
                           placeholder="20000"
                           class="w-full pl-9 pr-4 py-2.5 bg-amber-50/50 border border-amber-300 rounded-xl text-xs font-extrabold text-slate-900 focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
                </div>
                <input type="hidden" name="prices[OFFLINE]" id="hiddenPriceOffline" value="{{ old('base_price', $menu->priceForChannel('OFFLINE')) }}">
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                <!-- Website Pelanggan -->
                <div>
                    <label class="block text-xs font-bold text-slate-700 mb-1">
                        2. Harga Website Pelanggan (Self-Order)
                    </label>
                    <div class="relative">
                        <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-xs text-slate-500 font-bold">Rp</span>
                        <input type="number" id="inputPriceWeb" name="prices[CUSTOMER_WEB]" min="0" 
                               value="{{ old('prices.CUSTOMER_WEB', $menu->priceForChannel('CUSTOMER_WEB')) }}"
                               placeholder="20000"
                               class="w-full pl-9 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary focus:bg-white transition">
                    </div>
                </div>

                <!-- GoFood -->
                <div>
                    <label class="block text-xs font-bold text-slate-700 mb-1">
                        3. Harga GoFood
                    </label>
                    <div class="relative">
                        <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-xs text-slate-500 font-bold">Rp</span>
                        <input type="number" id="inputPriceGofood" name="prices[GOFOOD]" min="0" 
                               value="{{ old('prices.GOFOOD', $menu->priceForChannel('GOFOOD')) }}"
                               placeholder="24000"
                               class="w-full pl-9 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary focus:bg-white transition">
                    </div>
                </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <!-- GrabFood -->
                <div>
                    <label class="block text-xs font-bold text-slate-700 mb-1">
                        4. Harga GrabFood
                    </label>
                    <div class="relative">
                        <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-xs text-slate-500 font-bold">Rp</span>
                        <input type="number" id="inputPriceGrabfood" name="prices[GRABFOOD]" min="0" 
                               value="{{ old('prices.GRABFOOD', $menu->priceForChannel('GRABFOOD')) }}"
                               placeholder="24000"
                               class="w-full pl-9 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary focus:bg-white transition">
                    </div>
                </div>

                <!-- ShopeeFood -->
                <div>
                    <label class="block text-xs font-bold text-slate-700 mb-1">
                        5. Harga ShopeeFood
                    </label>
                    <div class="relative">
                        <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-xs text-slate-500 font-bold">Rp</span>
                        <input type="number" id="inputPriceShopeefood" name="prices[SHOPEEFOOD]" min="0" 
                               value="{{ old('prices.SHOPEEFOOD', $menu->priceForChannel('SHOPEEFOOD')) }}"
                               placeholder="23000"
                               class="w-full pl-9 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary focus:bg-white transition">
                    </div>
                </div>
            </div>
        </div>

        <!-- Tombol Submit -->
        <div class="flex items-center justify-end gap-3 pt-2">
            <a href="{{ route('kasir.menu.index') }}" class="px-5 py-3 rounded-xl border border-slate-200 text-xs font-bold text-slate-600 hover:bg-slate-50 transition">
                Batal
            </a>
            <button type="submit" 
                    class="bg-primary hover:bg-primary-dark text-white font-extrabold text-xs px-7 py-3 rounded-xl transition shadow flex items-center gap-2">
                <span>{{ $isEdit ? 'Simpan Perubahan' : 'Tambahkan Menu' }}</span>
                <span>✓</span>
            </button>
        </div>
    </form>

</div>
@endsection

@push('scripts')
<script>
    const inputOffline = document.getElementById('inputPriceOffline');
    const hiddenOffline = document.getElementById('hiddenPriceOffline');
    const inputWeb = document.getElementById('inputPriceWeb');
    const inputGofood = document.getElementById('inputPriceGofood');
    const inputGrabfood = document.getElementById('inputPriceGrabfood');
    const inputShopeefood = document.getElementById('inputPriceShopeefood');

    inputOffline.addEventListener('input', () => {
        hiddenOffline.value = inputOffline.value;
        // Defaultkan harga web sama dengan harga offline jika kosong
        if (!inputWeb.value || inputWeb.value === '0') {
            inputWeb.value = inputOffline.value;
        }
    });

    function hitungMarkupOjol() {
        const base = parseInt(inputOffline.value) || 0;
        if (base <= 0) {
            alert('Masukkan harga dasar kasir offline terlebih dahulu.');
            return;
        }

        // Markup 20% untuk platform ojol, dibulatkan ke kelipatan 1000
        const ojolPrice = Math.round((base * 1.20) / 1000) * 1000;
        const shopeePrice = Math.round((base * 1.15) / 1000) * 1000;

        if (!inputWeb.value) inputWeb.value = base;
        inputGofood.value = ojolPrice;
        inputGrabfood.value = ojolPrice;
        inputShopeefood.value = shopeePrice;
    }
</script>
@endpush
