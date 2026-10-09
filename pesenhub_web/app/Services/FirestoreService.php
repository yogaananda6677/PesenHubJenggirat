<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class FirestoreService
{
    protected string $projectId;
    protected string $apiKey;
    protected string $baseUrl;

    // Fake / in-memory storage for unit testing without network
    public static bool $fake = false;
    public static array $fakeMenus = [];
    public static array $fakeOrders = [];

    public function __construct()
    {
        $this->projectId = env('FIREBASE_PROJECT_ID', 'pml-yoga');
        $this->apiKey    = env('FIREBASE_API_KEY', 'AIzaSyDeZnmvCwK45LMUM1vKoMeSrrzxqa59iYc');
        $this->baseUrl   = "https://firestore.googleapis.com/v1/projects/{$this->projectId}/databases/(default)/documents";
    }

    public static function fake(array $menus = [], array $orders = []): void
    {
        self::$fake = true;
        self::$fakeMenus = $menus;
        self::$fakeOrders = $orders;
    }

    public static function resetFake(): void
    {
        self::$fake = false;
        self::$fakeMenus = [];
        self::$fakeOrders = [];
    }

    // ==========================================
    // PARSER & FORMATTER FIRESTORE
    // ==========================================

    public function parseValue(array $val)
    {
        if (isset($val['stringValue'])) return $val['stringValue'];
        if (isset($val['integerValue'])) return (int) $val['integerValue'];
        if (isset($val['doubleValue'])) return (float) $val['doubleValue'];
        if (isset($val['booleanValue'])) return (bool) $val['booleanValue'];
        if (isset($val['timestampValue'])) return $val['timestampValue'];
        if (isset($val['mapValue'])) {
            $res = [];
            foreach ($val['mapValue']['fields'] ?? [] as $k => $v) {
                $res[$k] = $this->parseValue($v);
            }
            return $res;
        }
        if (isset($val['arrayValue'])) {
            $res = [];
            foreach ($val['arrayValue']['values'] ?? [] as $v) {
                $res[] = $this->parseValue($v);
            }
            return $res;
        }
        return null;
    }

    public function formatValue($val): array
    {
        if (is_bool($val)) return ['booleanValue' => $val];
        if (is_int($val)) return ['integerValue' => (string) $val];
        if (is_float($val)) return ['doubleValue' => $val];
        if (is_array($val)) {
            // Check if associative array
            $isAssoc = !empty($val) && array_keys($val) !== range(0, count($val) - 1);
            if ($isAssoc) {
                $fields = [];
                foreach ($val as $k => $v) {
                    $fields[$k] = $this->formatValue($v);
                }
                return ['mapValue' => ['fields' => $fields]];
            } else {
                $values = [];
                foreach ($val as $v) {
                    $values[] = $this->formatValue($v);
                }
                return ['arrayValue' => ['values' => $values]];
            }
        }
        return ['stringValue' => (string) ($val ?? '')];
    }

    public function parseDocument(array $doc): array
    {
        $id = basename($doc['name'] ?? '');
        $fields = [];
        foreach ($doc['fields'] ?? [] as $key => $val) {
            $fields[$key] = $this->parseValue($val);
        }
        $fields['id'] = $id;
        $fields['createTime'] = $doc['createTime'] ?? null;
        $fields['updateTime'] = $doc['updateTime'] ?? null;

        return $fields;
    }

    // ==========================================
    // MENUS COLLECTION CRUD
    // ==========================================

    /**
     * Dapatkan semua menu dari Firestore.
     */
    public function getMenus(?string $kategori = null, ?string $keyword = null): array
    {
        if (self::$fake) {
            $results = array_values(self::$fakeMenus);
        } else {
            try {
                $url = "{$this->baseUrl}/menus?pageSize=100&key={$this->apiKey}";
                $response = Http::timeout(10)->get($url);
                $results = [];

                if ($response->successful()) {
                    $data = $response->json();
                    foreach ($data['documents'] ?? [] as $doc) {
                        $parsed = $this->parseDocument($doc);
                        // Standardize channelPrices fallback
                        $basePrice = (int) ($parsed['price'] ?? $parsed['base_price'] ?? 0);
                        if (!isset($parsed['channelPrices']) || !is_array($parsed['channelPrices'])) {
                            $parsed['channelPrices'] = [
                                'OFFLINE'      => $basePrice,
                                'CUSTOMER_WEB' => $basePrice,
                                'GOFOOD'       => (int) round($basePrice * 1.2),
                                'GRABFOOD'     => (int) round($basePrice * 1.2),
                                'SHOPEEFOOD'   => (int) round($basePrice * 1.15),
                            ];
                        }
                        $results[] = $parsed;
                    }
                }
            } catch (\Throwable $e) {
                Log::warning('Firestore getMenus error: ' . $e->getMessage());
                $results = [];
            }
        }

        // Filter kategori jika diminta
        if ($kategori && $kategori !== 'Semua') {
            $results = array_filter($results, fn($m) => ($m['category'] ?? '') === $kategori);
        }

        // Filter keyword pencarian jika ada
        if ($keyword) {
            $kw = strtolower($keyword);
            $results = array_filter($results, function ($m) use ($kw) {
                return str_contains(strtolower($m['name'] ?? ''), $kw)
                    || str_contains(strtolower($m['sku'] ?? ''), $kw)
                    || str_contains(strtolower($m['description'] ?? ''), $kw);
            });
        }

        // Urutkan berdasarkan kategori lalu nama
        usort($results, function ($a, $b) {
            $catCmp = strcmp($a['category'] ?? '', $b['category'] ?? '');
            if ($catCmp !== 0) return $catCmp;
            return strcmp($a['name'] ?? '', $b['name'] ?? '');
        });

        return array_values($results);
    }

    /**
     * Dapatkan satu menu berdasarkan doc ID atau SKU.
     */
    public function getMenu(string $id): ?array
    {
        if (self::$fake) {
            return self::$fakeMenus[$id] ?? null;
        }

        try {
            $url = "{$this->baseUrl}/menus/{$id}?key={$this->apiKey}";
            $response = Http::timeout(10)->get($url);
            if ($response->successful()) {
                $parsed = $this->parseDocument($response->json());
                $basePrice = (int) ($parsed['price'] ?? 0);
                if (!isset($parsed['channelPrices']) || !is_array($parsed['channelPrices'])) {
                    $parsed['channelPrices'] = [
                        'OFFLINE'      => $basePrice,
                        'CUSTOMER_WEB' => $basePrice,
                        'GOFOOD'       => (int) round($basePrice * 1.2),
                        'GRABFOOD'     => (int) round($basePrice * 1.2),
                        'SHOPEEFOOD'   => (int) round($basePrice * 1.15),
                    ];
                }
                return $parsed;
            }
        } catch (\Throwable $e) {
            Log::warning("Firestore getMenu({$id}) error: " . $e->getMessage());
        }

        return null;
    }

    /**
     * Simpan / Perbarui menu di Firestore.
     */
    public function saveMenu(array $data): array
    {
        $docId = $data['sku'] ?? $data['id'] ?? ('MENU-' . time());
        $basePrice = (int) ($data['base_price'] ?? $data['price'] ?? 0);

        $channelPrices = $data['prices'] ?? $data['channelPrices'] ?? [];
        if (empty($channelPrices['OFFLINE'])) {
            $channelPrices['OFFLINE'] = $basePrice;
        }

        $formattedFields = [
            'sku'           => $this->formatValue(strtoupper(trim($docId))),
            'name'          => $this->formatValue(trim($data['name'] ?? '')),
            'category'      => $this->formatValue($data['category'] ?? 'Martabak Telur'),
            'description'   => $this->formatValue($data['description'] ?? ''),
            'price'         => $this->formatValue($basePrice),
            'hppAmount'     => $this->formatValue((int) ($data['hpp_amount'] ?? $data['hppAmount'] ?? 0)),
            'available'     => $this->formatValue((bool) ($data['available'] ?? $data['is_available'] ?? true)),
            'imageUrl'      => $this->formatValue($data['image_url'] ?? $data['imageUrl'] ?? 'default_food_icon'),
            'channelPrices' => $this->formatValue($channelPrices),
            'updatedAt'     => ['timestampValue' => now()->toIso8601String()],
        ];

        if (self::$fake) {
            $record = [
                'id'            => $docId,
                'sku'           => $docId,
                'name'          => $data['name'],
                'category'      => $data['category'],
                'description'   => $data['description'] ?? '',
                'price'         => $basePrice,
                'hppAmount'     => (int) ($data['hpp_amount'] ?? 0),
                'available'     => (bool) ($data['available'] ?? $data['is_available'] ?? true),
                'imageUrl'      => $data['image_url'] ?? 'default_food_icon',
                'channelPrices' => $channelPrices,
            ];
            self::$fakeMenus[$docId] = $record;
            return $record;
        }

        try {
            $url = "{$this->baseUrl}/menus/{$docId}?key={$this->apiKey}";
            $response = Http::timeout(10)->patch($url, [
                'fields' => $formattedFields
            ]);

            if ($response->successful()) {
                return $this->parseDocument($response->json());
            }
        } catch (\Throwable $e) {
            Log::warning("Firestore saveMenu error: " . $e->getMessage());
        }

        return $data;
    }

    /**
     * Toggle ketersediaan menu (Tersedia / Habis) di Firestore.
     */
    public function toggleMenuAvailability(string $id): bool
    {
        $menu = $this->getMenu($id);
        if (!$menu) return false;

        $newAvailable = !($menu['available'] ?? true);

        if (self::$fake) {
            self::$fakeMenus[$id]['available'] = $newAvailable;
            return true;
        }

        try {
            $url = "{$this->baseUrl}/menus/{$id}?updateMask.fieldPaths=available&key={$this->apiKey}";
            $response = Http::timeout(10)->patch($url, [
                'fields' => [
                    'available' => ['booleanValue' => $newAvailable]
                ]
            ]);
            return $response->successful();
        } catch (\Throwable $e) {
            Log::warning("Firestore toggleMenuAvailability error: " . $e->getMessage());
            return false;
        }
    }

    /**
     * Hapus menu dari Firestore.
     */
    public function deleteMenu(string $id): bool
    {
        if (self::$fake) {
            unset(self::$fakeMenus[$id]);
            return true;
        }

        try {
            $url = "{$this->baseUrl}/menus/{$id}?key={$this->apiKey}";
            $response = Http::timeout(10)->delete($url);
            return $response->successful();
        } catch (\Throwable $e) {
            Log::warning("Firestore deleteMenu error: " . $e->getMessage());
            return false;
        }
    }

    // ==========================================
    // ORDERS COLLECTION CRUD
    // ==========================================

    /**
     * Dapatkan semua pesanan dari Firestore (antrean kasir).
     */
    public function getOrders(?string $status = null): array
    {
        if (self::$fake) {
            $orders = array_values(self::$fakeOrders);
            if ($status) {
                $orders = array_filter($orders, fn($o) => ($o['status'] ?? '') === $status);
            }
            return array_values($orders);
        }

        try {
            $url = "{$this->baseUrl}/orders?pageSize=100&key={$this->apiKey}";
            $response = Http::timeout(10)->get($url);
            $results = [];

            if ($response->successful()) {
                $data = $response->json();
                foreach ($data['documents'] ?? [] as $doc) {
                    $parsed = $this->parseDocument($doc);
                    $results[] = $parsed;
                }
            }

            // Urutkan dari yang terbaru (createdAt descending)
            usort($results, function ($a, $b) {
                return strcmp($b['createdAt'] ?? $b['createTime'] ?? '', $a['createdAt'] ?? $a['createTime'] ?? '');
            });

            if ($status) {
                $results = array_filter($results, fn($o) => ($o['status'] ?? '') === $status);
            }

            return array_values($results);
        } catch (\Throwable $e) {
            Log::warning("Firestore getOrders error: " . $e->getMessage());
            return [];
        }
    }

    /**
     * Dapatkan satu pesanan berdasarkan orderNumber.
     */
    public function getOrder(string $orderNumber): ?array
    {
        if (self::$fake) {
            return self::$fakeOrders[$orderNumber] ?? null;
        }

        try {
            $url = "{$this->baseUrl}/orders/{$orderNumber}?key={$this->apiKey}";
            $response = Http::timeout(10)->get($url);
            if ($response->successful()) {
                return $this->parseDocument($response->json());
            }
        } catch (\Throwable $e) {
            Log::warning("Firestore getOrder({$orderNumber}) error: " . $e->getMessage());
        }

        return null;
    }

    /**
     * Simpan pesanan baru pelanggan ke Firestore.
     */
    public function createOrder(array $orderData): array
    {
        $orderNumber = $orderData['orderNumber'] ?? ('ORD-WEB-' . date('Ymd-His') . '-' . rand(100, 999));
        $orderData['orderNumber'] = $orderNumber;

        if (self::$fake) {
            self::$fakeOrders[$orderNumber] = $orderData;
            return $orderData;
        }

        $formattedFields = [];
        foreach ($orderData as $k => $v) {
            $formattedFields[$k] = $this->formatValue($v);
        }
        $formattedFields['createdAt'] = ['timestampValue' => now()->toIso8601String()];

        try {
            $url = "{$this->baseUrl}/orders/{$orderNumber}?key={$this->apiKey}";
            $response = Http::timeout(10)->patch($url, [
                'fields' => $formattedFields
            ]);

            if ($response->successful()) {
                return $this->parseDocument($response->json());
            }
        } catch (\Throwable $e) {
            Log::warning("Firestore createOrder error: " . $e->getMessage());
        }

        return $orderData;
    }

    /**
     * Perbarui field tertentu pada pesanan Firestore.
     */
    public function updateOrder(string $orderNumber, array $fields): bool
    {
        if (self::$fake) {
            if (isset(self::$fakeOrders[$orderNumber])) {
                self::$fakeOrders[$orderNumber] = array_merge(self::$fakeOrders[$orderNumber], $fields);
                return true;
            }
            return false;
        }

        $updateMasks = [];
        $firestoreFields = [];
        foreach ($fields as $k => $v) {
            $updateMasks[] = "updateMask.fieldPaths={$k}";
            $firestoreFields[$k] = $this->formatValue($v);
        }
        $updateMasks[] = "updateMask.fieldPaths=updatedAt";
        $firestoreFields['updatedAt'] = ['timestampValue' => now()->toIso8601String()];

        $maskQuery = implode('&', $updateMasks);

        try {
            $url = "{$this->baseUrl}/orders/{$orderNumber}?{$maskQuery}&key={$this->apiKey}";
            $response = Http::timeout(10)->patch($url, [
                'fields' => $firestoreFields
            ]);

            return $response->successful();
        } catch (\Throwable $e) {
            Log::warning("Firestore updateOrder error: " . $e->getMessage());
            return false;
        }
    }
}
