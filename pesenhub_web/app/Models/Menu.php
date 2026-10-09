<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Menu extends Model
{
    public const CHANNELS = [
        'OFFLINE'      => 'Kasir / Dine-In (Offline)',
        'CUSTOMER_WEB' => 'Website Pelanggan (Web)',
        'GOFOOD'       => 'GoFood',
        'GRABFOOD'     => 'GrabFood',
        'SHOPEEFOOD'   => 'ShopeeFood',
    ];

    protected $fillable = [
        'sku',
        'name',
        'category',
        'description',
        'image_url',
        'hpp_amount',
        'base_price',
        'is_available',
        'sort_order',
    ];

    protected $casts = [
        'is_available' => 'boolean',
        'hpp_amount'   => 'integer',
        'base_price'   => 'integer',
        'sort_order'   => 'integer',
    ];

    public function channelPrices(): HasMany
    {
        return $this->hasMany(MenuChannelPrice::class);
    }

    /**
     * Dapatkan harga untuk saluran penjualan tertentu dengan fallback ke base_price.
     */
    public function priceForChannel(string $channel): int
    {
        $channelPrice = $this->channelPrices->firstWhere('channel', strtoupper($channel));
        if ($channelPrice && $channelPrice->amount > 0) {
            return (int) $channelPrice->amount;
        }

        return (int) $this->base_price;
    }

    /**
     * Simpan atau perbarui harga untuk semua saluran penjualan.
     */
    public function syncChannelPrices(array $channelAmounts): void
    {
        foreach (self::CHANNELS as $channelKey => $label) {
            if (isset($channelAmounts[$channelKey])) {
                $amount = max(0, (int) $channelAmounts[$channelKey]);
                MenuChannelPrice::updateOrCreate(
                    ['menu_id' => $this->id, 'channel' => $channelKey],
                    ['amount' => $amount]
                );
            }
        }
    }
}
