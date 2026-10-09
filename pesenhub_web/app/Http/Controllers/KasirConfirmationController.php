<?php

namespace App\Http\Controllers;

use App\DTO\OrderDto;
use App\Services\BarcodeGeneratorService;
use App\Services\FirestoreService;
use App\Services\SupabaseStorageService;
use Illuminate\Http\Request;

class KasirConfirmationController extends Controller
{
    protected BarcodeGeneratorService $barcodeService;
    protected SupabaseStorageService $supabaseService;
    protected FirestoreService $firestore;

    public function __construct(
        BarcodeGeneratorService $barcodeService,
        SupabaseStorageService $supabaseService,
        FirestoreService $firestore
    ) {
        $this->barcodeService  = $barcodeService;
        $this->supabaseService = $supabaseService;
        $this->firestore       = $firestore;
    }

    /**
     * Dashboard Antrean Web untuk Kasir / Admin Konfirmasi.
     * Mengambil seluruh data pesanan langsung dari Cloud Firestore.
     */
    public function index()
    {
        $rawOrders = $this->firestore->getOrders();

        $allOrders = collect($rawOrders)->map(fn($o) => new OrderDto($o));

        $ordersPending = $allOrders->filter(fn($o) => $o->status === 'PENDING')->values();
        $ordersActive = $allOrders->filter(fn($o) => in_array($o->status, ['CONFIRMED', 'PREPARING', 'READY']))->values();
        $ordersCompleted = $allOrders->filter(fn($o) => $o->status === 'COMPLETED')->take(10)->values();

        return view('kasir.antrian', compact('ordersPending', 'ordersActive', 'ordersCompleted'));
    }

    /**
     * Konfirmasi Pesanan oleh Kasir:
     * 1. Generate Barcode unik.
     * 2. Upload Barcode ke Supabase Storage.
     * 3. Update status pesanan di Cloud Firestore -> CONFIRMED & simpan barcode_url.
     */
    public function confirm(string $orderNumber)
    {
        $rawOrder = $this->firestore->getOrder($orderNumber);
        if (!$rawOrder) {
            return back()->with('error', "Pesanan #{$orderNumber} tidak ditemukan di Cloud Firestore.");
        }

        // 1. Simpan barcode lokal
        $localResult = $this->barcodeService->saveLocally($orderNumber);

        // 2. Upload ke Supabase Storage
        $uploadResult = $this->supabaseService->uploadBarcode(
            $localResult['filename'],
            $localResult['bytes'],
            'image/png'
        );

        $barcodeUrl = $uploadResult['public_url'];

        // 3. Update Dokumen Pesanan di Cloud Firestore
        $this->firestore->updateOrder($orderNumber, [
            'status'       => 'CONFIRMED',
            'barcodeCode'  => $orderNumber,
            'barcodeUrl'   => $barcodeUrl,
        ]);

        $pesan = "Pesanan #{$orderNumber} berhasil dikonfirmasi di Cloud Firestore! Barcode tersimpan di {$uploadResult['provider']} dan otomatis tampil di layar pelanggan.";

        return back()->with('success', $pesan);
    }

    /**
     * Tandai Pesanan Selesai / Lunas di Cloud Firestore (saat diambil & discan).
     */
    public function complete(string $orderNumber)
    {
        $rawOrder = $this->firestore->getOrder($orderNumber);
        if (!$rawOrder) {
            return back()->with('error', "Pesanan #{$orderNumber} tidak ditemukan di Cloud Firestore.");
        }

        $this->firestore->updateOrder($orderNumber, [
            'status'         => 'COMPLETED',
            'paymentStatus'  => 'PAID',
        ]);

        return back()->with('success', "Pesanan #{$orderNumber} telah ditandai Selesai & Lunas di Cloud Firestore!");
    }
}
