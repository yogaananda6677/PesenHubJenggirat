<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('orders', function (Blueprint $table) {
            $table->id();
            $table->string('order_number')->unique();
            $table->string('customer_name');
            $table->string('customer_phone');
            $table->string('payment_method')->default('Bayar di Kasir (Saat Ambil)');
            $table->string('payment_status')->default('UNPAID'); // UNPAID, PAID
            $table->string('pickup_time')->default('Langsung (15-20 mnt)');
            $table->text('notes')->nullable();
            $table->unsignedBigInteger('total_price')->default(0);
            $table->unsignedInteger('total_items')->default(1);
            $table->string('status')->default('PENDING'); // PENDING, CONFIRMED, PREPARING, READY, COMPLETED, CANCELLED
            $table->string('barcode_code')->nullable();
            $table->text('barcode_url')->nullable();
            $table->string('branch_name')->default('Jenggirat Kediri');
            $table->timestamps();
        });

        Schema::create('order_items', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained('orders')->onDelete('cascade');
            $table->string('menu_name');
            $table->string('category')->default('Martabak Telur');
            $table->unsignedBigInteger('base_price');
            $table->unsignedBigInteger('extra_price')->default(0);
            $table->unsignedBigInteger('unit_price');
            $table->unsignedInteger('quantity')->default(1);
            $table->unsignedBigInteger('subtotal');
            $table->json('toppings_json')->nullable();
            $table->text('notes')->nullable();
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('order_items');
        Schema::dropIfExists('orders');
    }
};
