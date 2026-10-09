@extends('layouts.app')

@section('title', 'Lokasi Outlet - PesenHub Jenggirat Kediri')

@push('scripts')
<!-- Leaflet OpenStreetMap CDN (Library yang sama dengan OSMdroid di Android) -->
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>

<script>
    document.addEventListener("DOMContentLoaded", function () {
        const outletLat = {{ $outlet['latitude'] }};
        const outletLng = {{ $outlet['longitude'] }};
        const outletName = "{{ $outlet['name'] }}";

        // 1. Inisialisasi Peta Leaflet (OpenStreetMap MAPNIK tiles - sama persis dengan OSMdroid Android)
        const map = L.map('mapOutlet', {
            zoomControl: true,
            scrollWheelZoom: true
        }).setView([outletLat, outletLng], 16);

        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors'
        }).addTo(map);

        // 2. Custom Marker Outlet
        const outletIcon = L.divIcon({
            html: '<div style="background-color: #FF6F00; color: white; width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; box-shadow: 0 4px 10px rgba(0,0,0,0.3); border: 3px solid white;">🥞</div>',
            className: 'custom-leaflet-marker',
            iconSize: [38, 38],
            iconAnchor: [19, 19]
        });

        const markerOutlet = L.marker([outletLat, outletLng], { icon: outletIcon }).addTo(map);
        markerOutlet.bindPopup(`
            <div style="font-family: 'Plus Jakarta Sans', sans-serif; padding: 4px; min-width: 180px;">
                <p style="font-size: 13px; font-weight: 800; color: #0F172A; margin: 0 0 2px 0;">${outletName}</p>
                <p style="font-size: 11px; color: #475569; margin: 0 0 6px 0;">Outlet Martabak & Terang Bulan Jenggirat</p>
                <span style="display: inline-block; background-color: #FEF3C7; color: #B45309; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 9999px;">
                    🕒 Buka 16.00 - 23.00 WIB
                </span>
            </div>
        `).openPopup();

        // 3. Tombol Pusatkan ke Outlet
        document.getElementById('btnCenterOutlet').addEventListener('click', function () {
            map.setView([outletLat, outletLng], 16.5, { animate: true });
        });

        // 4. Deteksi Lokasi Pengguna (GPS Geolocation) & Hitung Jarak
        let userMarker = null;
        let routeLine = null;

        document.getElementById('btnMyLoc').addEventListener('click', function () {
            const btn = this;
            const originalText = btn.innerHTML;
            btn.innerHTML = '<span>⏳ Mencari lokasi...</span>';
            btn.disabled = true;

            if (!navigator.geolocation) {
                alert('Browser Anda tidak mendukung deteksi lokasi otomatis.');
                btn.innerHTML = originalText;
                btn.disabled = false;
                return;
            }

            navigator.geolocation.getCurrentPosition(
                function (pos) {
                    const userLat = pos.coords.latitude;
                    const userLng = pos.coords.longitude;

                    if (userMarker) map.removeLayer(userMarker);
                    if (routeLine) map.removeLayer(routeLine);

                    // Marker Pengguna
                    const userIcon = L.divIcon({
                        html: '<div style="background-color: #2563EB; color: white; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; box-shadow: 0 4px 10px rgba(0,0,0,0.3); border: 3px solid white;">👤</div>',
                        className: 'user-leaflet-marker',
                        iconSize: [32, 32],
                        iconAnchor: [16, 16]
                    });

                    userMarker = L.marker([userLat, userLng], { icon: userIcon }).addTo(map);
                    userMarker.bindPopup('<strong style="font-size:12px;">Posisi Anda</strong>').openPopup();

                    // Garis rute putus-putus
                    routeLine = L.polyline([[userLat, userLng], [outletLat, outletLng]], {
                        color: '#FF6F00',
                        weight: 4,
                        dashArray: '6, 8',
                        opacity: 0.8
                    }).addTo(map);

                    // Fit bounds agar kedua titik terlihat
                    const group = new L.featureGroup([markerOutlet, userMarker]);
                    map.fitBounds(group.getBounds().pad(0.2));

                    // Hitung Jarak (Haversine Formula)
                    const jarakKm = hitungJarakKm(userLat, userLng, outletLat, outletLng);
                    const infoBox = document.getElementById('userDistanceInfo');
                    infoBox.classList.remove('hidden');
                    document.getElementById('distanceText').innerText = jarakKm.toFixed(1) + ' km';

                    btn.innerHTML = originalText;
                    btn.disabled = false;
                },
                function (err) {
                    alert('Gagal mengambil lokasi: ' + err.message + '. Pastikan izin GPS aktif di browser.');
                    btn.innerHTML = originalText;
                    btn.disabled = false;
                },
                { enableHighAccuracy: true, timeout: 10000 }
            );
        });

        function hitungJarakKm(lat1, lon1, lat2, lon2) {
            const R = 6371; // Radius bumi dalam km
            const dLat = (lat2 - lat1) * Math.PI / 180;
            const dLon = (lon2 - lon1) * Math.PI / 180;
            const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                      Math.sin(dLon / 2) * Math.sin(dLon / 2);
            const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }
    });
</script>
@endpush

@section('content')
<div class="px-4 py-4 space-y-4">

    <!-- Header & Navigasi Balik -->
    <div class="flex items-center justify-between">
        <a href="{{ route('order.menu') }}" 
           class="inline-flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-primary bg-white px-3 py-2 rounded-xl border border-slate-200 shadow-sm transition">
            <span>←</span>
            <span>Kembali ke Menu</span>
        </a>
        <span class="text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-1 rounded-full flex items-center gap-1">
            <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>Outlet Buka</span>
        </span>
    </div>

    <!-- Peta OpenStreetMap (Leaflet) Container -->
    <div class="bg-white p-3 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div class="flex items-center justify-between">
            <div>
                <h1 class="text-base font-extrabold text-slate-900 tracking-tight flex items-center gap-1.5">
                    <span>📍</span> <span>Peta Lokasi Outlet Jenggirat</span>
                </h1>
                <p class="text-xs text-slate-500">Peta realtime OpenStreetMap (OSM) • Jenggirat Kediri</p>
            </div>
            <div class="flex items-center gap-1">
                <button type="button" id="btnCenterOutlet" 
                        class="p-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition"
                        title="Pusatkan ke Outlet">
                    🎯 Outlet
                </button>
                <button type="button" id="btnMyLoc" 
                        class="px-2.5 py-2 bg-amber-50 hover:bg-amber-100 text-primary border border-amber-200 rounded-xl text-xs font-bold transition flex items-center gap-1"
                        title="Tampilkan Jarak Saya">
                    <span>🧭</span> <span>Lokasi Saya</span>
                </button>
            </div>
        </div>

        <!-- Wadah Render Peta -->
        <div class="w-full h-80 sm:h-96 rounded-xl overflow-hidden border border-slate-200 shadow-inner relative z-10" id="mapOutlet">
            <!-- Peta Leaflet di-mount disini -->
        </div>

        <!-- Info Jarak Terdeteksi (Tampil jika GPS aktif) -->
        <div id="userDistanceInfo" class="hidden bg-amber-50 border border-amber-200 rounded-xl p-3 flex items-center justify-between">
            <div class="flex items-center gap-2">
                <span class="text-xl">🛵</span>
                <div>
                    <p class="text-xs font-bold text-slate-800">Estimasi Jarak dari Lokasi Anda</p>
                    <p class="text-[11px] text-slate-500">Jarak lurus ke outlet Martabak Jenggirat</p>
                </div>
            </div>
            <span id="distanceText" class="text-base font-black text-primary bg-white px-3 py-1 rounded-lg border border-amber-200 shadow-sm">
                0 km
            </span>
        </div>
    </div>

    <!-- Detail Informasi Outlet -->
    <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <h2 class="text-sm font-extrabold text-slate-900 uppercase tracking-wider">Informasi Outlet &amp; Jam Operasional</h2>
        
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div class="bg-slate-50 p-3 rounded-xl border border-slate-100 space-y-1">
                <p class="text-slate-400 font-medium">Nama Outlet</p>
                <p class="font-bold text-slate-900 text-sm">{{ $outlet['fullName'] }}</p>
                <p class="text-slate-500 text-[11px]">{{ $outlet['address'] }}</p>
            </div>

            <div class="bg-slate-50 p-3 rounded-xl border border-slate-100 space-y-1">
                <p class="text-slate-400 font-medium">Jam Buka (Pick-Up)</p>
                <p class="font-bold text-slate-900 text-sm">{{ $outlet['hours'] }}</p>
                <p class="text-emerald-600 font-semibold text-[11px]">Setiap Hari (Senin - Minggu)</p>
            </div>
        </div>

        <!-- Tombol Aksi Tambahan -->
        <div class="pt-2 flex flex-col sm:flex-row gap-2">
            <a href="https://www.google.com/maps/dir/?api=1&destination={{ $outlet['latitude'] }},{{ $outlet['longitude'] }}" 
               target="_blank" 
               rel="noopener noreferrer"
               class="flex-1 bg-primary hover:bg-primary-dark text-white font-extrabold text-xs py-3 rounded-xl transition shadow text-center flex items-center justify-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                </svg>
                <span>Buka Petunjuk Arah (Google Maps)</span>
            </a>

            <a href="{{ route('order.menu') }}" 
               class="sm:w-auto px-6 py-3 bg-slate-900 hover:bg-slate-800 text-white font-extrabold text-xs rounded-xl transition shadow text-center flex items-center justify-center gap-1.5">
                <span>🥞</span>
                <span>Pesan Sekarang</span>
            </a>
        </div>
    </div>

</div>
@endsection
