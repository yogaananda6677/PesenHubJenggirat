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
        Schema::create('menus', function (Blueprint $table) {
            $table->id();
            $table->string('sku')->unique();
            $table->string('name');
            $table->string('category')->default('Martabak Telur');
            $table->text('description')->nullable();
            $table->text('image_url')->nullable();
            $table->unsignedBigInteger('hpp_amount')->default(0);
            $table->unsignedBigInteger('base_price')->default(0);
            $table->boolean('is_available')->default(true);
            $table->integer('sort_order')->default(0);
            $table->timestamps();
        });

        Schema::create('menu_channel_prices', function (Blueprint $table) {
            $table->id();
            $table->foreignId('menu_id')->constrained('menus')->onDelete('cascade');
            $table->string('channel'); // OFFLINE, CUSTOMER_WEB, GOFOOD, GRABFOOD, SHOPEEFOOD
            $table->unsignedBigInteger('amount');
            $table->timestamps();

            $table->unique(['menu_id', 'channel']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('menu_channel_prices');
        Schema::dropIfExists('menus');
    }
};
