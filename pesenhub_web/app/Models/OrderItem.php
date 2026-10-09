<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class OrderItem extends Model
{
    protected $fillable = [
        'order_id',
        'menu_name',
        'category',
        'base_price',
        'extra_price',
        'unit_price',
        'quantity',
        'subtotal',
        'toppings_json',
        'notes',
    ];

    protected $casts = [
        'toppings_json' => 'array',
    ];

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }
}
