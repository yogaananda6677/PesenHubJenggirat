<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * Tabel backup_orders digunakan sebagai Server Backup MySQL untuk Modul 12 (Threads & Coroutine).
     */
    public function up(): void
    {
        Schema::create('backup_orders', function (Blueprint $table) {
            $table->id();
            $table->string('order_number')->unique();
            $table->string('customer_name');
            $table->string('customer_phone')->nullable();
            $table->text('detail_item')->nullable();
            $table->string('payment_method')->default('Tunai');
            $table->unsignedBigInteger('total_price')->default(0);
            $table->string('status')->default('COMPLETED');
            $table->string('waktu_transaksi')->nullable();
            $table->string('sync_source')->default('ANDROID_COROUTINE');
            $table->timestamp('synced_at')->useCurrent();
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('backup_orders');
    }
};
