<?php

namespace App\Http\Controllers;

use App\Models\Menu;
use App\Models\Order;
use App\Models\OrderItem;
use App\Services\FirestoreSyncService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class CustomerOrderController extends Controller
{
    protected FirestoreSyncService $firestoreSync;

    public function __construct(FirestoreSyncService $firestoreSync)
    {
        $this->firestoreSync = $firestoreSync;
    }

    /**
     * Tampilan Menu Pelanggan (Guest Self-Order ala Gacoan).
     */
    public function index()
    {
        $dbMenus = Menu::with('channelPrices')
            ->where('is_available', true)
            ->orderBy('sort_order')
            ->orderBy('id')
            ->get();

        $katalogMenu = [];

        if ($dbMenus->isNotEmpty()) {
            foreach ($dbMenus as $m) {
                $category = $m->category ?: 'Martabak Telur';
                if (!isset($katalogMenu[$category])) {
                    $katalogMenu[$category] = [];
                }

                $katalogMenu[$category][] = [
                    'id'        => (string) $m->id,
                    'nama'      => $m->name,
                    'harga'     => $m->priceForChannel('CUSTOMER_WEB'),
                    'kategori'  => $m->category,
                    'deskripsi' => $m->description ?? '',
                    'sku'       => $m->sku,
                    'image_url' => $m->image_url,
                ];
            }
        } else {
            // Fallback default catalog jika database belum di-seed
            $katalogMenu = [
                'Martabak Telur' => [
                    [
                        'id'        => 'm1',
                        'nama'      => 'Martabak Sosis/Jamur Biasa',
                        'harga'     => 20000,
                        'kategori'  => 'Martabak Telur',
                        'deskripsi' => 'Kulit renyah gurih dengan isian telur & sosis/jamur pilihan',
                    ],
                    [
                        'id'        => 'm2',
                        'nama'      => 'Martabak Sosis/Jamur Spesial',
                        'harga'     => 30000,
                        'kategori'  => 'Martabak Telur',
                        'deskripsi' => 'Porsi spesial telur lebih tebal dengan isian sosis/jamur melimpah',
                    ],
                    [
                        'id'        => 'm3',
                        'nama'      => 'Martabak Daging Ayam Biasa',
                        'harga'     => 25000,
                        'kategori'  => 'Martabak Telur',
                        'deskripsi' => 'Daging ayam cincang gurih dipadu telur bebek berkualitas',
                    ],
                    [
                        'id'        => 'm4',
                        'nama'      => 'Martabak Daging Ayam Spesial',
                        'harga'     => 35000,
                        'kategori'  => 'Martabak Telur',
                        'deskripsi' => 'Daging ayam spesial porsi besar dengan aroma rempah harum',
                    ],
                    [
                        'id'        => 'm5',
                        'nama'      => 'Martabak Daging Sapi Biasa',
                        'harga'     => 30000,
                        'kategori'  => 'Martabak Telur',
                        'deskripsi' => 'Daging sapi cincang lezat pilihan favorit pelanggan setia',
                    ],
                    [
                        'id'        => 'm6',
                        'nama'      => 'Martabak Daging Sapi Spesial',
                        'harga'     => 40000,
                        'kategori'  => 'Martabak Telur',
                        'deskripsi' => 'Daging sapi spesial berlimpah dengan bumbu racikan khas Jenggirat',
                    ],
                    [
                        'id'        => 'm7',
                        'nama'      => 'Martabak Mozarella 1 Isian',
                        'harga'     => 50000,
                        'kategori'  => 'Martabak Telur',
                        'deskripsi' => 'Martabak telur gurih dibalut lelehan keju mozzarella molor nikmat',
                    ],
                ],
                'Terang Bulan' => [
                    [
                        'id'        => 'tb1',
                        'nama'      => 'Terang Bulan 1 Toping Biasa',
                        'harga'     => 18000,
                        'kategori'  => 'Terang Bulan',
                        'deskripsi' => 'Adonan lembut bersarang mentega manis wangi dengan 1 pilihan topping',
                    ],
                    [
                        'id'        => 'tb2',
                        'nama'      => 'Terang Bulan 1 Toping Besar',
                        'harga'     => 25000,
                        'kategori'  => 'Terang Bulan',
                        'deskripsi' => 'Porsi besar tebal pas dinikmati bersama keluarga tercinta',
                    ],
                    [
                        'id'        => 'tb3',
                        'nama'      => 'Terang Bulan 2 Toping Biasa',
                        'harga'     => 23000,
                        'kategori'  => 'Terang Bulan',
                        'deskripsi' => 'Kombinasi 2 topping lezat (Keju + Meses Coklat / Kacang)',
                    ],
                    [
                        'id'        => 'tb4',
                        'nama'      => 'Terang Bulan Cut Pizza All In One',
                        'harga'     => 45000,
                        'kategori'  => 'Terang Bulan',
                        'deskripsi' => 'Potongan pizza aneka topping warna-warni premium lezat melimpah',
                    ],
                ],
                'Minuman' => [
                    [
                        'id'        => 'dr1',
                        'nama'      => 'Es Teh Manis Jumbo',
                        'harga'     => 5000,
                        'kategori'  => 'Minuman',
                        'deskripsi' => 'Teh melati wangi segar dingin ukuran jumbo penyejuk dahaga',
                    ],
                    [
                        'id'        => 'dr2',
                        'nama'      => 'Es Jeruk Peras',
                        'harga'     => 7000,
                        'kategori'  => 'Minuman',
                        'deskripsi' => 'Jeruk peras murni kaya vitamin C nikmat menyegarkan',
                    ],
                ],
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

        $order = DB::transaction(function () use ($validated, $items, $orderNumber) {
            $totalPrice = 0;
            $totalItems = 0;

            foreach ($items as $item) {
                $qty = max(1, (int) ($item['qty'] ?? 1));
                $subtotal = (int) ($item['subtotal'] ?? 0);
                $totalPrice += $subtotal;
                $totalItems += $qty;
            }

            $order = Order::create([
                'order_number'   => $orderNumber,
                'customer_name'  => $validated['customer_name'],
                'customer_phone' => $validated['customer_phone'],
                'payment_method' => $validated['payment_method'],
                'payment_status' => ($validated['payment_method'] === 'QRIS Langsung') ? 'PAID' : 'UNPAID',
                'pickup_time'    => $validated['pickup_time'] ?: 'Langsung (15-20 mnt)',
                'notes'          => $validated['notes'] ?? '',
                'total_price'    => $totalPrice,
                'total_items'    => $totalItems,
                'status'         => 'PENDING',
                'branch_name'    => 'Jenggirat Kediri',
            ]);

            foreach ($items as $item) {
                OrderItem::create([
                    'order_id'      => $order->id,
                    'menu_name'     => $item['nama'],
                    'category'      => $item['kategori'] ?? 'Martabak Telur',
                    'base_price'    => (int) ($item['hargaDasar'] ?? 0),
                    'extra_price'   => (int) ($item['extraHarga'] ?? 0),
                    'unit_price'    => (int) ($item['hargaSatuan'] ?? 0),
                    'quantity'      => (int) ($item['qty'] ?? 1),
                    'subtotal'      => (int) ($item['subtotal'] ?? 0),
                    'toppings_json' => $item['toppings'] ?? [],
                    'notes'         => $item['catatan'] ?? null,
                ]);
            }

            return $order;
        });

        // Sync dengan Firestore
        $this->firestoreSync->syncOrder($order);

        return redirect()->route('order.track', $order->order_number)
            ->with('success', 'Pesanan Anda berhasil dikirim! Menunggu konfirmasi dari kasir.');
    }

    /**
     * Halaman Live Tracking Status Pesanan & Barcode.
     */
    public function track(string $orderNumber)
    {
        $order = Order::with('items')->where('order_number', $orderNumber)->firstOrFail();
        return view('customer.status', compact('order'));
    }

    /**
     * API Status Polling untuk Realtime Update di Browser Pelanggan.
     */
    public function checkStatus(string $orderNumber)
    {
        $order = Order::where('order_number', $orderNumber)->first();

        if (!$order) {
            return response()->json(['error' => 'Pesanan tidak ditemukan'], 404);
        }

        return response()->json([
            'order_number'   => $order->order_number,
            'status'         => $order->status,
            'payment_status' => $order->payment_status,
            'barcode_url'    => $order->barcode_url,
            'total_price'    => $order->total_price,
            'updated_at'     => $order->updated_at->toIso8601String(),
        ]);
    }
}
