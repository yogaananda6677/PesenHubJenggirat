package com.pesenhub.jenggirat

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.pesenhub.jenggirat.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var b: ActivityLoginBinding
    lateinit var auth: FirebaseAuth
    lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        sharedPref = getSharedPreferences("LoginPref", Context.MODE_PRIVATE)

        b.btnLogin.setOnClickListener(this)
        b.txDaftarBaru.setOnClickListener(this)

        // Baca SharedPreferences (Bab 14 PM): Apakah username disimpan sebelumnya
        bacaSharedPreferences()
    }

    private fun bacaSharedPreferences() {
        val ingat = sharedPref.getBoolean("ingat_username", false)
        if (ingat) {
            val usernameTersimpan = sharedPref.getString("username", "")
            b.edUsername.setText(usernameTersimpan)
            b.cbIngatUsername.isChecked = true
        } else {
            b.cbIngatUsername.isChecked = false
        }
    }

    private fun simpanSharedPreferences(username: String, ingat: Boolean) {
        with(sharedPref.edit()) {
            putBoolean("ingat_username", ingat)
            if (ingat) {
                putString("username", username)
            } else {
                remove("username")
            }
            apply()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            b.btnLogin.id -> {
                val email = b.edUsername.text.toString().trim()
                val password = b.edPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Email dan password wajib diisi", Toast.LENGTH_SHORT).show()
                    return
                }

                // Simpan username ke SharedPreferences sesuai centang CheckBox
                simpanSharedPreferences(email, b.cbIngatUsername.isChecked)

                // Firebase Authentication (Sesuai Pertemuan 04 PML)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid ?: ""
                            val dbFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                            dbFirestore.collection("users").document(uid).get()
                                .addOnSuccessListener { doc ->
                                    val role = doc.getString("role") ?: "kasir"
                                    val nama = doc.getString("nama") ?: email
                                    val outlet = doc.getString("namaOutlet") ?: "PesenHub Jenggirat"

                                    val userSession = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                                    userSession.edit()
                                        .putString("uid", uid)
                                        .putString("nama", nama)
                                        .putString("email", email)
                                        .putString("role", role)
                                        .putString("namaOutlet", outlet)
                                        .apply()

                                    Toast.makeText(this, "Login berhasil sebagai ${role.uppercase()}! Halo $nama", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Login berhasil! Selamat datang $email", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                        } else {
                            Toast.makeText(this, "Gagal login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
            b.txDaftarBaru.id -> {
                // Navigasi ke Halaman Pendaftaran Akun (Admin Outlet & Kasir)
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
