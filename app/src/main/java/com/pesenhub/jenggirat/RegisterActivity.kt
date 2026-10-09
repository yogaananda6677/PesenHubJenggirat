package com.pesenhub.jenggirat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pesenhub.jenggirat.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        dbFirestore = FirebaseFirestore.getInstance()


        // Toggle Tipe Akun (Admin vs Kasir)
        b.rgTipeAkun.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == b.rbAdmin.id) {
                b.layoutAdminOutlet.visibility = View.VISIBLE
                b.layoutKasirUndangan.visibility = View.GONE
                b.btnDaftar.text = "DAFTAR SEBAGAI ADMIN"
            } else {
                b.layoutAdminOutlet.visibility = View.GONE
                b.layoutKasirUndangan.visibility = View.VISIBLE
                b.btnDaftar.text = "DAFTAR SEBAGAI KASIR"
            }
        }

        b.btnDaftar.setOnClickListener {
            if (b.rbAdmin.isChecked) {
                prosesDaftarAdmin()
            } else {
                prosesDaftarKasir()
            }
        }

        b.txKembaliLogin.setOnClickListener {
            finish()
        }
    }

    private fun setSedangMemuat(loading: Boolean) {
        b.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnDaftar.isEnabled = !loading
    }

    private fun prosesDaftarAdmin() {
        val nama = b.edNama.text.toString().trim()
        val namaOutlet = b.edNamaOutlet.text.toString().trim()
        val email = b.edEmail.text.toString().trim()
        val password = b.edPassword.text.toString().trim()

        if (nama.isEmpty()) {
            b.edNama.error = "Nama wajib diisi"
            return
        }
        if (namaOutlet.isEmpty()) {
            b.edNamaOutlet.error = "Nama outlet wajib diisi"
            return
        }
        if (email.isEmpty()) {
            b.edEmail.error = "Email wajib diisi"
            return
        }
        if (password.length < 6) {
            b.edPassword.error = "Password minimal 6 karakter"
            return
        }

        setSedangMemuat(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                val adminData = hashMapOf(
                    "uid" to uid,
                    "nama" to nama,
                    "email" to email,
                    "role" to "admin",
                    "namaOutlet" to namaOutlet,
                    "createdAt" to Timestamp.now()
                )

                // Simpan profil admin ke koleksi Firestore users
                dbFirestore.collection("users").document(uid).set(adminData)
                    .addOnSuccessListener {
                        simpanSessionLocal(uid, nama, email, "admin", namaOutlet)
                        setSedangMemuat(false)
                        Toast.makeText(this, "Registrasi Admin Outlet berhasil!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setSedangMemuat(false)
                        Toast.makeText(this, "Gagal menyimpan data admin: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    // Akun sudah dibuat di Auth (misal saat percobaan sebelum rules di-update)
                    // Coba sign in untuk menyimpan data profil Firestore
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: ""
                            val adminData = hashMapOf(
                                "uid" to uid,
                                "nama" to nama,
                                "email" to email,
                                "role" to "admin",
                                "namaOutlet" to namaOutlet,
                                "createdAt" to Timestamp.now()
                            )
                            dbFirestore.collection("users").document(uid).set(adminData)
                                .addOnSuccessListener {
                                    simpanSessionLocal(uid, nama, email, "admin", namaOutlet)
                                    setSedangMemuat(false)
                                    Toast.makeText(this, "Profil Admin berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { err ->
                                    setSedangMemuat(false)
                                    Toast.makeText(this, "Gagal melengkapi data admin: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            setSedangMemuat(false)
                            Toast.makeText(this, "Email sudah terdaftar. Masukkan password yang sesuai atau lakukan login.", Toast.LENGTH_LONG).show()
                        }
                } else {
                    setSedangMemuat(false)
                    Toast.makeText(this, "Gagal membuat akun admin: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun prosesDaftarKasir() {
        val nama = b.edNama.text.toString().trim()
        val email = b.edEmail.text.toString().trim()
        val password = b.edPassword.text.toString().trim()
        val kodeUndangan = b.edKodeUndangan.text.toString().trim().uppercase()

        if (nama.isEmpty()) {
            b.edNama.error = "Nama wajib diisi"
            return
        }
        if (kodeUndangan.isEmpty()) {
            b.edKodeUndangan.error = "Kode undangan dari Admin wajib diisi"
            return
        }
        if (email.isEmpty()) {
            b.edEmail.error = "Email wajib diisi"
            return
        }
        if (password.length < 6) {
            b.edPassword.error = "Password minimal 6 karakter"
            return
        }

        setSedangMemuat(true)

        // Validasi kode undangan di Firestore invitations
        dbFirestore.collection("invitations")
            .whereEqualTo("kodeUndangan", kodeUndangan)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    setSedangMemuat(false)
                    Toast.makeText(
                        this,
                        "Kode undangan tidak ditemukan! Minta kode undangan resmi dari Admin Outlet Anda.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val inviteDoc = snapshot.documents[0]
                val status = inviteDoc.getString("status") ?: ""
                val invitedEmail = inviteDoc.getString("emailKasir") ?: ""
                val namaOutlet = inviteDoc.getString("namaOutlet") ?: "PesenHub Jenggirat"
                val adminEmail = inviteDoc.getString("adminEmail") ?: ""

                if (status != "PENDING") {
                    setSedangMemuat(false)
                    Toast.makeText(
                        this,
                        "Kode undangan ini sudah pernah digunakan atau tidak aktif lagi!",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                if (invitedEmail.isNotEmpty() && !invitedEmail.equals(email, ignoreCase = true)) {
                    setSedangMemuat(false)
                    Toast.makeText(
                        this,
                        "Email pendaftaran ($email) tidak cocok dengan email undangan ($invitedEmail)!",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                // Kode valid -> Buat akun di Firebase Auth
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: ""

                        // Update status undangan menjadi USED
                        inviteDoc.reference.update(
                            mapOf(
                                "status" to "USED",
                                "usedByUid" to uid,
                                "usedAt" to Timestamp.now()
                            )
                        )

                        // Simpan profil kasir ke koleksi Firestore users
                        val kasirData = hashMapOf(
                            "uid" to uid,
                            "nama" to nama,
                            "email" to email,
                            "role" to "kasir",
                            "namaOutlet" to namaOutlet,
                            "invitedBy" to adminEmail,
                            "kodeUndangan" to kodeUndangan,
                            "createdAt" to Timestamp.now()
                        )

                        dbFirestore.collection("users").document(uid).set(kasirData)
                            .addOnSuccessListener {
                                simpanSessionLocal(uid, nama, email, "kasir", namaOutlet)
                                setSedangMemuat(false)
                                Toast.makeText(this, "Registrasi Kasir berhasil via Undangan Admin!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                setSedangMemuat(false)
                                Toast.makeText(this, "Gagal menyimpan data kasir: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        setSedangMemuat(false)
                        Toast.makeText(this, "Gagal membuat akun kasir: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                setSedangMemuat(false)
                Toast.makeText(this, "Gagal memverifikasi kode undangan: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun simpanSessionLocal(uid: String, nama: String, email: String, role: String, outlet: String) {
        val pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        pref.edit()
            .putString("uid", uid)
            .putString("nama", nama)
            .putString("email", email)
            .putString("role", role)
            .putString("namaOutlet", outlet)
            .apply()
    }
}
