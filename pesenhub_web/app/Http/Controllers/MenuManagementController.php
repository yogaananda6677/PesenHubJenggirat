<?php

namespace App\Http\Controllers;

use App\DTO\MenuDto;
use App\Models\Menu;
use App\Services\FirestoreService;
use App\Services\SupabaseStorageService;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class MenuManagementController extends Controller
{
    protected SupabaseStorageService $supabaseService;
    protected FirestoreService $firestore;

    public function __construct(
        SupabaseStorageService $supabaseService,
        FirestoreService $firestore
    ) {
        $this->supabaseService = $supabaseService;
        $this->firestore       = $firestore;
    }

    /**
     * Tampilkan daftar kelola menu dari Cloud Firestore.
     */
    public function index(Request $request)
    {
        $kategoriAktif = $request->query('kategori', 'Semua');
        $keyword = $request->query('q', '');

        $rawMenus = $this->firestore->getMenus($kategoriAktif, $keyword);
        $menus = collect($rawMenus)->map(fn($m) => new MenuDto($m));
        $channels = Menu::CHANNELS;

        return view('kasir.menu.index', compact('menus', 'kategoriAktif', 'keyword', 'channels'));
    }

    /**
     * Form tambah menu baru.
     */
    public function create()
    {
        $menu = new MenuDto([
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
     * Simpan menu baru langsung ke Cloud Firestore.
     */
    public function store(Request $request)
    {
        $validated = $request->validate([
            'name'        => 'required|string|max:150',
            'sku'         => 'required|string|max:50',
            'category'    => 'required|string',
            'description' => 'nullable|string',
            'base_price'  => 'required|numeric|min:0',
            'hpp_amount'  => 'nullable|numeric|min:0',
            'image'       => 'nullable|image|max:3072',
            'prices'      => 'nullable|array',
        ]);

        $sku = strtoupper(trim($validated['sku']));

        $imageUrl = 'default_food_icon';
        if ($request->hasFile('image')) {
            $file = $request->file('image');
            $filename = 'menu_' . Str::slug($sku) . '_' . time() . '.' . $file->getClientOriginalExtension();
            $uploadResult = $this->supabaseService->uploadBarcode($filename, file_get_contents($file->path()), $file->getMimeType());
            $imageUrl = $uploadResult['public_url'];
        }

        $channelPrices = $request->input('prices', []);
        if (empty($channelPrices['OFFLINE'])) {
            $channelPrices['OFFLINE'] = (int) $validated['base_price'];
        }

        $menuData = [
            'sku'          => $sku,
            'name'         => trim($validated['name']),
            'category'     => $validated['category'],
            'description'  => $validated['description'] ?? '',
            'base_price'   => (int) $validated['base_price'],
            'price'        => (int) $validated['base_price'],
            'hpp_amount'   => (int) ($validated['hpp_amount'] ?? 0),
            'image_url'    => $imageUrl,
            'imageUrl'     => $imageUrl,
            'is_available' => $request->has('is_available'),
            'available'    => $request->has('is_available'),
            'prices'       => $channelPrices,
            'channelPrices'=> $channelPrices,
        ];

        $this->firestore->saveMenu($menuData);

        return redirect()->route('kasir.menu.index')
            ->with('success', "Menu '{$validated['name']}' ({$sku}) berhasil disimpan di Cloud Firestore!");
    }

    /**
     * Form edit menu.
     */
    public function edit($id)
    {
        $rawMenu = $this->firestore->getMenu($id);
        if (!$rawMenu) {
            return redirect()->route('kasir.menu.index')->with('error', "Menu '{$id}' tidak ditemukan di Cloud Firestore.");
        }

        $menu = new MenuDto($rawMenu);
        $channels = Menu::CHANNELS;
        $isEdit = true;

        return view('kasir.menu.form', compact('menu', 'channels', 'isEdit'));
    }

    /**
     * Perbarui data menu di Cloud Firestore.
     */
    public function update(Request $request, $id)
    {
        $rawMenu = $this->firestore->getMenu($id);
        if (!$rawMenu) {
            return redirect()->route('kasir.menu.index')->with('error', "Menu '{$id}' tidak ditemukan di Cloud Firestore.");
        }

        $validated = $request->validate([
            'name'        => 'required|string|max:150',
            'sku'         => 'required|string|max:50',
            'category'    => 'required|string',
            'description' => 'nullable|string',
            'base_price'  => 'required|numeric|min:0',
            'hpp_amount'  => 'nullable|numeric|min:0',
            'image'       => 'nullable|image|max:3072',
            'prices'      => 'nullable|array',
        ]);

        $imageUrl = $rawMenu['imageUrl'] ?? $rawMenu['image_url'] ?? 'default_food_icon';
        if ($request->hasFile('image')) {
            $file = $request->file('image');
            $filename = 'menu_' . Str::slug($validated['sku']) . '_' . time() . '.' . $file->getClientOriginalExtension();
            $uploadResult = $this->supabaseService->uploadBarcode($filename, file_get_contents($file->path()), $file->getMimeType());
            $imageUrl = $uploadResult['public_url'];
        }

        $channelPrices = $request->input('prices', []);
        if (empty($channelPrices['OFFLINE'])) {
            $channelPrices['OFFLINE'] = (int) $validated['base_price'];
        }

        $menuData = [
            'sku'          => strtoupper(trim($validated['sku'])),
            'name'         => trim($validated['name']),
            'category'     => $validated['category'],
            'description'  => $validated['description'] ?? '',
            'base_price'   => (int) $validated['base_price'],
            'price'        => (int) $validated['base_price'],
            'hpp_amount'   => (int) ($validated['hpp_amount'] ?? 0),
            'image_url'    => $imageUrl,
            'imageUrl'     => $imageUrl,
            'is_available' => $request->has('is_available'),
            'available'    => $request->has('is_available'),
            'prices'       => $channelPrices,
            'channelPrices'=> $channelPrices,
        ];

        $this->firestore->saveMenu($menuData);

        return redirect()->route('kasir.menu.index')
            ->with('success', "Menu '{$validated['name']}' berhasil diperbarui di Cloud Firestore!");
    }

    /**
     * Toggle ketersediaan menu (Tersedia / Habis) di Cloud Firestore.
     */
    public function toggleAvailability($id)
    {
        $success = $this->firestore->toggleMenuAvailability($id);
        if ($success) {
            return back()->with('success', "Status ketersediaan menu '{$id}' berhasil diubah di Cloud Firestore!");
        }

        return back()->with('error', "Gagal mengubah status menu '{$id}' di Cloud Firestore.");
    }

    /**
     * Hapus menu dari Cloud Firestore.
     */
    public function destroy($id)
    {
        $this->firestore->deleteMenu($id);

        return redirect()->route('kasir.menu.index')
            ->with('success', "Menu '{$id}' berhasil dihapus dari Cloud Firestore!");
    }
}
