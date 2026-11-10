<?php

namespace App\Support;

class OrderCrypto
{
    public const PREFIX = 'ENC1:';

    /**
     * Field yang wajib dienkripsi secara terpisah.
     */
    public const ENCRYPTED_FIELDS = [
        'customerName',
        'customerPhone',
        'status',
        'source',
        'paymentMethod',
        'paymentStatus',
        'pickupTime',
        'notes',
        'total',
        'totalPrice',
        'totalItems',
        'menuItem',
        'detailItem',
        'items',
        'branchName',
        'barcodeUrl',
        'barcode_url',
    ];

    /**
     * Field bertipe numerik yang harus dikonversi ke string saat enkripsi
     * dan dikembalikan ke int saat dekripsi.
     */
    public const NUMERIC_FIELDS = [
        'total',
        'totalPrice',
        'totalItems',
    ];

    /**
     * Dapatkan kunci rahasia AES 32-byte.
     */
    public static function getSecretKey(?string $keyOverride = null): string
    {
        $key = $keyOverride ?: config('services.aes_key');

        if (empty($key)) {
            throw new \RuntimeException('AES_SECRET_KEY belum dikonfigurasi di environment atau config services.php');
        }

        if (strlen($key) !== 32) {
            throw new \RuntimeException('AES_SECRET_KEY harus memiliki panjang tepat 32 karakter (256-bit)');
        }

        return $key;
    }

    /**
     * Enkripsi satu nilai menggunakan AES-256-CBC.
     */
    public static function encryptValue(mixed $value, ?string $keyOverride = null): mixed
    {
        if ($value === null || is_bool($value)) {
            return $value;
        }

        // Jangan enkripsi ulang jika sudah terenkripsi
        if (is_string($value) && str_starts_with($value, self::PREFIX)) {
            return $value;
        }

        $plainText = is_array($value)
            ? json_encode($value, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE)
            : (string) $value;

        try {
            $key = self::getSecretKey($keyOverride);
            $iv = random_bytes(16);
            $cipherRaw = openssl_encrypt($plainText, 'aes-256-cbc', $key, OPENSSL_RAW_DATA, $iv);

            if ($cipherRaw === false) {
                return $value;
            }

            return self::PREFIX . base64_encode($iv . $cipherRaw);
        } catch (\Throwable) {
            // Jangan crash dan jangan log kunci / plaintext
            return $value;
        }
    }

    /**
     * Dekripsi satu nilai ciphertext yang diawali ENC1:.
     */
    public static function decryptValue(mixed $value, ?string $keyOverride = null): mixed
    {
        if (!is_string($value) || !str_starts_with($value, self::PREFIX)) {
            return $value;
        }

        try {
            $key = self::getSecretKey($keyOverride);
            $raw = base64_decode(substr($value, strlen(self::PREFIX)), true);

            if ($raw === false || strlen($raw) < 16) {
                return $value;
            }

            $iv = substr($raw, 0, 16);
            $cipherRaw = substr($raw, 16);

            $decrypted = openssl_decrypt($cipherRaw, 'aes-256-cbc', $key, OPENSSL_RAW_DATA, $iv);

            if ($decrypted === false) {
                return $value;
            }

            return $decrypted;
        } catch (\Throwable) {
            // Jika dekripsi gagal (kunci salah / data rusak), kembalikan apa adanya dan jangan crash
            return $value;
        }
    }

    /**
     * Enkripsi field-field yang berhak dalam array data dokumen orders.
     */
    public static function encryptFields(array $data, ?string $keyOverride = null): array
    {
        $encryptedFieldsFlip = array_flip(self::ENCRYPTED_FIELDS);
        $numericFieldsFlip = array_flip(self::NUMERIC_FIELDS);

        $result = $data;

        foreach ($data as $k => $v) {
            if (!isset($encryptedFieldsFlip[$k])) {
                continue;
            }

            if ($v === null || is_bool($v)) {
                continue;
            }

            if ($k === 'items') {
                if (is_array($v)) {
                    $json = json_encode($v, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
                    $result[$k] = self::encryptValue($json, $keyOverride);
                } else {
                    $result[$k] = self::encryptValue($v, $keyOverride);
                }
            } elseif (isset($numericFieldsFlip[$k])) {
                $result[$k] = self::encryptValue((string) $v, $keyOverride);
            } else {
                $result[$k] = self::encryptValue((string) $v, $keyOverride);
            }
        }

        return $result;
    }

    /**
     * Dekripsi field-field dalam array data dokumen orders dan kembalikan ke tipe aslinya.
     */
    public static function decryptFields(array $data, ?string $keyOverride = null): array
    {
        $encryptedFieldsFlip = array_flip(self::ENCRYPTED_FIELDS);
        $numericFieldsFlip = array_flip(self::NUMERIC_FIELDS);

        $result = $data;

        foreach ($data as $k => $v) {
            if (!isset($encryptedFieldsFlip[$k])) {
                continue;
            }

            if (!is_string($v) || !str_starts_with($v, self::PREFIX)) {
                continue;
            }

            $decrypted = self::decryptValue($v, $keyOverride);

            if ($decrypted === $v) {
                // Dekripsi tidak mengubah atau gagal, pertahankan nilai
                continue;
            }

            if ($k === 'items') {
                if (is_string($decrypted) && (str_starts_with($decrypted, '[') || str_starts_with($decrypted, '{'))) {
                    $decoded = json_decode($decrypted, true);
                    $result[$k] = is_array($decoded) ? $decoded : $decrypted;
                } else {
                    $result[$k] = $decrypted;
                }
            } elseif (isset($numericFieldsFlip[$k])) {
                if (is_numeric($decrypted)) {
                    $result[$k] = (int) $decrypted;
                } else {
                    $result[$k] = $decrypted;
                }
            } else {
                $result[$k] = $decrypted;
            }
        }

        return $result;
    }
}
