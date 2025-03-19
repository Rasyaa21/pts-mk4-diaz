package com.example.papipum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.papipum.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        configureGoogleSignIn()

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signInWithEmail(email, password)
        }

        binding.googleButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
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
            Toast.makeText(this, "Failed to load Google Sign-In configuration", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w(TAG, "Login failed", task.exception)
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Google Sign-In: ${e.message}")
            Toast.makeText(this, "Failed to start Google Sign-In", Toast.LENGTH_SHORT).show()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val data = result.data
            if (data == null) {
                Log.w(TAG, "Google sign in failed: No data returned")
                Toast.makeText(this, "Google login failed: No data returned", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "Google login canceled", Toast.LENGTH_SHORT).show()
                    }
                    CommonStatusCodes.NETWORK_ERROR -> {
                        Log.e(TAG, "Network error during Google sign in")
                        Toast.makeText(this, "Failed to connect to Google. Check your internet connection.", Toast.LENGTH_SHORT).show()
                    }
                    CommonStatusCodes.DEVELOPER_ERROR -> {
                        Log.e(TAG, "Developer error with Google Sign-In configuration")
                        Toast.makeText(this, "Google Sign-In configuration error", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.w(TAG, "Google sign in failed with code: ${e.statusCode}", e)
                        Toast.makeText(this, "Google login failed (code: ${e.statusCode})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in Google Sign-In result", e)
            Toast.makeText(this, "Unexpected error during Google login", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w(TAG, "Sign in with Google failed", task.exception)
                    Toast.makeText(this, "Google login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}