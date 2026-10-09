<?php

namespace Tests\Feature;

use App\Services\FirestoreService;
use Tests\TestCase;

class MenuManagementTest extends TestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        FirestoreService::fake();
    }

    protected function tearDown(): void
    {
        FirestoreService::resetFake();
        parent::tearDown();
    }

    /**
     * Test tampilan kelola menu menampilkan daftar menu dan multi-channel pricing dari Cloud Firestore.
     */
    public function test_can_render_menu_management_index(): void
    {
        FirestoreService::$fakeMenus['MRT-TEST-01'] = [
            'id'            => 'MRT-TEST-01',
            'sku'           => 'MRT-TEST-01',
            'name'          => 'Martabak Super Keju Test',
            'category'      => 'Martabak Telur',
            'description'   => 'Deskripsi uji coba kelola menu',
            'price'         => 30000,
            'hppAmount'     => 18000,
            'available'     => true,
            'channelPrices' => [
                'OFFLINE'      => 30000,
                'CUSTOMER_WEB' => 30000,
                'GOFOOD'       => 36000,
                'GRABFOOD'     => 36000,
                'SHOPEEFOOD'   => 35000,
            ],
        ];

        $response = $this->get(route('kasir.menu.index'));

        $response->assertStatus(200);
        $response->assertSee('Kelola Menu &amp; Harga Multi-Channel', false);
        $response->assertSee('Martabak Super Keju Test');
        $response->assertSee('MRT-TEST-01');
        $response->assertSee('Rp 30.000');
        $response->assertSee('Rp 36.000');
    }

    /**
     * Test tambah menu baru beserta harga multi-channel ke Cloud Firestore.
     */
    public function test_can_create_menu_with_multi_channel_prices(): void
    {
        $payload = [
            'sku'         => 'TB-TEST-02',
            'name'        => 'Terang Bulan Red Velvet Test',
            'category'    => 'Terang Bulan',
            'description' => 'Terang bulan red velvet lembut dengan cream cheese',
            'base_price'  => 35000,
            'hpp_amount'  => 20000,
            'is_available' => '1',
            'prices'      => [
                'OFFLINE'      => 35000,
                'CUSTOMER_WEB' => 37000,
                'GOFOOD'       => 42000,
                'GRABFOOD'     => 42000,
                'SHOPEEFOOD'   => 40000,
            ],
        ];

        $response = $this->post(route('kasir.menu.store'), $payload);

        $response->assertRedirect(route('kasir.menu.index'));

        $savedMenu = FirestoreService::$fakeMenus['TB-TEST-02'] ?? null;
        $this->assertNotNull($savedMenu);
        $this->assertEquals('Terang Bulan Red Velvet Test', $savedMenu['name']);
        $this->assertEquals(35000, $savedMenu['price']);
        $this->assertEquals(37000, $savedMenu['channelPrices']['CUSTOMER_WEB']);
        $this->assertEquals(42000, $savedMenu['channelPrices']['GOFOOD']);
        $this->assertEquals(40000, $savedMenu['channelPrices']['SHOPEEFOOD']);
    }

    /**
     * Test perbarui data menu dan harga multi-channel di Cloud Firestore.
     */
    public function test_can_update_menu_and_channel_prices(): void
    {
        FirestoreService::$fakeMenus['MNM-TEST-03'] = [
            'id'            => 'MNM-TEST-03',
            'sku'           => 'MNM-TEST-03',
            'name'          => 'Es Coklat Spesial',
            'category'      => 'Minuman',
            'description'   => 'Coklat kental manis',
            'price'         => 10000,
            'hppAmount'     => 5000,
            'available'     => true,
            'channelPrices' => [
                'OFFLINE'      => 10000,
                'CUSTOMER_WEB' => 10000,
            ],
        ];

        $updatePayload = [
            'sku'         => 'MNM-TEST-03',
            'name'        => 'Es Coklat Premium Extra',
            'category'    => 'Minuman',
            'description' => 'Coklat belgia asli',
            'base_price'  => 12000,
            'hpp_amount'  => 6000,
            'prices'      => [
                'OFFLINE'      => 12000,
                'CUSTOMER_WEB' => 13000,
                'GOFOOD'       => 15000,
            ],
        ];

        $response = $this->put(route('kasir.menu.update', 'MNM-TEST-03'), $updatePayload);

        $response->assertRedirect(route('kasir.menu.index'));

        $updated = FirestoreService::$fakeMenus['MNM-TEST-03'];
        $this->assertEquals('Es Coklat Premium Extra', $updated['name']);
        $this->assertEquals(12000, $updated['price']);
        $this->assertEquals(13000, $updated['channelPrices']['CUSTOMER_WEB']);
        $this->assertEquals(15000, $updated['channelPrices']['GOFOOD']);
    }

    /**
     * Test toggle ketersediaan menu (Tersedia <-> Habis) di Cloud Firestore.
     */
    public function test_can_toggle_menu_availability(): void
    {
        FirestoreService::$fakeMenus['MRT-TOGGLE'] = [
            'id'            => 'MRT-TOGGLE',
            'sku'           => 'MRT-TOGGLE',
            'name'          => 'Martabak Jamur Biasa',
            'category'      => 'Martabak Telur',
            'price'         => 20000,
            'available'     => true,
            'channelPrices' => ['CUSTOMER_WEB' => 20000],
        ];

        $this->assertTrue(FirestoreService::$fakeMenus['MRT-TOGGLE']['available']);

        // Toggle to false
        $response = $this->patch(route('kasir.menu.toggle', 'MRT-TOGGLE'));
        $response->assertRedirect();
        $this->assertFalse(FirestoreService::$fakeMenus['MRT-TOGGLE']['available']);

        // Toggle to true
        $this->patch(route('kasir.menu.toggle', 'MRT-TOGGLE'));
        $this->assertTrue(FirestoreService::$fakeMenus['MRT-TOGGLE']['available']);
    }

    /**
     * Test menu yang habis tidak muncul di halaman self-order pelanggan,
     * dan harga yang muncul adalah harga channel CUSTOMER_WEB dari Cloud Firestore.
     */
    public function test_customer_web_uses_customer_web_channel_price_and_availability(): void
    {
        FirestoreService::$fakeMenus['MRT-WEB-01'] = [
            'id'            => 'MRT-WEB-01',
            'sku'           => 'MRT-WEB-01',
            'name'          => 'Martabak Web Eksklusif',
            'category'      => 'Martabak Telur',
            'price'         => 25000,
            'available'     => true,
            'channelPrices' => [
                'OFFLINE'      => 25000,
                'CUSTOMER_WEB' => 28000, // Harga khusus web
            ],
        ];

        FirestoreService::$fakeMenus['MRT-WEB-02'] = [
            'id'            => 'MRT-WEB-02',
            'sku'           => 'MRT-WEB-02',
            'name'          => 'Martabak Stok Habis',
            'category'      => 'Martabak Telur',
            'price'         => 30000,
            'available'     => false, // Habis
            'channelPrices' => [
                'OFFLINE'      => 30000,
                'CUSTOMER_WEB' => 30000,
            ],
        ];

        $response = $this->get('/');

        $response->assertStatus(200);
        $response->assertSee('Martabak Web Eksklusif');
        $response->assertSee('Rp 28.000'); // Memakai harga CUSTOMER_WEB
        $response->assertDontSee('Martabak Stok Habis'); // Menu habis disembunyikan
    }
}
