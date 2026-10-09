<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>@yield('title', 'PesenHub Jenggirat - Pemesanan Online Pick-Up')</title>
    <!-- Tailwind CSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        primary: '#FF6F00',
                        'primary-dark': '#E65100',
                        'primary-light': '#FFF3E0',
                        dark: '#1E232A',
                    }
                }
            }
        }
    </script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background-color: #F8FAFC;
        }
        /* Hide scrollbars for chips */
        .no-scrollbar::-webkit-scrollbar {
            display: none;
        }
        .no-scrollbar {
            -ms-overflow-style: none;
            scrollbar-width: none;
        }
    </style>
</head>
<body class="text-slate-800 antialiased min-h-screen flex flex-col">

    <!-- Global Top Nav -->
    <header class="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div class="max-w-2xl mx-auto px-4 h-16 flex items-center justify-between">
            <a href="{{ route('order.menu') }}" class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center font-bold text-primary text-xl border border-amber-200">
                    J
                </div>
                <div>
                    <div class="flex items-center gap-2">
                        <span class="font-extrabold text-slate-900 tracking-tight text-base">Jenggirat</span>
                        <span class="bg-emerald-50 text-emerald-700 text-[10px] font-bold px-2 py-0.5 rounded-full border border-emerald-200 flex items-center gap-1">
                            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span> Online
                        </span>
                    </div>
                    <p class="text-xs text-slate-500 font-medium">Pemesanan Pick-Up • Jenggirat Kediri</p>
                </div>
            </a>
            <div class="flex items-center gap-2">
                @php
                    $isCustomerActive = session('customer_name') && (session('customer_session_expires_at') > time());
                    $remainingHours = $isCustomerActive ? max(1, ceil((session('customer_session_expires_at') - time()) / 3600)) : 0;
                    $hasActiveOrder = session('last_order_number') || !empty(session('customer_orders'));
                    $activeOrderNumber = session('last_order_number') ?? (is_array(session('customer_orders')) && count(session('customer_orders')) > 0 ? end(session('customer_orders')) : null);
                @endphp

                <!-- Tombol Cek Pesanan Pelanggan -->
                @if($hasActiveOrder && $activeOrderNumber)
                    <a href="{{ route('order.track', $activeOrderNumber) }}" 
                       class="text-xs bg-amber-500 hover:bg-amber-600 text-white px-3 py-1.5 rounded-xl font-bold transition shadow-sm flex items-center gap-1.5"
                       title="Cek Status & Barcode Pesanan Anda">
                        <span class="w-2 h-2 rounded-full bg-white animate-pulse"></span>
                        <span>Cek Pesanan</span>
                    </a>
                @else
                    <a href="{{ route('order.check.redirect') }}" 
                       class="text-xs bg-slate-100 hover:bg-slate-200 text-slate-700 px-3 py-1.5 rounded-xl font-semibold transition flex items-center gap-1"
                       title="Cek Status Pesanan">
                        <span>📋</span>
                        <span class="hidden sm:inline">Cek Pesanan</span>
                    </a>
                @endif

                @if($isCustomerActive)
                    <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-2.5 py-1.5">
                        <div class="text-left">
                            <p class="text-[11px] font-bold text-slate-800 leading-tight truncate max-w-[120px] sm:max-w-[160px]">
                                {{ session('customer_name') }}
                            </p>
                            <p class="text-[9px] text-amber-600 font-semibold leading-tight">
                                Sesi {{ $remainingHours }} jam
                            </p>
                        </div>
                        <form action="{{ route('customer.logout') }}" method="POST" class="inline m-0">
                            @csrf
                            <button type="submit" title="Ganti Identitas / Keluar" class="text-[10px] text-slate-400 hover:text-rose-600 font-bold p-1 rounded hover:bg-slate-100">
                                ✕
                            </button>
                        </form>
                    </div>
                @else
                    <a href="{{ route('customer.login') }}" class="text-xs bg-primary hover:bg-primary-dark text-white px-3.5 py-1.5 rounded-xl font-bold transition shadow-sm flex items-center gap-1.5">
                        <span>👤</span> <span>Masuk</span>
                    </a>
                @endif
            </div>
        </div>
    </header>

    <!-- Main Content Container -->
    <main class="flex-1 max-w-2xl w-full mx-auto pb-24">
        @if(session('success'))
            <div class="m-4 p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-sm font-medium flex items-center gap-3 shadow-sm">
                <svg class="w-5 h-5 text-emerald-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                </svg>
                <span>{{ session('success') }}</span>
            </div>
        @endif

        @if(session('error'))
            <div class="m-4 p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm font-medium flex items-center gap-3 shadow-sm">
                <svg class="w-5 h-5 text-rose-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.586 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
                </svg>
                <span>{{ session('error') }}</span>
            </div>
        @endif

        @yield('content')
    </main>

    @stack('scripts')
</body>
</html>
