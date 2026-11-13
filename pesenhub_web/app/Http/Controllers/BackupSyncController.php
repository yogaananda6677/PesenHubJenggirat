<?php

namespace App\Http\Controllers;

use App\Models\BackupOrder;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class BackupSyncController extends Controller
{
    /**
     * Endpoint API untuk menerima data backup transaksi dari Android (Modul 12 Threads/Coroutine).
     * Menerima baik single order map maupun batch list of orders.
     */
    public function push(Request $request): JsonResponse
    {
        try {
            $input = $request->all();
            $orders = [];

            if (isset($input['orders']) && is_array($input['orders'])) {
                $orders = $input['orders'];
            } elseif (isset($input['order_number']) || isset($input['id_pesanan'])) {
                $orders = [$input];
            } else {
                return response()->json([
                    'success' => false,
                    'message' => 'Format payload tidak valid. Harap sertakan orders (array) atau objek pesanan tunggal.',
                ], 422);
            }

            $syncedCount = 0;

            foreach ($orders as $o) {
                $orderNumber = $o['order_number'] ?? $o['id_pesanan'] ?? null;
                if (!$orderNumber) continue;

                $customerName  = $o['customer_name'] ?? $o['nama_pelanggan'] ?? 'Pelanggan';
                $customerPhone = $o['customer_phone'] ?? $o['hp_pelanggan'] ?? '-';
                $detailItem    = $o['detail_item'] ?? $o['menu_item'] ?? '-';
                $paymentMethod = $o['payment_method'] ?? $o['metode_bayar'] ?? 'Tunai';
                $totalPrice    = (int) ($o['total_price'] ?? $o['total_bayar'] ?? $o['total'] ?? 0);
                $status        = $o['status'] ?? $o['status_order'] ?? 'COMPLETED';
                $waktu         = $o['waktu_transaksi'] ?? $o['waktu_selesai'] ?? date('d/m/Y H:i');
                $syncSource    = $o['sync_source'] ?? 'ANDROID_COROUTINE';

                BackupOrder::updateOrCreate(
                    ['order_number' => $orderNumber],
                    [
                        'customer_name'   => $customerName,
                        'customer_phone'  => $customerPhone,
                        'detail_item'     => $detailItem,
                        'payment_method'  => $paymentMethod,
                        'total_price'     => $totalPrice,
                        'status'          => $status,
                        'waktu_transaksi' => $waktu,
                        'sync_source'     => $syncSource,
                        'synced_at'       => now(),
                    ]
                );

                $syncedCount++;
            }

            $totalInDb = BackupOrder::count();

            return response()->json([
                'success'              => true,
                'message'              => "Berhasil mem-backup {$syncedCount} transaksi ke MySQL Backup Server.",
                'synced_count'         => $syncedCount,
                'total_backup_records' => $totalInDb,
                'server_time'          => now()->toDateTimeString(),
            ]);
        } catch (\Throwable $e) {
            Log::error("BackupSyncController push error: " . $e->getMessage());
            return response()->json([
                'success' => false,
                'message' => 'Gagal memproses sinkronisasi backup: ' . $e->getMessage(),
            ], 500);
        }
    }

    /**
     * Endpoint API untuk mengunduh daftar data backup transaksi dari MySQL ke Android (Restore / Sync).
     */
    public function getOrders(Request $request): JsonResponse
    {
        try {
            $limit = (int) $request->query('limit', 100);
            $orders = BackupOrder::orderBy('id', 'desc')->limit($limit)->get();

            return response()->json([
                'success' => true,
                'total'   => $orders->count(),
                'data'    => $orders,
            ]);
        } catch (\Throwable $e) {
            return response()->json([
                'success' => false,
                'message' => 'Gagal mengambil data backup: ' . $e->getMessage(),
            ], 500);
        }
    }

    /**
     * Status koneksi MySQL server backup.
     */
    public function status(): JsonResponse
    {
        try {
            $connectionName = DB::getDefaultConnection();
            $dbName = DB::connection()->getDatabaseName();
            $totalRecords = BackupOrder::count();

            return response()->json([
                'success'       => true,
                'status'        => 'ONLINE',
                'connection'    => $connectionName,
                'database'      => $dbName,
                'total_records' => $totalRecords,
                'timestamp'     => now()->toIso8601String(),
            ]);
        } catch (\Throwable $e) {
            return response()->json([
                'success' => false,
                'status'  => 'OFFLINE',
                'error'   => $e->getMessage(),
            ], 500);
        }
    }
}
