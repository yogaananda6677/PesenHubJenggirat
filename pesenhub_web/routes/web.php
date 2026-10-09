<?php

use App\Http\Controllers\CustomerOrderController;
use App\Http\Controllers\KasirConfirmationController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Web Routes - PesenHub Jenggirat Web Ordering & POS
|--------------------------------------------------------------------------
*/

// Pelanggan: Guest Self-Order ala Gacoan (Tanpa Kredensial)
Route::get('/', [CustomerOrderController::class, 'index'])->name('order.menu');
Route::post('/pesan', [CustomerOrderController::class, 'store'])->name('order.store');
Route::get('/pesanan/{orderNumber}', [CustomerOrderController::class, 'track'])->name('order.track');

// API Polling Realtime Status Pesanan Pelanggan
Route::get('/api/pesanan/{orderNumber}/status', [CustomerOrderController::class, 'checkStatus'])->name('order.status.check');

// Kasir / Admin: Konfirmasi Pesanan & Upload Barcode ke Supabase
Route::get('/kasir', [KasirConfirmationController::class, 'index'])->name('kasir.antrian');
Route::post('/kasir/pesanan/{orderNumber}/konfirmasi', [KasirConfirmationController::class, 'confirm'])->name('kasir.order.confirm');
Route::post('/kasir/pesanan/{orderNumber}/selesai', [KasirConfirmationController::class, 'complete'])->name('kasir.order.complete');
