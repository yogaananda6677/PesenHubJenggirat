<?php

namespace App\Http\Controllers;

use App\DTO\OrderDto;
use App\Services\FirestoreService;
use Illuminate\Http\Request;

class CustomerOrderController extends Controller
{
    protected FirestoreService $firestore;

    public function __construct(FirestoreService $firestore)
    {
        $this->firestore = $firestore;
    }

    /**
     * Tampilan Menu Pelanggan (Guest Self-Order ala Gacoan).
     * Mengambil langsung dari Cloud Firestore collection 'menus'.
     */
    public function index()
    {
        $rawMenus = $this->firestore->getMenus();
        $katalogMenu = [];

        foreach ($rawMenus as $m) {
            // Saring menu yang tidak tersedia (available == false)
            if (isset($m['available']) && $m['available'] === false) {
                continue;
            }

            $cat = $m['category'] ?? 'Martabak Telur';
            if (!isset($katalogMenu[$cat])) {
                $katalogMenu[$cat] = [];
            }

            // Ambil harga channel CUSTOMER_WEB
            $webPrice = (int) ($m['channelPrices']['CUSTOMER_WEB'] ?? $m['price'] ?? 0);

            $katalogMenu[$cat][] = [
                'id'        => (string) ($m['id'] ?? $m['sku'] ?? ''),
                'nama'      => (string) ($m['name'] ?? ''),
                'harga'     => $webPrice,
                'kategori'  => $cat,
                'deskripsi' => (string) ($m['description'] ?? ''),
                'sku'       => (string) ($m['sku'] ?? $m['id'] ?? ''),
                'image_url' => $m['imageUrl'] ?? null,
            ];
        }

        $daftarTopping = [
            ['nama' => 'Keju Mozzarella', 'harga' => 15000],
            ['nama' => 'Daging Sapi', 'harga' => 7000],
            ['nama' => 'Daging Ayam', 'harga' => 5000],
            ['nama' => 'Jamur Tiram', 'harga' => 5000],
            ['nama' => 'Sosis Sapi', 'harga' => 5000],
            ['nama' => 'Coklat Meses', 'harga' => 4000],
            ['nama' => 'Sambal Uleg Super Pedas', 'harga' => 4000],
        ];

        return view('customer.menu', compact('katalogMenu', 'daftarTopping'));
    }

    /**
     * Submit Pesanan Pelanggan (Guest).
     * Disimpan langsung ke Cloud Firestore collection 'orders'.
     */
    public function store(Request $request)
    {
        $validated = $request->validate([
            'customer_name'  => 'required|string|max:100',
            'customer_phone' => 'required|string|max:20',
            'payment_method' => 'required|string',
            'pickup_time'    => 'nullable|string',
            'notes'          => 'nullable|string|max:500',
            'items_json'     => 'required|string',
        ]);

        $items = json_decode($validated['items_json'], true);
        if (empty($items) || !is_array($items)) {
            return back()->with('error', 'Keranjang pesanan tidak boleh kosong!');
        }

        $orderNumber = 'ORD-WEB-' . date('Ymd-His') . '-' . rand(100, 999);
        $totalPrice = 0;
        $totalItems = 0;
        $detailList = [];

        foreach ($items as $item) {
            $qty = max(1, (int) ($item['qty'] ?? 1));
            $subtotal = (int) ($item['subtotal'] ?? 0);
            $totalPrice += $subtotal;
            $totalItems += $qty;

            $toppings = !empty($item['toppings']) ? ' (' . implode(', ', $item['toppings']) . ')' : '';
            $detailList[] = "{$qty}x {$item['nama']}{$toppings} - Rp " . number_format($subtotal, 0, ',', '.');
        }

        $detailText = implode("\n", $detailList);

        $orderData = [
            'orderNumber'   => $orderNumber,
            'customerName'  => $validated['customer_name'],
            'customerPhone' => $validated['customer_phone'],
            'paymentMethod' => $validated['payment_method'],
            'paymentStatus' => ($validated['payment_method'] === 'QRIS Langsung') ? 'PAID' : 'UNPAID',
            'pickupTime'    => $validated['pickup_time'] ?: 'Langsung (15-20 mnt)',
            'notes'         => $validated['notes'] ?? '',
            'total'         => $totalPrice,
            'totalPrice'    => $totalPrice,
            'totalItems'    => $totalItems,
            'status'        => 'PENDING',
            'source'        => 'CUSTOMER_WEB',
            'branchName'    => 'Jenggirat Kediri',
            'branchId'      => 'kediri',
            'menuItem'      => $detailText,
            'detailItem'    => $detailText,
            'items'         => $items,
        ];

        // Simpan langsung ke Cloud Firestore
        $this->firestore->createOrder($orderData);

        return redirect()->route('order.track', $orderNumber)
            ->with('success', 'Pesanan Anda berhasil dikirim! Menunggu konfirmasi dari kasir.');
    }

    /**
     * Halaman Live Tracking Status Pesanan & Barcode.
     */
    public function track(string $orderNumber)
    {
        $rawOrder = $this->firestore->getOrder($orderNumber);
        if (!$rawOrder) {
            abort(404, 'Pesanan tidak ditemukan di sistem.');
        }

        $order = new OrderDto($rawOrder);
        return view('customer.status', compact('order'));
    }

    /**
     * API Status Polling untuk Realtime Update di Browser Pelanggan.
     */
    public function checkStatus(string $orderNumber)
    {
        $order = $this->firestore->getOrder($orderNumber);

        if (!$order) {
            return response()->json(['error' => 'Pesanan tidak ditemukan'], 404);
        }

        return response()->json([
            'order_number'   => $order['orderNumber'] ?? $order['id'],
            'status'         => $order['status'] ?? 'PENDING',
            'payment_status' => $order['paymentStatus'] ?? 'UNPAID',
            'barcode_url'    => $order['barcodeUrl'] ?? null,
            'total_price'    => (int) ($order['total'] ?? $order['totalPrice'] ?? 0),
            'updated_at'     => $order['updatedAt'] ?? now()->toIso8601String(),
        ]);
    }
}
