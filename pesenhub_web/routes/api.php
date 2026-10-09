<?php

use App\Http\Controllers\CustomerOrderController;
use App\Models\Order;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes - PesenHub Jenggirat
|--------------------------------------------------------------------------
*/

// Check status pesanan realtime
Route::get('/pesanan/{orderNumber}/status', [CustomerOrderController::class, 'checkStatus']);

// Submit pesanan via API
Route::post('/pesanan', [CustomerOrderController::class, 'store']);

