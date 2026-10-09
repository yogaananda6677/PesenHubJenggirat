<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class SupabaseStorageService
{
    protected string $url;
    protected string $key;
    protected string $bucket;

    public function __construct()
    {
        $this->url    = rtrim(env('SUPABASE_URL', ''), '/');
        $this->key    = env('SUPABASE_KEY', '');
        $this->bucket = env('SUPABASE_BUCKET', 'order-barcodes');
    }

    /**
     * Check if valid Supabase credentials are set.
     */
    public function isConfigured(): bool
    {
        return !empty($this->url) &&
               !empty($this->key) &&
               !str_contains($this->url, 'xyzcompany.supabase.co') &&
               !str_contains($this->key, 'placeholder');
    }

    /**
     * Upload barcode image bytes to Supabase Storage.
     * Returns an array with public URL and upload status.
     */
    public function uploadBarcode(string $filename, string $fileBytes, string $mimeType = 'image/png'): array
    {
        $publicUrlFallback = asset('storage/barcodes/' . $filename);

        if (!$this->isConfigured()) {
            return [
                'success'          => true,
                'provider'         => 'Local (Supabase Ready)',
                'public_url'       => $publicUrlFallback,
                'supabase_path'    => "{$this->bucket}/{$filename}",
                'message'          => 'Supabase belum dikonfigurasi, menggunakan penyimpanan lokal yang valid.',
            ];
        }

        try {
            $endpoint = "{$this->url}/storage/v1/object/{$this->bucket}/{$filename}";

            $response = Http::withHeaders([
                'apikey'        => $this->key,
                'Authorization' => "Bearer {$this->key}",
                'x-upsert'      => 'true',
            ])->withBody($fileBytes, $mimeType)
              ->post($endpoint);

            if ($response->successful()) {
                $publicUrl = "{$this->url}/storage/v1/object/public/{$this->bucket}/{$filename}";

                return [
                    'success'       => true,
                    'provider'      => 'Supabase Storage',
                    'public_url'    => $publicUrl,
                    'supabase_path' => "{$this->bucket}/{$filename}",
                    'message'       => 'Berhasil diunggah ke Supabase Storage.',
                ];
            } else {
                Log::warning('Supabase upload response non-200: ' . $response->body());

                return [
                    'success'       => false,
                    'provider'      => 'Local Fallback',
                    'public_url'    => $publicUrlFallback,
                    'supabase_path' => "{$this->bucket}/{$filename}",
                    'message'       => 'Supabase Storage HTTP ' . $response->status() . ', fallback ke URL lokal.',
                ];
            }
        } catch (\Throwable $e) {
            Log::error('Supabase upload error: ' . $e->getMessage());

            return [
                'success'       => false,
                'provider'      => 'Local Fallback',
                'public_url'    => $publicUrlFallback,
                'supabase_path' => "{$this->bucket}/{$filename}",
                'message'       => 'Koneksi Supabase gagal: ' . $e->getMessage() . ', fallback ke URL lokal.',
            ];
        }
    }
}
