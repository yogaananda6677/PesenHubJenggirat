<?php

namespace App\DTO;

use Carbon\Carbon;

class OrderDto
{
    public string $order_number;
    public string $customer_name;
    public string $customer_phone;
    public string $payment_method;
    public string $payment_status;
    public string $status;
    public string $pickup_time;
    public string $notes;
    public int $total_price;
    public int $total_items;
    public ?string $barcode_url;
    public ?string $barcode_code;
    public Carbon $created_at;
    public Carbon $updated_at;
    public array $items;

    public function __construct(array $data)
    {
        $this->order_number   = (string) ($data['orderNumber'] ?? $data['id'] ?? '');
        $this->customer_name  = (string) ($data['customerName'] ?? '');
        $this->customer_phone = (string) ($data['customerPhone'] ?? '');
        $this->payment_method = (string) ($data['paymentMethod'] ?? 'Tunai');
        $this->payment_status = (string) ($data['paymentStatus'] ?? 'UNPAID');
        $this->status         = (string) ($data['status'] ?? 'PENDING');
        $this->pickup_time    = (string) ($data['pickupTime'] ?? 'Langsung (15-20 mnt)');
        $this->notes          = (string) ($data['notes'] ?? '');
        $this->total_price    = (int) ($data['total'] ?? $data['totalPrice'] ?? 0);
        $this->total_items    = (int) ($data['totalItems'] ?? 0);
        $this->barcode_url    = $data['barcodeUrl'] ?? null;
        $this->barcode_code   = $data['barcodeCode'] ?? $this->order_number;

        $createdStr = $data['createdAt'] ?? $data['createTime'] ?? now()->toIso8601String();
        $this->created_at = Carbon::parse($createdStr);

        $updatedStr = $data['updatedAt'] ?? $data['updateTime'] ?? $createdStr;
        $this->updated_at = Carbon::parse($updatedStr);

        $rawItems = $data['items'] ?? [];
        $parsedItems = [];
        if (!empty($rawItems) && is_array($rawItems)) {
            foreach ($rawItems as $it) {
                $parsedItems[] = (object) [
                    'menu_name'     => $it['nama'] ?? $it['name'] ?? $it['menu_name'] ?? 'Menu',
                    'quantity'      => (int) ($it['qty'] ?? $it['quantity'] ?? 1),
                    'unit_price'    => (int) ($it['hargaSatuan'] ?? $it['unit_price'] ?? 0),
                    'subtotal'      => (int) ($it['subtotal'] ?? 0),
                    'toppings_json' => $it['toppings'] ?? $it['toppings_json'] ?? [],
                    'notes'         => $it['catatan'] ?? $it['notes'] ?? null,
                ];
            }
        } elseif (!empty($data['detailItem']) || !empty($data['menuItem'])) {
            // Fallback jika hanya ada teks summary (dari Android lama)
            $text = $data['detailItem'] ?? $data['menuItem'] ?? '';
            $lines = explode("\n", $text);
            foreach ($lines as $line) {
                if (trim($line)) {
                    $parsedItems[] = (object) [
                        'menu_name'     => trim($line),
                        'quantity'      => 1,
                        'unit_price'    => $this->total_price,
                        'subtotal'      => $this->total_price,
                        'toppings_json' => [],
                        'notes'         => null,
                    ];
                }
            }
        }
        $this->items = $parsedItems;
    }
}
