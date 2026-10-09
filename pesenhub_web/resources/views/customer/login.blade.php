@extends('layouts.app')

@section('title', 'Masuk Pemesanan Pelanggan - PesenHub Jenggirat')

@section('content')
<div class="px-4 py-8 max-w-md mx-auto">

    <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 space-y-6">
        
        <!-- Header Ilustrasi & Sambutan -->
        <div class="text-center space-y-2">
            <div class="w-16 h-16 rounded-2xl bg-amber-100 border border-amber-200 text-primary font-black text-2xl flex items-center justify-center mx-auto shadow-sm">
                🥞
            </div>
            <h1 class="text-lg font-extrabold text-slate-900 tracking-tight">Masuk Pemesanan Pelanggan</h1>
            <p class="text-xs text-slate-500 leading-relaxed">
                Pesan mandiri Martabak &amp; Terang Bulan Jenggirat Kediri. Cukup masukkan nama dan nomor WhatsApp Anda (tanpa perlu kata sandi).
            </p>
            <div class="inline-flex items-center gap-1.5 bg-amber-50 border border-amber-200 text-amber-800 text-[11px] font-bold px-3 py-1 rounded-full mt-1">
                <span>⏱️ Sesi pemesanan aktif selama 5 jam</span>
            </div>
        </div>

        @if($errors->any())
            <div class="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs space-y-1">
                @foreach($errors->all() as $error)
                    <p class="flex items-center gap-1.5 font-medium">
                        <span>⚠️</span> <span>{{ $error }}</span>
                    </p>
                @endforeach
            </div>
        @endif

        <!-- Form Masuk Pelanggan -->
        <form action="{{ route('customer.login.submit') }}" method="POST" class="space-y-4">
            @csrf
            <input type="hidden" name="redirect_to" value="{{ request('redirect_to', route('order.menu')) }}">

            <div>
                <label for="inputNama" class="block text-xs font-bold text-slate-700 mb-1">
                    Nama Pemesan *
                </label>
                <div class="relative">
                    <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400">
                        👤
                    </span>
                    <input type="text" 
                           id="inputNama" 
                           name="customer_name" 
                           value="{{ old('customer_name', session('customer_name')) }}" 
                           required 
                           placeholder="Contoh: Rizqi Yoga" 
                           class="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
                </div>
                <p class="text-[10px] text-slate-400 mt-1">Nama ini akan tercantum di antrean dapur &amp; struk barcode</p>
            </div>

            <div>
                <label for="inputPhone" class="block text-xs font-bold text-slate-700 mb-1">
                    Nomor WhatsApp / HP *
                </label>
                <div class="relative">
                    <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400">
                        📱
                    </span>
                    <input type="tel" 
                           id="inputPhone" 
                           name="customer_phone" 
                           value="{{ old('customer_phone', session('customer_phone')) }}" 
                           required 
                           placeholder="Contoh: 081234567890" 
                           class="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition">
                </div>
                <p class="text-[10px] text-slate-400 mt-1">Untuk verifikasi pengambilan pesanan di outlet</p>
            </div>

            <button type="submit" 
                    class="w-full bg-primary hover:bg-primary-dark text-white font-extrabold text-xs py-3 rounded-xl transition shadow flex items-center justify-center gap-2">
                <span>Mulai Memesan Sekarang</span>
                <span>→</span>
            </button>
        </form>

        <div class="text-center pt-2 border-t border-slate-100">
            <a href="{{ route('order.menu') }}" class="text-xs text-slate-500 hover:text-slate-800 font-medium">
                Lihat Katalog Menu Terlebih Dahulu
            </a>
        </div>

    </div>

</div>
@endsection
