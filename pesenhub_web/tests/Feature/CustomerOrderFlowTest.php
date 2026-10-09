<?php

namespace Tests\Feature;

use App\Models\Order;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CustomerOrderFlowTest extends TestCase
{
    use RefreshDatabase;
    /**
     * Test menu catalog is accessible without login (guest).
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
     * Test placing an order without credentials creates order with PENDING status.
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
                    'id'          => 'm1',
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

        $this->assertDatabaseHas('orders', [
            'customer_name'  => 'Rizqi Yoga',
            'customer_phone' => '081234567890',
            'status'         => 'PENDING',
            'total_price'    => 70000,
            'total_items'    => 2,
        ]);

        $order = Order::where('customer_name', 'Rizqi Yoga')->first();
        $this->assertNotNull($order);

        $response->assertRedirect(route('order.track', $order->order_number));

        // Check tracking view
        $trackResponse = $this->get(route('order.track', $order->order_number));
        $trackResponse->assertStatus(200);
        $trackResponse->assertSee('Menunggu Konfirmasi');
    }

    /**
     * Test cashier confirmation generates barcode and updates status to CONFIRMED.
     */
    public function test_cashier_can_confirm_order_and_generate_barcode(): void
    {
        $order = Order::create([
            'order_number'   => 'ORD-WEB-TEST-' . rand(1000, 9999),
            'customer_name'  => 'Budi Test',
            'customer_phone' => '081987654321',
            'payment_method' => 'Bayar di Kasir (Saat Ambil)',
            'payment_status' => 'UNPAID',
            'pickup_time'    => 'Langsung (15-20 mnt)',
            'total_price'    => 35000,
            'total_items'    => 1,
            'status'         => 'PENDING',
        ]);

        $response = $this->post(route('kasir.order.confirm', $order->order_number));
        $response->assertRedirect();

        $order->refresh();

        $this->assertEquals('CONFIRMED', $order->status);
        $this->assertNotNull($order->barcode_code);
        $this->assertNotNull($order->barcode_url);
        $this->assertNotEmpty($order->barcode_url);

        // Check API polling
        $apiResponse = $this->get(route('order.status.check', $order->order_number));
        $apiResponse->assertStatus(200);
        $apiResponse->assertJson([
            'order_number' => $order->order_number,
            'status'       => 'CONFIRMED',
        ]);
    }

    /**
     * Test cashier completing order when customer picks up and presents barcode.
     */
    public function test_cashier_can_complete_order(): void
    {
        $order = Order::create([
            'order_number'   => 'ORD-WEB-PICKUP-' . rand(1000, 9999),
            'customer_name'  => 'Pelanggan Pickup',
            'customer_phone' => '081111222333',
            'payment_method' => 'Bayar di Kasir (Saat Ambil)',
            'payment_status' => 'UNPAID',
            'pickup_time'    => 'Langsung (15-20 mnt)',
            'total_price'    => 50000,
            'total_items'    => 1,
            'status'         => 'CONFIRMED',
            'barcode_code'   => 'ORD-WEB-PICKUP-1234',
            'barcode_url'    => 'https://example.com/barcode.png',
        ]);

        $response = $this->post(route('kasir.order.complete', $order->order_number));
        $response->assertRedirect();

        $order->refresh();

        $this->assertEquals('COMPLETED', $order->status);
        $this->assertEquals('PAID', $order->payment_status);
    }
}
