<?php

namespace Database\Seeders;

use App\Models\Menu;
use Illuminate\Database\Seeder;

class MenuCatalogSeeder extends Seeder
{
    public function run(): void
    {
        $menus = [
            [
                'sku'         => 'MRT-001',
                'name'        => 'Martabak Sosis/Jamur Biasa',
                'category'    => 'Martabak Telur',
                'description' => 'Kulit renyah gurih dengan isian telur & sosis/jamur pilihan',
                'base_price'  => 20000,
                'hpp_amount'  => 12000,
                'prices'      => ['OFFLINE' => 20000, 'CUSTOMER_WEB' => 20000, 'GOFOOD' => 24000, 'GRABFOOD' => 24000, 'SHOPEEFOOD' => 23000],
            ],
            [
                'sku'         => 'MRT-002',
                'name'        => 'Martabak Sosis/Jamur Spesial',
                'category'    => 'Martabak Telur',
                'description' => 'Porsi spesial telur lebih tebal dengan isian sosis/jamur melimpah',
                'base_price'  => 30000,
                'hpp_amount'  => 18000,
                'prices'      => ['OFFLINE' => 30000, 'CUSTOMER_WEB' => 30000, 'GOFOOD' => 36000, 'GRABFOOD' => 36000, 'SHOPEEFOOD' => 35000],
            ],
            [
                'sku'         => 'MRT-003',
                'name'        => 'Martabak Daging Ayam Biasa',
                'category'    => 'Martabak Telur',
                'description' => 'Daging ayam cincang gurih dipadu telur bebek berkualitas',
                'base_price'  => 25000,
                'hpp_amount'  => 15000,
                'prices'      => ['OFFLINE' => 25000, 'CUSTOMER_WEB' => 25000, 'GOFOOD' => 30000, 'GRABFOOD' => 30000, 'SHOPEEFOOD' => 29000],
            ],
            [
                'sku'         => 'MRT-004',
                'name'        => 'Martabak Daging Ayam Spesial',
                'category'    => 'Martabak Telur',
                'description' => 'Daging ayam spesial porsi besar dengan aroma rempah harum',
                'base_price'  => 35000,
                'hpp_amount'  => 20000,
                'prices'      => ['OFFLINE' => 35000, 'CUSTOMER_WEB' => 35000, 'GOFOOD' => 42000, 'GRABFOOD' => 42000, 'SHOPEEFOOD' => 40000],
            ],
            [
                'sku'         => 'MRT-005',
                'name'        => 'Martabak Daging Sapi Biasa',
                'category'    => 'Martabak Telur',
                'description' => 'Daging sapi cincang lezat pilihan favorit pelanggan setia',
                'base_price'  => 30000,
                'hpp_amount'  => 18000,
                'prices'      => ['OFFLINE' => 30000, 'CUSTOMER_WEB' => 30000, 'GOFOOD' => 36000, 'GRABFOOD' => 36000, 'SHOPEEFOOD' => 35000],
            ],
            [
                'sku'         => 'MRT-006',
                'name'        => 'Martabak Daging Sapi Spesial',
                'category'    => 'Martabak Telur',
                'description' => 'Daging sapi spesial berlimpah dengan bumbu racikan khas Jenggirat',
                'base_price'  => 40000,
                'hpp_amount'  => 24000,
                'prices'      => ['OFFLINE' => 40000, 'CUSTOMER_WEB' => 40000, 'GOFOOD' => 48000, 'GRABFOOD' => 48000, 'SHOPEEFOOD' => 46000],
            ],
            [
                'sku'         => 'MRT-007',
                'name'        => 'Martabak Mozarella 1 Isian',
                'category'    => 'Martabak Telur',
                'description' => 'Martabak telur gurih dibalut lelehan keju mozzarella molor nikmat',
                'base_price'  => 50000,
                'hpp_amount'  => 30000,
                'prices'      => ['OFFLINE' => 50000, 'CUSTOMER_WEB' => 50000, 'GOFOOD' => 60000, 'GRABFOOD' => 60000, 'SHOPEEFOOD' => 58000],
            ],
            [
                'sku'         => 'TRB-001',
                'name'        => 'Terang Bulan 1 Toping Biasa',
                'category'    => 'Terang Bulan',
                'description' => 'Adonan lembut bersarang mentega manis wangi dengan 1 pilihan topping',
                'base_price'  => 18000,
                'hpp_amount'  => 10000,
                'prices'      => ['OFFLINE' => 18000, 'CUSTOMER_WEB' => 18000, 'GOFOOD' => 22000, 'GRABFOOD' => 22000, 'SHOPEEFOOD' => 21000],
            ],
            [
                'sku'         => 'TRB-002',
                'name'        => 'Terang Bulan 1 Toping Besar',
                'category'    => 'Terang Bulan',
                'description' => 'Porsi besar tebal pas dinikmati bersama keluarga tercinta',
                'base_price'  => 25000,
                'hpp_amount'  => 14000,
                'prices'      => ['OFFLINE' => 25000, 'CUSTOMER_WEB' => 25000, 'GOFOOD' => 30000, 'GRABFOOD' => 30000, 'SHOPEEFOOD' => 29000],
            ],
            [
                'sku'         => 'TRB-003',
                'name'        => 'Terang Bulan 2 Toping Biasa',
                'category'    => 'Terang Bulan',
                'description' => 'Kombinasi 2 topping lezat (Keju + Meses Coklat / Kacang)',
                'base_price'  => 23000,
                'hpp_amount'  => 13000,
                'prices'      => ['OFFLINE' => 23000, 'CUSTOMER_WEB' => 23000, 'GOFOOD' => 28000, 'GRABFOOD' => 28000, 'SHOPEEFOOD' => 27000],
            ],
            [
                'sku'         => 'TRB-004',
                'name'        => 'Terang Bulan Cut Pizza All In One',
                'category'    => 'Terang Bulan',
                'description' => 'Potongan pizza aneka topping warna-warni premium lezat melimpah',
                'base_price'  => 45000,
                'hpp_amount'  => 25000,
                'prices'      => ['OFFLINE' => 45000, 'CUSTOMER_WEB' => 45000, 'GOFOOD' => 54000, 'GRABFOOD' => 54000, 'SHOPEEFOOD' => 52000],
            ],
            [
                'sku'         => 'MNM-001',
                'name'        => 'Es Teh Manis Jumbo',
                'category'    => 'Minuman',
                'description' => 'Teh melati wangi segar dingin ukuran jumbo penyejuk dahaga',
                'base_price'  => 5000,
                'hpp_amount'  => 2000,
                'prices'      => ['OFFLINE' => 5000, 'CUSTOMER_WEB' => 5000, 'GOFOOD' => 7000, 'GRABFOOD' => 7000, 'SHOPEEFOOD' => 6000],
            ],
            [
                'sku'         => 'MNM-002',
                'name'        => 'Es Jeruk Peras',
                'category'    => 'Minuman',
                'description' => 'Jeruk peras murni kaya vitamin C nikmat menyegarkan',
                'base_price'  => 7000,
                'hpp_amount'  => 3000,
                'prices'      => ['OFFLINE' => 7000, 'CUSTOMER_WEB' => 7000, 'GOFOOD' => 9000, 'GRABFOOD' => 9000, 'SHOPEEFOOD' => 8000],
            ],
        ];

        foreach ($menus as $m) {
            $menu = Menu::updateOrCreate(
                ['sku' => $m['sku']],
                [
                    'name'        => $m['name'],
                    'category'    => $m['category'],
                    'description' => $m['description'],
                    'base_price'  => $m['base_price'],
                    'hpp_amount'  => $m['hpp_amount'],
                    'is_available' => true,
                ]
            );

            $menu->syncChannelPrices($m['prices']);
        }
    }
}
