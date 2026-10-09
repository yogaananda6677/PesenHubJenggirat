<?php

use App\Http\Controllers\CustomerOrderController;
use App\Http\Controllers\KasirConfirmationController;
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

// List pesanan untuk integrasi Android POS
Route::get('/pesanan', function (Request $request) {
    $status = $request->query('status');
    $query = Order::with('items')->orderBy('created_at', 'desc');

    if ($status) {
        $query->where('status', $status);
    }

    return response()->json([
        'success' => true,
        'data'    => $query->get(),
    ]);
});

// Submit pesanan via API
Route::post('/pesanan', [CustomerOrderController::class, 'store']);

// Konfirmasi pesanan via API
Route::post('/pesanan/{orderNumber}/konfirmasi', [KasirConfirmationController::class, 'confirm']);

// Selesaikan pesanan via API
Route::post('/pesanan/{orderNumber}/selesai', [KasirConfirmationController::class, 'complete']);
