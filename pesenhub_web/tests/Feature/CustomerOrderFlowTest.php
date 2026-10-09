<?php

namespace Tests\Feature;

use App\Services\FirestoreService;
use Tests\TestCase;

class CustomerOrderFlowTest extends TestCase
{
    protected function setUp(): void
    {
        parent::setUp();

        FirestoreService::fake(
            menus: [
                'MRT-001' => [
                    'id'            => 'MRT-001',
                    'sku'           => 'MRT-001',
                    'name'          => 'Martabak Sosis/Jamur Biasa',
                    'category'      => 'Martabak Telur',
                    'description'   => 'Kulit renyah gurih',
                    'price'         => 20000,
                    'available'     => true,
                    'channelPrices' => ['CUSTOMER_WEB' => 20000, 'OFFLINE' => 20000],
                ],
                'TB-001' => [
                    'id'            => 'TB-001',
                    'sku'           => 'TB-001',
                    'name'          => 'Terang Bulan 1 Toping Biasa',
                    'category'      => 'Terang Bulan',
                    'description'   => 'Adonan lembut bersarang',
                    'price'         => 18000,
                    'available'     => true,
                    'channelPrices' => ['CUSTOMER_WEB' => 18000, 'OFFLINE' => 18000],
                ],
                'MNM-001' => [
                    'id'            => 'MNM-001',
                    'sku'           => 'MNM-001',
                    'name'          => 'Es Teh Manis Jumbo',
                    'category'      => 'Minuman',
                    'description'   => 'Teh melati wangi',
                    'price'         => 5000,
                    'available'     => true,
                    'channelPrices' => ['CUSTOMER_WEB' => 5000, 'OFFLINE' => 5000],
                ],
            ],
            orders: []
        );
    }

    protected function tearDown(): void
    {
        FirestoreService::resetFake();
        parent::tearDown();
    }

    /**
     * Test menu catalog is accessible without login (guest) from Cloud Firestore.
     */
    public function test_customer_can_view_menu_catalog_as_guest(): void
    {
        $response = $this->get('/');

        $response->assertStatus(200);
        $response->assertSee('Jenggirat');
        $response->assertSee('Martabak Sosis/Jamur Biasa');
        $response->assertSee('Terang Bulan 1 Toping Biasa');
        $response->assertSee('Es Teh Manis Jumbo');
    }

    /**
     * Test placing an order without credentials creates order in Firestore with PENDING status.
     */
    public function test_customer_can_place_order_and_status_is_pending(): void
    {
        $payload = [
            'customer_name'  => 'Rizqi Yoga',
            'customer_phone' => '081234567890',
            'payment_method' => 'Bayar di Kasir (Saat Ambil)',
            'pickup_time'    => 'Langsung (15-20 mnt)',
            'notes'          => 'Tolong agak garing',
            'items_json'     => json_encode([
                [
                    'id'          => 'MRT-001',
                    'nama'        => 'Martabak Sosis/Jamur Biasa',
                    'kategori'    => 'Martabak Telur',
                    'hargaDasar'  => 20000,
                    'extraHarga'  => 15000,
                    'hargaSatuan' => 35000,
                    'qty'         => 2,
                    'subtotal'    => 70000,
                    'toppings'    => ['Keju Mozzarella'],
                ]
            ]),
        ];

        $response = $this->post('/pesan', $payload);

        $savedOrders = array_values(FirestoreService::$fakeOrders);
        $this->assertNotEmpty($savedOrders);

        $order = $savedOrders[0];
        $this->assertEquals('Rizqi Yoga', $order['customerName']);
        $this->assertEquals('081234567890', $order['customerPhone']);
        $this->assertEquals('PENDING', $order['status']);
        $this->assertEquals(70000, $order['total']);
        $this->assertEquals(2, $order['totalItems']);

        $response->assertRedirect(route('order.track', $order['orderNumber']));

        // Check tracking view
        $trackResponse = $this->get(route('order.track', $order['orderNumber']));
        $trackResponse->assertStatus(200);
        $trackResponse->assertSee('Menunggu Konfirmasi');
    }

    /**
     * Test cashier confirmation generates barcode and updates status to CONFIRMED in Firestore.
     */
    public function test_cashier_can_confirm_order_and_generate_barcode(): void
    {
        $orderNumber = 'ORD-WEB-TEST-1234';
        FirestoreService::$fakeOrders[$orderNumber] = [
            'orderNumber'   => $orderNumber,
            'customerName'  => 'Budi Test',
            'customerPhone' => '081987654321',
            'paymentMethod' => 'Bayar di Kasir (Saat Ambil)',
            'paymentStatus' => 'UNPAID',
            'pickupTime'    => 'Langsung (15-20 mnt)',
            'total'         => 35000,
            'totalItems'    => 1,
            'status'        => 'PENDING',
        ];

        $response = $this->post(route('kasir.order.confirm', $orderNumber));
        $response->assertRedirect();

        $updated = FirestoreService::$fakeOrders[$orderNumber];
        $this->assertEquals('CONFIRMED', $updated['status']);
        $this->assertNotEmpty($updated['barcodeCode']);
        $this->assertNotEmpty($updated['barcodeUrl']);

        // Check API polling
        $apiResponse = $this->get(route('order.status.check', $orderNumber));
        $apiResponse->assertStatus(200);
        $apiResponse->assertJson([
            'order_number' => $orderNumber,
            'status'       => 'CONFIRMED',
        ]);
    }

    /**
     * Test cashier completing order when customer picks up and presents barcode in Firestore.
     */
    public function test_cashier_can_complete_order(): void
    {
        $orderNumber = 'ORD-WEB-PICKUP-5678';
        FirestoreService::$fakeOrders[$orderNumber] = [
            'orderNumber'   => $orderNumber,
            'customerName'  => 'Pelanggan Pickup',
            'customerPhone' => '081111222333',
            'paymentMethod' => 'Bayar di Kasir (Saat Ambil)',
            'paymentStatus' => 'UNPAID',
            'pickupTime'    => 'Langsung (15-20 mnt)',
            'total'         => 50000,
            'totalItems'    => 1,
            'status'        => 'CONFIRMED',
            'barcodeCode'   => $orderNumber,
            'barcodeUrl'    => 'https://example.com/barcode.png',
        ];

        $response = $this->post(route('kasir.order.complete', $orderNumber));
        $response->assertRedirect();

        $updated = FirestoreService::$fakeOrders[$orderNumber];
        $this->assertEquals('COMPLETED', $updated['status']);
        $this->assertEquals('PAID', $updated['paymentStatus']);
    }
}
