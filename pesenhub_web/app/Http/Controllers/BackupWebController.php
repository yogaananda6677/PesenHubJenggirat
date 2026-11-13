<?php

namespace App\Http\Controllers;

use App\Models\BackupOrder;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class BackupWebController extends Controller
{
    /**
     * Halaman Dashboard Server Backup MySQL & Penjelasan Coding Thread Coroutine (Modul 12).
     */
    public function index()
    {
        $backupOrders = BackupOrder::orderBy('id', 'desc')->paginate(15);
        $totalOrders = BackupOrder::count();
        $totalRevenue = BackupOrder::sum('total_price');
        $dbDriver = DB::getDefaultConnection();
        $dbName = DB::connection()->getDatabaseName();

        return view('backup.index', compact('backupOrders', 'totalOrders', 'totalRevenue', 'dbDriver', 'dbName'));
    }
}
