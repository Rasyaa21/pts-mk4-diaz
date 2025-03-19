package com.example.papipum

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.papipum.databinding.ActivityRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        configureGoogleSignIn()

        binding.loginButton.setOnClickListener {
            val password = binding.passwordInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
                signUpWithEmail(email, password)
        }

        binding.googleButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun configureGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Google Sign-In: ${e.message}")
            Toast.makeText(this, "Gagal memuat konfigurasi Google Sign-In", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Log.d(TAG, "Email verifikasi telah dikirim ke $email")

                            Toast.makeText(
                                this,
                                "Registrasi berhasil! Silakan cek email untuk verifikasi.",
                                Toast.LENGTH_LONG
                            ).show()

                            auth.signOut()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Log.e(TAG, "Gagal mengirim email verifikasi", verificationTask.exception)
                            Toast.makeText(
                                this,
                                "Gagal mengirim email verifikasi.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Log.w(TAG, "Registrasi gagal", task.exception)
                    Toast.makeText(this, "Registrasi gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Fungsi Sign-In dengan Google
    private fun signInWithGoogle() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Google Sign-In: ${e.message}")
            Toast.makeText(this, "Gagal memulai Google Sign-In", Toast.LENGTH_SHORT).show()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val data = result.data
            if (data == null) {
                Log.w(TAG, "Google sign in failed: No data returned")
                Toast.makeText(this, "Login Google gagal: Tidak ada data yang dikembalikan", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                when (e.statusCode) {
                    CommonStatusCodes.CANCELED -> {
                        Log.d(TAG, "Google sign in was canceled by user")
                        Toast.makeText(this, "Login Google dibatalkan", Toast.LENGTH_SHORT).show()
                    }
                    CommonStatusCodes.NETWORK_ERROR -> {
                        Log.e(TAG, "Network error during Google sign in")
                        Toast.makeText(this, "Gagal terhubung ke Google. Periksa koneksi internet Anda.", Toast.LENGTH_SHORT).show()
                    }
                    CommonStatusCodes.DEVELOPER_ERROR -> {
                        Log.e(TAG, "Developer error with Google Sign-In configuration")
                        Toast.makeText(this, "Terjadi kesalahan konfigurasi Google Sign-In", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.w(TAG, "Google sign in failed with code: ${e.statusCode}", e)
                        Toast.makeText(this, "Login Google gagal (kode: ${e.statusCode})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in Google Sign-In result", e)
            Toast.makeText(this, "Terjadi kesalahan tak terduga saat login Google", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                    }
                    Toast.makeText(this, "Login dengan Google berhasil!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w(TAG, "Sign in with Google failed", task.exception)
                    Toast.makeText(this, "Login Google gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

}