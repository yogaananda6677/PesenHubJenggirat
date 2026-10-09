<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Order extends Model
{
    protected $fillable = [
        'order_number',
        'customer_name',
        'customer_phone',
        'payment_method',
        'payment_status',
        'pickup_time',
        'notes',
        'total_price',
        'total_items',
        'status',
        'barcode_code',
        'barcode_url',
        'branch_name',
    ];

    public function items(): HasMany
    {
        return $this->hasMany(OrderItem::class);
    }
}
