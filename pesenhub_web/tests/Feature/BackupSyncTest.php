<?php

namespace Tests\Feature;

use App\Models\BackupOrder;
use Tests\TestCase;

class BackupSyncTest extends TestCase
{
    public function test_backup_web_dashboard_can_be_rendered(): void
    {
        $response = $this->get('/backup');
        $response->assertStatus(200);
        $response->assertSee('Server Backup MySQL');
        $response->assertSee('Modul 12');
    }

    public function test_backup_push_api_persists_order(): void
    {
        $testOrderNumber = 'TEST-ORD-' . uniqid();
        $payload = [
            'id_pesanan'      => $testOrderNumber,
            'nama_pelanggan'  => 'Budi Tes Thread',
            'hp_pelanggan'    => '081234567890',
            'detail_item'     => '1x Martabak Telur Spesial Daging Sapi',
            'metode_bayar'    => 'QRIS',
            'total_bayar'     => 45000,
            'status_order'    => 'COMPLETED',
            'waktu_selesai'   => '13/11/2026 10:50',
            'sync_source'     => 'ANDROID_COROUTINE',
        ];

        $response = $this->postJson('/api/backup/push', $payload);
        $response->assertStatus(200);
        $response->assertJson([
            'success'      => true,
            'synced_count' => 1,
        ]);

        $this->assertDatabaseHas('backup_orders', [
            'order_number'  => $testOrderNumber,
            'customer_name' => 'Budi Tes Thread',
            'total_price'   => 45000,
        ]);

        // Cleanup
        BackupOrder::where('order_number', $testOrderNumber)->delete();
    }

    public function test_backup_orders_list_api(): void
    {
        $response = $this->getJson('/api/backup/orders');
        $response->assertStatus(200);
        $response->assertJsonStructure([
            'success',
            'total',
            'data',
        ]);
    }

    public function test_backup_status_api(): void
    {
        $response = $this->getJson('/api/backup/status');
        $response->assertStatus(200);
        $response->assertJson([
            'success' => true,
            'status'  => 'ONLINE',
        ]);
    }
}
