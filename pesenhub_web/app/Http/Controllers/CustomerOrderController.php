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
                'image_url' => $this->resolveImageUrl($m['imageUrl'] ?? null),
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

        $lastOrderNumber = session('last_order_number');
        $customerOrders = session('customer_orders', []);

        return view('customer.menu', compact('katalogMenu', 'daftarTopping', 'lastOrderNumber', 'customerOrders'));
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

        // Simpan riwayat pesanan dalam sesi pelanggan aktif 5 jam
        $customerOrders = session('customer_orders', []);
        if (!in_array($orderNumber, $customerOrders)) {
            $customerOrders[] = $orderNumber;
        }

        session([
            'customer_name'               => $validated['customer_name'],
            'customer_phone'              => $validated['customer_phone'],
            'customer_session_expires_at' => now()->addHours(5)->timestamp,
            'last_order_number'           => $orderNumber,
            'customer_orders'             => $customerOrders,
        ]);

        // Simpan langsung ke Cloud Firestore
        $this->firestore->createOrder($orderData);

        return redirect()->route('order.track', $orderNumber)
            ->with('success', 'Pesanan Anda berhasil dikirim! Menunggu konfirmasi dari outlet Jenggirat.');
    }

    /**
     * Tombol "Cek Pesanan" pintar: redirect ke pesanan terakhir atau pesanan terbaru di sesi.
     */
    public function checkOrdersRedirect()
    {
        $lastOrder = session('last_order_number');
        if ($lastOrder) {
            return redirect()->route('order.track', $lastOrder);
        }

        $customerOrders = session('customer_orders', []);
        if (!empty($customerOrders)) {
            return redirect()->route('order.track', end($customerOrders));
        }

        return redirect()->route('order.menu')
            ->with('error', 'Belum ada pesanan yang tercatat dalam sesi ini. Silakan pesan menu terlebih dahulu.');
    }

    /**
     * Cari pesanan manual berdasarkan nomor pesanan.
     */
    public function findOrder(Request $request)
    {
        $validated = $request->validate([
            'order_number' => 'required|string|max:50',
        ]);

        $orderNumber = trim($validated['order_number']);
        return redirect()->route('order.track', $orderNumber);
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

    private function resolveImageUrl(?string $rawImg): ?string
    {
        if (empty($rawImg) || $rawImg === 'default_food_icon') {
            return null;
        }

        if (str_starts_with($rawImg, 'http')) {
            return $rawImg;
        }

        if (str_starts_with($rawImg, 'supabase://')) {
            // Contoh format: supabase://storage.pesenhub.jenggirat/menu-images/xxx.jpg
            $path = preg_replace('#^supabase://[^/]+/#', '', $rawImg);
            return "https://ckgymiinffzfyfbiiyob.supabase.co/storage/v1/object/public/Storage/{$path}";
        }

        return null;
    }
}
