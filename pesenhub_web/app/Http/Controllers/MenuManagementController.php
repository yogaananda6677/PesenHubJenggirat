<?php

namespace App\Http\Controllers;

use App\Models\Menu;
use App\Models\MenuChannelPrice;
use App\Services\FirestoreSyncService;
use App\Services\SupabaseStorageService;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class MenuManagementController extends Controller
{
    protected SupabaseStorageService $supabaseService;
    protected FirestoreSyncService $firestoreSync;

    public function __construct(
        SupabaseStorageService $supabaseService,
        FirestoreSyncService $firestoreSync
    ) {
        $this->supabaseService = $supabaseService;
        $this->firestoreSync   = $firestoreSync;
    }

    /**
     * Tampilkan daftar kelola menu dengan multi-channel pricing.
     */
    public function index(Request $request)
    {
        $kategoriAktif = $request->query('kategori', 'Semua');
        $keyword = $request->query('q', '');

        $query = Menu::with('channelPrices')->orderBy('category')->orderBy('name');

        if ($kategoriAktif !== 'Semua') {
            $query->where('category', $kategoriAktif);
        }

        if (!empty($keyword)) {
            $query->where(function ($q) use ($keyword) {
                $q->where('name', 'like', "%{$keyword}%")
                  ->orWhere('sku', 'like', "%{$keyword}%")
                  ->orWhere('description', 'like', "%{$keyword}%");
            });
        }

        $menus = $query->get();
        $channels = Menu::CHANNELS;

        return view('kasir.menu.index', compact('menus', 'kategoriAktif', 'keyword', 'channels'));
    }

    /**
     * Form tambah menu baru.
     */
    public function create()
    {
        $menu = new Menu([
            'category'     => 'Martabak Telur',
            'is_available' => true,
            'base_price'   => 0,
            'hpp_amount'   => 0,
        ]);
        $channels = Menu::CHANNELS;
        $isEdit = false;

        return view('kasir.menu.form', compact('menu', 'channels', 'isEdit'));
    }

    /**
     * Simpan menu baru beserta harga multi-channel.
     */
    public function store(Request $request)
    {
        $validated = $request->validate([
            'name'        => 'required|string|max:150',
            'sku'         => 'required|string|max:50|unique:menus,sku',
            'category'    => 'required|string',
            'description' => 'nullable|string',
            'base_price'  => 'required|numeric|min:0',
            'hpp_amount'  => 'nullable|numeric|min:0',
            'image'       => 'nullable|image|max:3072',
            'prices'      => 'nullable|array',
        ]);

        $imageUrl = null;
        if ($request->hasFile('image')) {
            $file = $request->file('image');
            $filename = 'menu_' . Str::slug($validated['sku']) . '_' . time() . '.' . $file->getClientOriginalExtension();
            $uploadResult = $this->supabaseService->uploadBarcode($filename, file_get_contents($file->path()), $file->getMimeType());
            $imageUrl = $uploadResult['public_url'];
        }

        $menu = Menu::create([
            'sku'          => strtoupper(trim($validated['sku'])),
            'name'         => trim($validated['name']),
            'category'     => $validated['category'],
            'description'  => $validated['description'] ?? '',
            'base_price'   => (int) $validated['base_price'],
            'hpp_amount'   => (int) ($validated['hpp_amount'] ?? 0),
            'image_url'    => $imageUrl,
            'is_available' => $request->has('is_available'),
        ]);

        // Simpan harga multi-channel
        $channelPrices = $request->input('prices', []);
        if (empty($channelPrices['OFFLINE'])) {
            $channelPrices['OFFLINE'] = $menu->base_price;
        }
        $menu->syncChannelPrices($channelPrices);

        // Sinkronisasi ke Cloud Firestore
        $menu->load('channelPrices');
        $this->firestoreSync->syncMenu($menu);

        return redirect()->route('kasir.menu.index')
            ->with('success', "Menu '{$menu->name}' ({$menu->sku}) berhasil ditambahkan dengan harga multi-channel!");
    }

    /**
     * Form edit menu.
     */
    public function edit($id)
    {
        $menu = Menu::with('channelPrices')->findOrFail($id);
        $channels = Menu::CHANNELS;
        $isEdit = true;

        return view('kasir.menu.form', compact('menu', 'channels', 'isEdit'));
    }

    /**
     * Perbarui data menu dan harga multi-channel.
     */
    public function update(Request $request, $id)
    {
        $menu = Menu::with('channelPrices')->findOrFail($id);

        $validated = $request->validate([
            'name'        => 'required|string|max:150',
            'sku'         => 'required|string|max:50|unique:menus,sku,' . $menu->id,
            'category'    => 'required|string',
            'description' => 'nullable|string',
            'base_price'  => 'required|numeric|min:0',
            'hpp_amount'  => 'nullable|numeric|min:0',
            'image'       => 'nullable|image|max:3072',
            'prices'      => 'nullable|array',
        ]);

        $imageUrl = $menu->image_url;
        if ($request->hasFile('image')) {
            $file = $request->file('image');
            $filename = 'menu_' . Str::slug($validated['sku']) . '_' . time() . '.' . $file->getClientOriginalExtension();
            $uploadResult = $this->supabaseService->uploadBarcode($filename, file_get_contents($file->path()), $file->getMimeType());
            $imageUrl = $uploadResult['public_url'];
        }

        $menu->update([
            'sku'          => strtoupper(trim($validated['sku'])),
            'name'         => trim($validated['name']),
            'category'     => $validated['category'],
            'description'  => $validated['description'] ?? '',
            'base_price'   => (int) $validated['base_price'],
            'hpp_amount'   => (int) ($validated['hpp_amount'] ?? 0),
            'image_url'    => $imageUrl,
            'is_available' => $request->has('is_available'),
        ]);

        // Simpan harga multi-channel
        $channelPrices = $request->input('prices', []);
        if (empty($channelPrices['OFFLINE'])) {
            $channelPrices['OFFLINE'] = $menu->base_price;
        }
        $menu->syncChannelPrices($channelPrices);

        // Sinkronisasi ke Cloud Firestore
        $menu->load('channelPrices');
        $this->firestoreSync->syncMenu($menu);

        return redirect()->route('kasir.menu.index')
            ->with('success', "Menu '{$menu->name}' berhasil diperbarui!");
    }

    /**
     * Toggle ketersediaan menu (Tersedia / Habis).
     */
    public function toggleAvailability($id)
    {
        $menu = Menu::with('channelPrices')->findOrFail($id);
        $menu->is_available = !$menu->is_available;
        $menu->save();

        // Sinkronisasi status ke Firestore
        $this->firestoreSync->syncMenu($menu);

        $statusText = $menu->is_available ? 'Tersedia' : 'Habis / Non-Aktif';

        return back()->with('success', "Status menu '{$menu->name}' diubah menjadi: {$statusText}");
    }

    /**
     * Hapus menu.
     */
    public function destroy($id)
    {
        $menu = Menu::findOrFail($id);
        $nama = $menu->name;
        $menu->delete();

        return redirect()->route('kasir.menu.index')
            ->with('success', "Menu '{$nama}' berhasil dihapus dari katalog!");
    }
}
