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

// =========================================================================
// MODUL 12: THREADS (COROUTINE) - SERVER BACKUP & SYNC DATA MYSQL
// =========================================================================
Route::prefix('backup')->group(function () {
    Route::post('/push', [\App\Http\Controllers\BackupSyncController::class, 'push']);
    Route::get('/orders', [\App\Http\Controllers\BackupSyncController::class, 'getOrders']);
    Route::get('/status', [\App\Http\Controllers\BackupSyncController::class, 'status']);
});


