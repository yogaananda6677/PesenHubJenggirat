<?php

namespace App\Services;

use chillerlan\QRCode\Output\QRGdImagePNG;
use chillerlan\QRCode\QRCode;
use chillerlan\QRCode\QROptions;
use Illuminate\Support\Facades\Storage;

class BarcodeGeneratorService
{
    /**
     * Generate QR Code binary image (PNG format) for an order number.
     */
    public function generatePngBytes(string $orderNumber): string
    {
        $options = new QROptions([
            'outputInterface'     => QRGdImagePNG::class,
            'outputBase64'        => false,
            'scale'               => 10,
            'imageTransparent'    => false,
            'drawCircularModules' => false,
        ]);

        $qrcode = new QRCode($options);
        return $qrcode->render($orderNumber);
    }

    /**
     * Generate QR Code as Data URI for immediate display on web.
     */
    public function generateDataUri(string $orderNumber): string
    {
        $options = new QROptions([
            'outputInterface'  => QRGdImagePNG::class,
            'outputBase64'     => true,
            'scale'            => 8,
            'imageTransparent' => false,
        ]);

        $qrcode = new QRCode($options);
        return $qrcode->render($orderNumber);
    }

    /**
     * Save QR Code to local public storage and return the public URL.
     */
    public function saveLocally(string $orderNumber): array
    {
        $filename = 'barcode_' . preg_replace('/[^A-Za-z0-9_\-]/', '_', $orderNumber) . '.png';
        $path = 'public/barcodes/' . $filename;

        $pngBytes = $this->generatePngBytes($orderNumber);
        Storage::disk('local')->put($path, $pngBytes);

        // Also ensure public storage directory exists and file is copied for web access
        $publicDir = public_path('storage/barcodes');
        if (!file_exists($publicDir)) {
            mkdir($publicDir, 0755, true);
        }
        file_put_contents($publicDir . '/' . $filename, $pngBytes);

        return [
            'filename' => $filename,
            'bytes'    => $pngBytes,
            'local_url' => asset('storage/barcodes/' . $filename),
        ];
    }
}
