package com.pesenhub.jenggirat

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object SupabaseStorageHelper {
    const val SUPABASE_URL = "https://ckgymiinffzfyfbiiyob.supabase.co"
    const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNrZ3ltaWluZmZ6ZnlmYmlpeW9iIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEyMDM3MDEsImV4cCI6MjEwNjc3OTcwMX0.uiCTFbIJkFk2H_urShILpXJbptKexztW0MT4yWoGviA"
    const val BUCKET_NAME = "Storage"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Upload JPEG image bytes ke Supabase Storage (Bucket: Storage, folder: menu-images).
     * Mengembalikan URL publik Supabase yang dapat langsung diakses oleh Android dan Web.
     */
    fun uploadMenuImage(
        filename: String,
        imageBytes: ByteArray,
        onComplete: (success: Boolean, publicUrl: String?, errorMessage: String?) -> Unit
    ) {
        executor.execute {
            try {
                val cleanFilename = filename.replace(" ", "_")
                val uploadEndpoint = "$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/menu-images/$cleanFilename"
                val url = URL(uploadEndpoint)
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("apikey", SUPABASE_KEY)
                conn.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                conn.setRequestProperty("Content-Type", "image/jpeg")
                conn.setRequestProperty("x-upsert", "true")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val os = DataOutputStream(conn.outputStream)
                os.write(imageBytes)
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val publicUrl = "$SUPABASE_URL/storage/v1/object/public/$BUCKET_NAME/menu-images/$cleanFilename"
                    Log.d("SupabaseHelper", "Upload sukses: $publicUrl")
                    mainHandler.post {
                        onComplete(true, publicUrl, null)
                    }
                } else {
                    val errorStream = conn.errorStream ?: conn.inputStream
                    val errorMsg = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    Log.e("SupabaseHelper", "Upload gagal: HTTP $responseCode - $errorMsg")
                    mainHandler.post {
                        onComplete(false, null, "Supabase HTTP $responseCode: $errorMsg")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("SupabaseHelper", "Exception upload Supabase", e)
                mainHandler.post {
                    onComplete(false, null, e.message ?: "Koneksi Supabase gagal")
                }
            }
        }
    }
}
