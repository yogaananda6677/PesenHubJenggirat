<?php

namespace App\Services;

use App\Models\Order;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class FirestoreSyncService
{
    protected string $projectId;
    protected string $apiKey;

    public function __construct()
    {
        $this->projectId = env('FIREBASE_PROJECT_ID', 'pml-yoga');
        $this->apiKey    = env('FIREBASE_API_KEY', 'AIzaSyDeZnmvCwK45LMUM1vKoMeSrrzxqa59iYc');
    }

    /**
     * Sync order to Firestore collection 'orders'.
     */
    public function syncOrder(Order $order): bool
    {
        try {
            $endpoint = "https://firestore.googleapis.com/v1/projects/{$this->projectId}/databases/(default)/documents/orders/{$order->order_number}?key={$this->apiKey}";

            // Format items text
            $detailItems = $order->items->map(function ($item) {
                $toppings = !empty($item->toppings_json) ? ' (' . implode(', ', $item->toppings_json) . ')' : '';
                return "{$item->quantity}x {$item->menu_name}{$toppings} - Rp " . number_format($item->subtotal, 0, ',', '.');
            })->implode("\n");

            // Format fields in Firestore REST schema
            $fields = [
                'orderNumber'   => ['stringValue' => $order->order_number],
                'customerName'  => ['stringValue' => $order->customer_name],
                'customerPhone' => ['stringValue' => $order->customer_phone],
                'menuItem'      => ['stringValue' => $detailItems],
                'detailItem'    => ['stringValue' => $detailItems],
                'totalItems'    => ['integerValue' => (string) $order->total_items],
                'total'         => ['integerValue' => (string) $order->total_price],
                'paymentMethod' => ['stringValue' => $order->payment_method],
                'paymentStatus' => ['stringValue' => $order->payment_status],
                'status'        => ['stringValue' => $order->status],
                'source'        => ['stringValue' => 'CUSTOMER_WEB'],
                'branchName'    => ['stringValue' => $order->branch_name ?? 'Jenggirat Kediri'],
                'branchId'      => ['stringValue' => 'kediri'],
                'notes'         => ['stringValue' => ($order->notes ?? '') . " (Siap: {$order->pickup_time})"],
                'createdAt'     => ['timestampValue' => now()->toIso8601String()],
            ];

            if (!empty($order->barcode_url)) {
                $fields['barcodeUrl'] = ['stringValue' => $order->barcode_url];
            }

            $response = Http::patch($endpoint, [
                'fields' => $fields,
            ]);

            return $response->successful();
        } catch (\Throwable $e) {
            Log::warning('Firestore sync warning: ' . $e->getMessage());
            return false;
        }
    }
}
