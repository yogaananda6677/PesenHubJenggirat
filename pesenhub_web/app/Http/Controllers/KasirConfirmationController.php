<?php

namespace App\Http\Controllers;

use App\Models\Order;
use App\Services\BarcodeGeneratorService;
use App\Services\FirestoreSyncService;
use App\Services\SupabaseStorageService;
use Illuminate\Http\Request;

class KasirConfirmationController extends Controller
{
    protected BarcodeGeneratorService $barcodeService;
    protected SupabaseStorageService $supabaseService;
    protected FirestoreSyncService $firestoreSync;

    public function __construct(
        BarcodeGeneratorService $barcodeService,
        SupabaseStorageService $supabaseService,
        FirestoreSyncService $firestoreSync
    ) {
        $this->barcodeService  = $barcodeService;
        $this->supabaseService = $supabaseService;
        $this->firestoreSync   = $firestoreSync;
    }

    /**
     * Dashboard Antrean Web untuk Kasir / Admin Konfirmasi.
     */
    public function index()
    {
        $ordersPending = Order::with('items')
            ->where('status', 'PENDING')
            ->orderBy('created_at', 'asc')
            ->get();

        $ordersActive = Order::with('items')
            ->whereIn('status', ['CONFIRMED', 'PREPARING', 'READY'])
            ->orderBy('updated_at', 'desc')
            ->get();

        $ordersCompleted = Order::with('items')
            ->where('status', 'COMPLETED')
            ->orderBy('updated_at', 'desc')
            ->limit(10)
            ->get();

        return view('kasir.antrian', compact('ordersPending', 'ordersActive', 'ordersCompleted'));
    }

    /**
     * Konfirmasi Pesanan oleh Kasir:
     * 1. Generate Barcode unik.
     * 2. Upload Barcode ke Supabase Storage.
     * 3. Update status pesanan -> CONFIRMED & simpan barcode_url.
     * 4. Sinkronisasi ke Cloud Firestore.
     */
    public function confirm(string $orderNumber)
    {
        $order = Order::with('items')->where('order_number', $orderNumber)->firstOrFail();

        // 1. Simpan barcode lokal
        $localResult = $this->barcodeService->saveLocally($order->order_number);

        // 2. Upload ke Supabase Storage
        $uploadResult = $this->supabaseService->uploadBarcode(
            $localResult['filename'],
            $localResult['bytes'],
            'image/png'
        );

        $barcodeUrl = $uploadResult['public_url'];

        // 3. Update Order di Database
        $order->update([
            'status'       => 'CONFIRMED',
            'barcode_code' => $order->order_number,
            'barcode_url'  => $barcodeUrl,
        ]);

        // 4. Sinkronisasi ke Cloud Firestore
        $this->firestoreSync->syncOrder($order);

        $pesan = "Pesanan #{$order->order_number} berhasil dikonfirmasi! Barcode berhasil diunggah ke {$uploadResult['provider']} dan dikirim ke pelanggan.";

        return back()->with('success', $pesan);
    }

    /**
     * Tandai Pesanan Selesai / Lunas (saat diambil & discan).
     */
    public function complete(string $orderNumber)
    {
        $order = Order::with('items')->where('order_number', $orderNumber)->firstOrFail();

        $order->update([
            'status'         => 'COMPLETED',
            'payment_status' => 'PAID',
        ]);

        // Sinkronisasi ke Cloud Firestore
        $this->firestoreSync->syncOrder($order);

        return back()->with('success', "Pesanan #{$order->order_number} telah diselesaikan dan ditandai Lunas!");
    }
}
