<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class CustomerAuthController extends Controller
{
    /**
     * Tampilkan halaman/modal login pelanggan.
     */
    public function showLogin()
    {
        // Jika sesi sudah aktif dan belum exp (5 jam), langsung ke menu
        if (session('customer_name') && session('customer_session_expires_at') > time()) {
            return redirect()->route('order.menu')
                ->with('info', 'Sesi Anda masih aktif (' . session('customer_name') . ').');
        }

        return view('customer.login');
    }

    /**
     * Proses login pelanggan dengan Nama dan Nomor HP (masa aktif 5 jam).
     */
    public function login(Request $request)
    {
        $validated = $request->validate([
            'customer_name'  => 'required|string|min:2|max:100',
            'customer_phone' => 'required|string|min:9|max:20',
        ], [
            'customer_name.required'  => 'Nama pemesan wajib diisi',
            'customer_name.min'       => 'Nama minimal 2 karakter',
            'customer_phone.required' => 'Nomor WhatsApp / HP wajib diisi',
            'customer_phone.min'      => 'Nomor HP minimal 9 digit',
        ]);

        $name = trim($validated['customer_name']);
        $phone = preg_replace('/[^0-9+]/', '', trim($validated['customer_phone']));

        // Simpan ke sesi Laravel dengan masa kedaluwarsa 5 jam
        $expiresAt = now()->addHours(5)->timestamp;

        session([
            'customer_name'               => $name,
            'customer_phone'              => $phone,
            'customer_session_expires_at' => $expiresAt,
        ]);

        $redirectUrl = $request->input('redirect_to', route('order.menu'));

        return redirect($redirectUrl)
            ->with('success', "Selamat datang, {$name}! Sesi pemesanan Anda aktif selama 5 jam.");
    }

    /**
     * Logout / ganti identitas pelanggan.
     */
    public function logout()
    {
        session()->forget([
            'customer_name',
            'customer_phone',
            'customer_session_expires_at',
        ]);

        return redirect()->route('order.menu')
            ->with('success', 'Sesi pemesanan telah diakhiri. Silakan masukkan identitas baru saat memesan.');
    }
}
