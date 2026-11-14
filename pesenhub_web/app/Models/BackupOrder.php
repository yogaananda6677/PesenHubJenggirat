<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class BackupOrder extends Model
{
    protected $table = 'backup_orders';

    protected $fillable = [
        'order_number',
        'customer_name',
        'customer_phone',
        'detail_item',
        'payment_method',
        'total_price',
        'status',
        'waktu_transaksi',
        'sync_source',
        'synced_at',
    ];

    protected $casts = [
        'total_price' => 'integer',
        'synced_at'   => 'datetime',
    ];
}
