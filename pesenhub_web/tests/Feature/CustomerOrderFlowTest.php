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
     * Test customer can login with name and phone number, creating 5-hour session.
     */
    public function test_customer_can_login_with_name_and_phone(): void
    {
        $response = $this->post(route('customer.login.submit'), [
            'customer_name'  => 'Yoga Ananda',
            'customer_phone' => '081234567890',
        ]);

        $response->assertRedirect(route('order.menu'));
        $this->assertEquals('Yoga Ananda', session('customer_name'));
        $this->assertEquals('081234567890', session('customer_phone'));
        $this->assertGreaterThan(time() + (4 * 3600), session('customer_session_expires_at'));
        $this->assertLessThanOrEqual(time() + (5 * 3600), session('customer_session_expires_at'));

        // Check catalog view displays customer name
        $menuRes = $this->get(route('order.menu'));
        $menuRes->assertSee('Yoga Ananda');
    }

    /**
     * Test customer can logout / end guest session.
     */
    public function test_customer_can_logout(): void
    {
        $this->withSession([
            'customer_name'               => 'Yoga Ananda',
            'customer_phone'              => '081234567890',
            'customer_session_expires_at' => time() + 3600,
        ]);

        $response = $this->post(route('customer.logout'));
        $response->assertRedirect(route('order.menu'));
        $this->assertNull(session('customer_name'));
        $this->assertNull(session('customer_phone'));
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

        // Verify "Cek Pesanan" button appears on the menu catalog
        $catalogResponse = $this->get(route('order.menu'));
        $catalogResponse->assertStatus(200);
        $catalogResponse->assertSee('Pesanan Aktif Anda');
        $catalogResponse->assertSee($order['orderNumber']);
        $catalogResponse->assertSee('Cek Pesanan');
    }

    /**
     * Test /cek-pesanan redirects customer directly to their active order.
     */
    public function test_customer_can_use_cek_pesanan_redirect(): void
    {
        $this->withSession([
            'last_order_number' => 'ORD-WEB-TEST-777',
        ]);

        $response = $this->get(route('order.check.redirect'));
        $response->assertRedirect(route('order.track', 'ORD-WEB-TEST-777'));
    }

    /**
     * Test customer tracking page displays confirmed status and barcode once confirmed in Firestore.
     */
    public function test_customer_can_view_confirmed_order_with_barcode(): void
    {
        $orderNumber = 'ORD-WEB-CONFIRMED-999';
        FirestoreService::$fakeOrders[$orderNumber] = [
            'orderNumber'   => $orderNumber,
            'customerName'  => 'Yoga Ananda',
            'customerPhone' => '081234567890',
            'paymentMethod' => 'Bayar di Tempat (Saat Ambil)',
            'paymentStatus' => 'UNPAID',
            'pickupTime'    => 'Langsung (15-20 mnt)',
            'total'         => 35000,
            'totalItems'    => 1,
            'status'        => 'CONFIRMED',
            'barcodeCode'   => $orderNumber,
            'barcodeUrl'    => 'https://example.com/barcode.png',
        ];

        $response = $this->get(route('order.track', $orderNumber));
        $response->assertStatus(200);
        $response->assertSee('Pesanan Dikonfirmasi');
        $response->assertSee('Barcode Pengambilan Pesanan');
        $response->assertSee($orderNumber);

        // Check API polling
        $apiResponse = $this->get(route('order.status.check', $orderNumber));
        $apiResponse->assertStatus(200);
        $apiResponse->assertJson([
            'order_number' => $orderNumber,
            'status'       => 'CONFIRMED',
        ]);
    }
}
