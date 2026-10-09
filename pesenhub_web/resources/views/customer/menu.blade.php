@extends('layouts.app')

@section('title', 'Katalog Menu - PesenHub Jenggirat Kediri')

@section('content')
<div class="px-4 py-4 space-y-4">

    <!-- Search & Filter Banner -->
    <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div>
            <h1 class="text-lg font-extrabold text-slate-900 tracking-tight">Pesan Martabak &amp; Terang Bulan</h1>
            <p class="text-xs text-slate-500">Pesan mandiri ala self-order, ambil langsung di kasir Jenggirat Kediri</p>
        </div>

        <!-- Search Bar -->
        <div class="relative">
            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                </svg>
            </div>
            <input type="text" id="inputSearchMenu" placeholder="Cari menu favorit Anda..." 
                   class="w-full pl-9 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
        </div>

        <!-- Category Filter Chips -->
        <div class="flex items-center gap-2 overflow-x-auto no-scrollbar pt-1">
            <button type="button" onclick="filterKategori('Semua')" id="chip-Semua"
                    class="chip-kat active px-4 py-1.5 rounded-full text-xs font-bold whitespace-nowrap bg-slate-900 text-white transition">
                Semua
            </button>
            <button type="button" onclick="filterKategori('Martabak Telur')" id="chip-Martabak Telur"
                    class="chip-kat px-4 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap bg-slate-100 text-slate-700 hover:bg-slate-200 transition">
                Martabak Telur
            </button>
            <button type="button" onclick="filterKategori('Terang Bulan')" id="chip-Terang Bulan"
                    class="chip-kat px-4 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap bg-slate-100 text-slate-700 hover:bg-slate-200 transition">
                Terang Bulan
            </button>
            <button type="button" onclick="filterKategori('Minuman')" id="chip-Minuman"
                    class="chip-kat px-4 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap bg-slate-100 text-slate-700 hover:bg-slate-200 transition">
                Minuman
            </button>
        </div>
    </div>

    <!-- Menu Grid -->
    <div class="space-y-3" id="daftarMenuContainer">
        @foreach($katalogMenu as $kategori => $items)
            @foreach($items as $menu)
                <div class="menu-item-card bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between gap-3 hover:border-amber-300 transition"
                     data-nama="{{ strtolower($menu['nama']) }}"
                     data-kategori="{{ $menu['kategori'] }}">
                    
                    <div class="flex items-center gap-3 flex-1 min-w-0">
                        <div class="w-16 h-16 rounded-xl bg-amber-50 border border-amber-100 flex-shrink-0 flex items-center justify-center text-primary font-black text-xl">
                            🥞
                        </div>
                        <div class="min-w-0">
                            <span class="inline-block bg-slate-100 text-slate-600 text-[10px] font-bold px-2 py-0.5 rounded-md mb-1">
                                {{ $menu['kategori'] }}
                            </span>
                            <h3 class="font-bold text-slate-900 text-sm truncate">{{ $menu['nama'] }}</h3>
                            <p class="text-xs text-slate-500 truncate mt-0.5">{{ $menu['deskripsi'] }}</p>
                            <p class="text-sm font-extrabold text-primary mt-1">Rp {{ number_format($menu['harga'], 0, ',', '.') }}</p>
                        </div>
                    </div>

                    <button type="button" 
                            onclick="bukaModalPilih({{ json_encode($menu) }})"
                            class="bg-primary hover:bg-primary-dark text-white text-xs font-bold px-4 py-2 rounded-xl transition flex-shrink-0 shadow-sm">
                        Pilih
                    </button>
                </div>
            @endforeach
        @endforeach
    </div>

    <!-- Empty State -->
    <div id="emptyMenuState" class="hidden text-center py-12 bg-white rounded-2xl border border-slate-200">
        <p class="text-slate-400 text-3xl mb-2">🔍</p>
        <p class="text-sm font-bold text-slate-700">Menu tidak ditemukan</p>
        <p class="text-xs text-slate-400 mt-1">Coba gunakan kata kunci pencarian yang lain</p>
    </div>

</div>

<!-- Sticky Floating Bottom Cart Bar -->
<div id="floatingCartBar" class="hidden fixed bottom-4 left-4 right-4 max-w-2xl mx-auto z-40">
    <div class="bg-slate-900 text-white rounded-2xl p-3 px-4 shadow-2xl flex items-center justify-between border border-slate-800">
        <div class="flex items-center gap-3">
            <span id="floatingItemBadge" class="w-7 h-7 rounded-full bg-rose-500 text-white font-extrabold text-xs flex items-center justify-center">
                0
            </span>
            <div>
                <p id="floatingItemText" class="text-[11px] text-slate-400 font-medium">0 item terpilih</p>
                <p id="floatingTotalText" class="text-sm font-extrabold text-white">Rp 0</p>
            </div>
        </div>
        <button type="button" onclick="bukaDrawerCheckout()" 
                class="bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-xs px-5 py-2.5 rounded-xl transition shadow">
            Lanjut Pesan →
        </button>
    </div>
</div>

<!-- Modal 1: Kustomisasi Menu & Topping -->
<div id="modalKustomisasi" class="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 hidden flex items-end sm:items-center justify-center">
    <div class="bg-white w-full max-w-lg rounded-t-3xl sm:rounded-2xl max-h-[90vh] flex flex-col overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-150">
        <!-- Header Modal -->
        <div class="p-4 border-b border-slate-100 flex items-center justify-between">
            <div>
                <h3 id="modalMenuNama" class="font-extrabold text-base text-slate-900">Nama Menu</h3>
                <p id="modalMenuHargaDasar" class="text-xs font-bold text-primary">Rp 0</p>
            </div>
            <button type="button" onclick="tutupModalPilih()" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100">
                ✕
            </button>
        </div>

        <!-- Scrollable Toppings & Portions -->
        <div class="p-4 overflow-y-auto space-y-4 flex-1">
            <div>
                <h4 class="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2">Pilihan Extra Isian / Topping</h4>
                <div class="space-y-2">
                    @foreach($daftarTopping as $idx => $tp)
                        <div class="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                            <div>
                                <p class="text-xs font-bold text-slate-800">{{ $tp['nama'] }}</p>
                                <p class="text-[11px] text-slate-500">+Rp {{ number_format($tp['harga'], 0, ',', '.') }}</p>
                            </div>
                            <div class="flex items-center gap-2">
                                <button type="button" onclick="ubahToppingQty({{ $idx }}, -1)" class="w-7 h-7 rounded-full bg-white border border-slate-200 text-slate-700 font-bold text-sm flex items-center justify-center hover:bg-slate-100">
                                    -
                                </button>
                                <span id="topping-qty-{{ $idx }}" class="w-6 text-center text-xs font-extrabold text-slate-900">0</span>
                                <button type="button" onclick="ubahToppingQty({{ $idx }}, 1)" class="w-7 h-7 rounded-full bg-white border border-slate-200 text-slate-700 font-bold text-sm flex items-center justify-center hover:bg-slate-100">
                                    +
                                </button>
                            </div>
                        </div>
                    @endforeach
                </div>
            </div>

            <!-- Main Portion Stepper -->
            <div class="pt-2 border-t border-slate-100 flex items-center justify-between">
                <div>
                    <h4 class="text-xs font-bold text-slate-900">Jumlah Porsi</h4>
                    <p class="text-[11px] text-slate-500">Berapa porsi menu ini yang diinginkan</p>
                </div>
                <div class="flex items-center gap-2">
                    <button type="button" onclick="ubahPorsiUtama(-1)" class="w-8 h-8 rounded-full bg-slate-100 font-bold text-base flex items-center justify-center hover:bg-slate-200">
                        -
                    </button>
                    <span id="porsiUtamaText" class="w-8 text-center font-extrabold text-sm text-slate-900">1</span>
                    <button type="button" onclick="ubahPorsiUtama(1)" class="w-8 h-8 rounded-full bg-primary text-white font-bold text-base flex items-center justify-center hover:bg-primary-dark">
                        +
                    </button>
                </div>
            </div>

            <!-- Notes -->
            <div>
                <label class="block text-xs font-bold text-slate-700 mb-1">Catatan Tambahan (Opsional)</label>
                <input type="text" id="catatanItemModal" placeholder="Misal: jangan terlalu gosong, potong 8..." 
                       class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-primary">
            </div>
        </div>

        <!-- Footer Modal -->
        <div class="p-4 border-t border-slate-100 bg-slate-50 flex items-center justify-between gap-3">
            <div>
                <p class="text-[11px] text-slate-500 font-medium">Total Item Ini</p>
                <p id="modalGrandTotal" class="text-base font-extrabold text-primary">Rp 0</p>
            </div>
            <button type="button" onclick="simpanItemKeKeranjang()" 
                    class="bg-primary hover:bg-primary-dark text-white font-bold text-xs px-6 py-3 rounded-xl transition shadow">
                + Tambah ke Pesanan
            </button>
        </div>
    </div>
</div>

<!-- Modal 2: Checkout & Data Pelanggan -->
<div id="drawerCheckout" class="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 hidden flex items-end sm:items-center justify-center">
    <div class="bg-white w-full max-w-lg rounded-t-3xl sm:rounded-2xl max-h-[92vh] flex flex-col overflow-hidden shadow-2xl">
        <!-- Header Drawer -->
        <div class="p-4 border-b border-slate-100 flex items-center justify-between">
            <div>
                <h3 class="font-extrabold text-base text-slate-900">Konfirmasi Pemesanan</h3>
                <p class="text-xs text-slate-500">Periksa daftar pesanan dan lengkapi identitas Anda</p>
            </div>
            <button type="button" onclick="tutupDrawerCheckout()" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100">
                ✕
            </button>
        </div>

        <form action="{{ route('order.store') }}" method="POST" id="formCheckout" class="flex-1 flex flex-col overflow-hidden">
            @csrf
            <input type="hidden" name="items_json" id="hiddenItemsJson">

            <div class="p-4 overflow-y-auto space-y-4 flex-1">
                <!-- Rincian Keranjang -->
                <div>
                    <h4 class="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2">Item Terpilih</h4>
                    <div id="checkoutItemsList" class="space-y-2">
                        <!-- Dynamic items -->
                    </div>
                </div>

                <!-- Form Identitas Pelanggan (Ala Gacoan Tanpa Akun) -->
                <div class="space-y-3 pt-3 border-t border-slate-100">
                    <h4 class="text-xs font-bold text-slate-900 uppercase tracking-wider">Identitas Pelanggan</h4>
                    <div>
                        <label class="block text-xs font-bold text-slate-700 mb-1">Nama Lengkap *</label>
                        <input type="text" name="customer_name" required placeholder="Masukkan nama pemesan..." 
                               class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
                    </div>
                    <div>
                        <label class="block text-xs font-bold text-slate-700 mb-1">Nomor WhatsApp (Aktif) *</label>
                        <input type="tel" name="customer_phone" required placeholder="081234567890..." 
                               class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
                    </div>
                </div>

                <!-- Opsi Pembayaran -->
                <div class="space-y-2 pt-3 border-t border-slate-100">
                    <h4 class="text-xs font-bold text-slate-900 uppercase tracking-wider">Metode Pembayaran</h4>
                    <div class="grid grid-cols-2 gap-2">
                        <label class="border border-slate-200 p-3 rounded-xl flex items-center gap-2 cursor-pointer hover:bg-slate-50 transition has-[:checked]:border-primary has-[:checked]:bg-amber-50/50">
                            <input type="radio" name="payment_method" value="Bayar di Kasir (Saat Ambil)" checked class="text-primary focus:ring-primary">
                            <div>
                                <p class="text-xs font-bold text-slate-800">Bayar di Kasir</p>
                                <p class="text-[10px] text-slate-500">Bayar saat ambil pesanan</p>
                            </div>
                        </label>
                        <label class="border border-slate-200 p-3 rounded-xl flex items-center gap-2 cursor-pointer hover:bg-slate-50 transition has-[:checked]:border-primary has-[:checked]:bg-amber-50/50">
                            <input type="radio" name="payment_method" value="QRIS Langsung" class="text-primary focus:ring-primary">
                            <div>
                                <p class="text-xs font-bold text-slate-800">QRIS Langsung</p>
                                <p class="text-[10px] text-slate-500">Scan QRIS saat memesan</p>
                            </div>
                        </label>
                    </div>
                </div>

                <!-- Estimasi Jam Pengambilan -->
                <div class="space-y-1 pt-2">
                    <label class="block text-xs font-bold text-slate-700">Estimasi Pengambilan</label>
                    <select name="pickup_time" class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-primary">
                        <option value="Langsung (15-20 mnt)">Langsung Siap (15-20 menit)</option>
                        <option value="30 Menit Lagi">30 Menit Lagi</option>
                        <option value="1 Jam Lagi">1 Jam Lagi</option>
                        <option value="Nanti Malam (19:00 - 21:00)">Nanti Malam (19:00 - 21:00)</option>
                    </select>
                </div>
            </div>

            <!-- Footer Drawer Submit -->
            <div class="p-4 border-t border-slate-100 bg-slate-50 flex items-center justify-between gap-3">
                <div>
                    <p class="text-[11px] text-slate-500 font-medium">Total Pembayaran</p>
                    <p id="drawerGrandTotal" class="text-base font-extrabold text-primary">Rp 0</p>
                </div>
                <button type="submit" 
                        class="bg-primary hover:bg-primary-dark text-white font-bold text-xs px-6 py-3.5 rounded-xl transition shadow flex items-center gap-2">
                    <span>Kirim Pesanan</span>
                    <span>→</span>
                </button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
<script>
    const daftarToppingData = @json($daftarTopping);
    let keranjang = [];
    let menuSedangDipilih = null;
    let qtyTopping = {};
    let porsiUtama = 1;

    // Reset Topping Stepper
    function resetModalState() {
        porsiUtama = 1;
        document.getElementById('porsiUtamaText').innerText = '1';
        document.getElementById('catatanItemModal').value = '';
        daftarToppingData.forEach((_, idx) => {
            qtyTopping[idx] = 0;
            const el = document.getElementById(`topping-qty-${idx}`);
            if (el) el.innerText = '0';
        });
    }

    function bukaModalPilih(menu) {
        menuSedangDipilih = menu;
        resetModalState();

        document.getElementById('modalMenuNama').innerText = menu.nama;
        document.getElementById('modalMenuHargaDasar').innerText = 'Harga Dasar: Rp ' + formatRupiah(menu.harga);
        hitungModalTotal();

        document.getElementById('modalKustomisasi').classList.remove('hidden');
    }

    function tutupModalPilih() {
        document.getElementById('modalKustomisasi').classList.add('hidden');
    }

    function ubahToppingQty(idx, delta) {
        qtyTopping[idx] = Math.max(0, (qtyTopping[idx] || 0) + delta);
        document.getElementById(`topping-qty-${idx}`).innerText = qtyTopping[idx];
        hitungModalTotal();
    }

    function ubahPorsiUtama(delta) {
        porsiUtama = Math.max(1, porsiUtama + delta);
        document.getElementById('porsiUtamaText').innerText = porsiUtama;
        hitungModalTotal();
    }

    function hitungModalTotal() {
        if (!menuSedangDipilih) return;
        let extra = 0;
        daftarToppingData.forEach((tp, idx) => {
            extra += (qtyTopping[idx] || 0) * tp.harga;
        });
        const total = (menuSedangDipilih.harga + extra) * porsiUtama;
        document.getElementById('modalGrandTotal').innerText = 'Rp ' + formatRupiah(total);
    }

    function simpanItemKeKeranjang() {
        if (!menuSedangDipilih) return;

        let toppingTerpilih = [];
        let extraHarga = 0;

        daftarToppingData.forEach((tp, idx) => {
            const q = qtyTopping[idx] || 0;
            if (q > 0) {
                toppingTerpilih.push(q === 1 ? tp.nama : `${tp.nama} (${q})`);
                extraHarga += (q * tp.harga);
            }
        });

        const hargaSatuan = menuSedangDipilih.harga + extraHarga;
        const subtotal = hargaSatuan * porsiUtama;
        const catatan = document.getElementById('catatanItemModal').value.trim();

        keranjang.push({
            id: menuSedangDipilih.id,
            nama: menuSedangDipilih.nama,
            kategori: menuSedangDipilih.kategori,
            hargaDasar: menuSedangDipilih.harga,
            extraHarga: extraHarga,
            hargaSatuan: hargaSatuan,
            qty: porsiUtama,
            subtotal: subtotal,
            toppings: toppingTerpilih,
            catatan: catatan
        });

        tutupModalPilih();
        updateTampilanKeranjang();
    }

    function updateTampilanKeranjang() {
        const floatingBar = document.getElementById('floatingCartBar');
        if (keranjang.length === 0) {
            floatingBar.classList.add('hidden');
            return;
        }

        floatingBar.classList.remove('hidden');

        const totalItems = keranjang.reduce((acc, it) => acc + it.qty, 0);
        const totalHarga = keranjang.reduce((acc, it) => acc + it.subtotal, 0);

        document.getElementById('floatingItemBadge').innerText = totalItems;
        document.getElementById('floatingItemText').innerText = `${totalItems} item terpilih`;
        document.getElementById('floatingTotalText').innerText = 'Rp ' + formatRupiah(totalHarga);
    }

    function bukaDrawerCheckout() {
        const listContainer = document.getElementById('checkoutItemsList');
        listContainer.innerHTML = '';

        keranjang.forEach((it, idx) => {
            const toppingText = it.toppings.length > 0 ? `<p class="text-[10px] text-slate-500 mt-0.5">+ ${it.toppings.join(', ')}</p>` : '';
            const catatanText = it.catatan ? `<p class="text-[10px] text-amber-600 italic mt-0.5">Note: ${it.catatan}</p>` : '';

            const el = document.createElement('div');
            el.className = 'flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100';
            el.innerHTML = `
                <div class="min-w-0 flex-1 pr-2">
                    <p class="text-xs font-bold text-slate-900 truncate">${it.qty}x ${it.nama}</p>
                    ${toppingText}
                    ${catatanText}
                    <p class="text-xs font-extrabold text-primary mt-1">Rp ${formatRupiah(it.subtotal)}</p>
                </div>
                <button type="button" onclick="hapusItemKeranjang(${idx})" class="p-1.5 text-rose-500 hover:bg-rose-50 rounded-lg text-xs font-bold">
                    Hapus
                </button>
            `;
            listContainer.appendChild(el);
        });

        const totalHarga = keranjang.reduce((acc, it) => acc + it.subtotal, 0);
        document.getElementById('drawerGrandTotal').innerText = 'Rp ' + formatRupiah(totalHarga);
        document.getElementById('hiddenItemsJson').value = JSON.stringify(keranjang);

        document.getElementById('drawerCheckout').classList.remove('hidden');
    }

    function tutupDrawerCheckout() {
        document.getElementById('drawerCheckout').classList.add('hidden');
    }

    function hapusItemKeranjang(index) {
        keranjang.splice(index, 1);
        updateTampilanKeranjang();
        if (keranjang.length === 0) {
            tutupDrawerCheckout();
        } else {
            bukaDrawerCheckout();
        }
    }

    // Filter Kategori Chips
    function filterKategori(kat) {
        document.querySelectorAll('.chip-kat').forEach(btn => {
            btn.classList.remove('active', 'bg-slate-900', 'text-white');
            btn.classList.add('bg-slate-100', 'text-slate-700');
        });

        const activeBtn = document.getElementById(`chip-${kat}`);
        if (activeBtn) {
            activeBtn.classList.add('active', 'bg-slate-900', 'text-white');
            activeBtn.classList.remove('bg-slate-100', 'text-slate-700');
        }

        filterDaftarMenu();
    }

    // Search & Filter
    document.getElementById('inputSearchMenu').addEventListener('input', filterDaftarMenu);

    function filterDaftarMenu() {
        const keyword = document.getElementById('inputSearchMenu').value.toLowerCase().trim();
        const activeChip = document.querySelector('.chip-kat.active').id.replace('chip-', '');
        let visibleCount = 0;

        document.querySelectorAll('.menu-item-card').forEach(card => {
            const nama = card.getAttribute('data-nama');
            const kategori = card.getAttribute('data-kategori');

            const matchKategori = (activeChip === 'Semua' || kategori === activeChip);
            const matchSearch = keyword === '' || nama.includes(keyword);

            if (matchKategori && matchSearch) {
                card.classList.remove('hidden');
                visibleCount++;
            } else {
                card.classList.add('hidden');
            }
        });

        const emptyEl = document.getElementById('emptyMenuState');
        if (visibleCount === 0) {
            emptyEl.classList.remove('hidden');
        } else {
            emptyEl.classList.add('hidden');
        }
    }

    function formatRupiah(num) {
        return new Intl.NumberFormat('id-ID').format(num);
    }
</script>
@endpush
