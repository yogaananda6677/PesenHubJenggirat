<?php

namespace App\DTO;

class MenuDto
{
    public string $id;
    public string $sku;
    public string $name;
    public string $category;
    public ?string $description;
    public ?string $image_url;
    public int $base_price;
    public int $hpp_amount;
    public bool $is_available;
    public array $channelPrices;

    public function __construct(array $data)
    {
        $this->id           = (string) ($data['id'] ?? $data['sku'] ?? '');
        $this->sku          = (string) ($data['sku'] ?? $this->id);
        $this->name         = (string) ($data['name'] ?? '');
        $this->category     = (string) ($data['category'] ?? 'Martabak Telur');
        $this->description  = $data['description'] ?? '';
        $this->image_url    = $data['imageUrl'] ?? $data['image_url'] ?? null;
        $this->base_price   = (int) ($data['price'] ?? $data['base_price'] ?? 0);
        $this->hpp_amount   = (int) ($data['hppAmount'] ?? $data['hpp_amount'] ?? 0);
        $this->is_available = (bool) ($data['available'] ?? $data['is_available'] ?? true);
        $this->channelPrices = (array) ($data['channelPrices'] ?? []);

        // Defaultkan channelPrices jika kosong
        if (empty($this->channelPrices['OFFLINE'])) {
            $this->channelPrices['OFFLINE'] = $this->base_price;
        }
        if (empty($this->channelPrices['CUSTOMER_WEB'])) {
            $this->channelPrices['CUSTOMER_WEB'] = $this->base_price;
        }
    }

    public function priceForChannel(string $channel): int
    {
        $ch = strtoupper($channel);
        return (int) ($this->channelPrices[$ch] ?? $this->base_price);
    }
}
