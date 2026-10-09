<?php

namespace Tests\Feature;

use App\Models\Menu;
use App\Models\MenuChannelPrice;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MenuManagementTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Test tampilan kelola menu menampilkan daftar menu dan multi-channel pricing.
     */
    public function test_can_render_menu_management_index(): void
    {
        $menu = Menu::create([
            'sku'          => 'MRT-TEST-01',
            'name'         => 'Martabak Super Keju Test',
            'category'     => 'Martabak Telur',
            'description'  => 'Deskripsi uji coba kelola menu',
            'base_price'   => 30000,
            'hpp_amount'   => 18000,
            'is_available' => true,
        ]);

        $menu->syncChannelPrices([
            'OFFLINE'      => 30000,
            'CUSTOMER_WEB' => 30000,
            'GOFOOD'       => 36000,
            'GRABFOOD'     => 36000,
            'SHOPEEFOOD'   => 35000,
        ]);

        $response = $this->get(route('kasir.menu.index'));

        $response->assertStatus(200);
        $response->assertSee('Kelola Menu &amp; Harga Multi-Channel', false);
        $response->assertSee('Martabak Super Keju Test');
        $response->assertSee('MRT-TEST-01');
        $response->assertSee('Rp 30.000');
        $response->assertSee('Rp 36.000');
    }

    /**
     * Test tambah menu baru beserta harga multi-channel.
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

        $this->assertDatabaseHas('menus', [
            'sku'        => 'TB-TEST-02',
            'name'       => 'Terang Bulan Red Velvet Test',
            'base_price' => 35000,
        ]);

        $menu = Menu::where('sku', 'TB-TEST-02')->first();
        $this->assertNotNull($menu);
        $this->assertEquals(37000, $menu->priceForChannel('CUSTOMER_WEB'));
        $this->assertEquals(42000, $menu->priceForChannel('GOFOOD'));
        $this->assertEquals(40000, $menu->priceForChannel('SHOPEEFOOD'));
    }

    /**
     * Test perbarui data menu dan harga multi-channel.
     */
    public function test_can_update_menu_and_channel_prices(): void
    {
        $menu = Menu::create([
            'sku'          => 'MNM-TEST-03',
            'name'         => 'Es Coklat Spesial',
            'category'     => 'Minuman',
            'description'  => 'Coklat kental manis',
            'base_price'   => 10000,
            'hpp_amount'   => 5000,
            'is_available' => true,
        ]);

        $menu->syncChannelPrices([
            'OFFLINE'      => 10000,
            'CUSTOMER_WEB' => 10000,
        ]);

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

        $response = $this->put(route('kasir.menu.update', $menu->id), $updatePayload);

        $response->assertRedirect(route('kasir.menu.index'));

        $menu->refresh();
        $this->assertEquals('Es Coklat Premium Extra', $menu->name);
        $this->assertEquals(12000, $menu->base_price);
        $this->assertEquals(13000, $menu->priceForChannel('CUSTOMER_WEB'));
        $this->assertEquals(15000, $menu->priceForChannel('GOFOOD'));
    }

    /**
     * Test toggle ketersediaan menu (Tersedia <-> Habis).
     */
    public function test_can_toggle_menu_availability(): void
    {
        $menu = Menu::create([
            'sku'          => 'MRT-TOGGLE',
            'name'         => 'Martabak Jamur Biasa',
            'category'     => 'Martabak Telur',
            'base_price'   => 20000,
            'is_available' => true,
        ]);

        $this->assertTrue($menu->is_available);

        // Toggle to false
        $response = $this->patch(route('kasir.menu.toggle', $menu->id));
        $response->assertRedirect();

        $menu->refresh();
        $this->assertFalse($menu->is_available);

        // Toggle to true
        $this->patch(route('kasir.menu.toggle', $menu->id));
        $menu->refresh();
        $this->assertTrue($menu->is_available);
    }

    /**
     * Test menu yang habis tidak muncul di halaman self-order pelanggan,
     * dan harga yang muncul adalah harga channel CUSTOMER_WEB.
     */
    public function test_customer_web_uses_customer_web_channel_price_and_availability(): void
    {
        $menuAvailable = Menu::create([
            'sku'          => 'MRT-WEB-01',
            'name'         => 'Martabak Web Eksklusif',
            'category'     => 'Martabak Telur',
            'base_price'   => 25000,
            'is_available' => true,
        ]);
        $menuAvailable->syncChannelPrices([
            'OFFLINE'      => 25000,
            'CUSTOMER_WEB' => 28000, // Harga khusus web
        ]);

        $menuOut = Menu::create([
            'sku'          => 'MRT-WEB-02',
            'name'         => 'Martabak Stok Habis',
            'category'     => 'Martabak Telur',
            'base_price'   => 30000,
            'is_available' => false, // Habis
        ]);
        $menuOut->syncChannelPrices([
            'OFFLINE'      => 30000,
            'CUSTOMER_WEB' => 30000,
        ]);

        $response = $this->get('/');

        $response->assertStatus(200);
        $response->assertSee('Martabak Web Eksklusif');
        $response->assertSee('Rp 28.000'); // Memakai harga CUSTOMER_WEB
        $response->assertDontSee('Martabak Stok Habis'); // Menu habis disembunyikan
    }
}
