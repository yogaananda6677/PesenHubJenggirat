<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class MenuChannelPrice extends Model
{
    protected $fillable = [
        'menu_id',
        'channel',
        'amount',
    ];

    protected $casts = [
        'amount' => 'integer',
    ];

    public function menu(): BelongsTo
    {
        return $this->belongsTo(Menu::class);
    }
}
