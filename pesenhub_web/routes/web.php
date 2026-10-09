<?php

use App\Http\Controllers\CustomerAuthController;
use App\Http\Controllers\CustomerOrderController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Web Routes - PesenHub Jenggirat Web Ordering (Khusus Pelanggan)
|--------------------------------------------------------------------------
*/

// Sesi Pelanggan (Masuk Nama & No HP, Expired 5 Jam)
Route::get('/masuk', [CustomerAuthController::class, 'showLogin'])->name('customer.login');
Route::post('/masuk', [CustomerAuthController::class, 'login'])->name('customer.login.submit');
Route::post('/keluar', [CustomerAuthController::class, 'logout'])->name('customer.logout');

// Pelanggan: Katalog Menu & Pemesanan Mandiri
Route::get('/', [CustomerOrderController::class, 'index'])->name('order.menu');
Route::post('/pesan', [CustomerOrderController::class, 'store'])->name('order.store');
Route::get('/pesanan/{orderNumber}', [CustomerOrderController::class, 'track'])->name('order.track');
Route::get('/cek-pesanan', [CustomerOrderController::class, 'checkOrdersRedirect'])->name('order.check.redirect');
Route::post('/cek-pesanan', [CustomerOrderController::class, 'findOrder'])->name('order.find');
Route::get('/lokasi', [CustomerOrderController::class, 'location'])->name('outlet.location');

// API Polling Realtime Status Pesanan Pelanggan
Route::get('/api/pesanan/{orderNumber}/status', [CustomerOrderController::class, 'checkStatus'])->name('order.status.check');


