package com.pesenhub.jenggirat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pesenhub.jenggirat.databinding.FragmentAkunBinding

class AkunFragment : Fragment() {

    private var _binding: FragmentAkunBinding? = null
    private val b get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val dbFirestore = FirebaseFirestore.getInstance()

    private var uid: String = ""
    private var role: String = "kasir"
    private var email: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAkunBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        muatDataSesi()
        ambilDataDariFirestore()

        b.btnSimpanBiodata.setOnClickListener {
            simpanPerubahanBiodata()
        }

        b.btnLogoutAkun.setOnClickListener {
            tampilkanDialogLogout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun muatDataSesi() {
        val pref = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        uid = pref.getString("uid", "") ?: auth.currentUser?.uid ?: ""
        val nama = pref.getString("nama", "") ?: ""
        email = pref.getString("email", "") ?: auth.currentUser?.email ?: ""
        role = pref.getString("role", "kasir") ?: "kasir"
        val outlet = pref.getString("namaOutlet", "Jenggirat Kediri") ?: "Jenggirat Kediri"
        val phone = pref.getString("phone", "") ?: ""
        val alamat = pref.getString("alamat", "") ?: ""
        val bio = pref.getString("bio", "") ?: ""

        tampilkanKeView(nama, email, role, outlet, phone, alamat, bio)
    }

    private fun ambilDataDariFirestore() {
        val currentUid = auth.currentUser?.uid ?: uid
        if (currentUid.isEmpty()) return

        dbFirestore.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded || doc == null || !doc.exists()) return@addOnSuccessListener

                val nama = doc.getString("nama") ?: ""
                val docRole = doc.getString("role") ?: role
                val docEmail = doc.getString("email") ?: email
                val docOutlet = doc.getString("namaOutlet") ?: "Jenggirat Kediri"
                val phone = doc.getString("phone") ?: ""
                val alamat = doc.getString("alamat") ?: ""
                val bio = doc.getString("bio") ?: ""

                role = docRole
                email = docEmail

                // Simpan ke SharedPreferences
                val pref = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                pref.edit()
                    .putString("uid", currentUid)
                    .putString("nama", nama)
                    .putString("email", docEmail)
                    .putString("role", docRole)
                    .putString("namaOutlet", docOutlet)
                    .putString("phone", phone)
                    .putString("alamat", alamat)
                    .putString("bio", bio)
                    .apply()

                tampilkanKeView(nama, docEmail, docRole, docOutlet, phone, alamat, bio)
            }
    }

    private fun tampilkanKeView(
        nama: String,
        email: String,
        role: String,
        outlet: String,
        phone: String,
        alamat: String,
        bio: String
    ) {
        val displayNama = nama.ifEmpty { email.substringBefore("@") }
        b.txHeaderNama.text = displayNama
        b.txHeaderEmail.text = email
        b.txHeaderOutlet.text = outlet
        b.txHeaderRole.text = if (role == "admin") "ADMIN OUTLET" else "KASIR OUTLET"
        b.txRoleDetail.text = role.uppercase()

        val inisial = if (displayNama.isNotEmpty()) displayNama.take(1).uppercase() else "U"
        b.txAvatarInisial.text = inisial

        // Jangan timpa input jika pengguna sedang mengetik (hanya set jika kosong)
        if (b.edNamaProfil.text.isNullOrEmpty()) b.edNamaProfil.setText(displayNama)
        if (b.edPhoneProfil.text.isNullOrEmpty()) b.edPhoneProfil.setText(phone)
        if (b.edAlamatProfil.text.isNullOrEmpty()) b.edAlamatProfil.setText(alamat)
        if (b.edBioProfil.text.isNullOrEmpty()) b.edBioProfil.setText(bio)
    }

    private fun simpanPerubahanBiodata() {
        val namaBaru = b.edNamaProfil.text.toString().trim()
        val phoneBaru = b.edPhoneProfil.text.toString().trim()
        val alamatBaru = b.edAlamatProfil.text.toString().trim()
        val bioBaru = b.edBioProfil.text.toString().trim()

        if (namaBaru.isEmpty()) {
            b.edNamaProfil.error = "Nama lengkap tidak boleh kosong"
            b.edNamaProfil.requestFocus()
            return
        }

        val currentUid = auth.currentUser?.uid ?: uid
        if (currentUid.isEmpty()) {
            Toast.makeText(requireContext(), "Sesi akun tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnSimpanBiodata.isEnabled = false
        b.btnSimpanBiodata.text = "Menyimpan Biodata..."

        val updateData = hashMapOf<String, Any>(
            "nama" to namaBaru,
            "phone" to phoneBaru,
            "alamat" to alamatBaru,
            "bio" to bioBaru,
            "updatedAt" to Timestamp.now()
        )

        dbFirestore.collection("users").document(currentUid)
            .update(updateData)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                b.btnSimpanBiodata.isEnabled = true
                b.btnSimpanBiodata.text = "Simpan Perubahan Biodata"

                // Update SharedPreferences lokal
                val pref = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                pref.edit()
                    .putString("nama", namaBaru)
                    .putString("phone", phoneBaru)
                    .putString("alamat", alamatBaru)
                    .putString("bio", bioBaru)
                    .apply()

                // Update tampilan profil atas
                b.txHeaderNama.text = namaBaru
                b.txAvatarInisial.text = namaBaru.take(1).uppercase()

                // Sinkronkan subtitle toolbar MainActivity
                (activity as? MainActivity)?.perbaruiToolbarSubtitle()

                Toast.makeText(requireContext(), "Biodata diri akun berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                b.btnSimpanBiodata.isEnabled = true
                b.btnSimpanBiodata.text = "Simpan Perubahan Biodata"
                Toast.makeText(requireContext(), "Gagal menyimpan biodata: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun tampilkanDialogLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                val pref = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                pref.edit().clear().apply()
                auth.signOut()

                Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()

                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                activity?.finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
