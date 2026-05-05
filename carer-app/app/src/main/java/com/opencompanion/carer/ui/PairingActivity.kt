package com.opencompanion.carer.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.opencompanion.carer.R
import com.opencompanion.carer.databinding.ActivityPairingBinding

class PairingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPairingBinding
    private val db = FirebaseDatabase.getInstance().reference
    private var code: String = ""
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureAuth { startPairing() }
    }

    private fun startPairing() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            code = (100000..999999).random().toString()
            val expiry = System.currentTimeMillis() + 10 * 60 * 1000  // 10 minutes

            db.child("pairing").child("codes").child(code).setValue(
                mapOf("carerFcmToken" to fcmToken, "expires" to expiry)
            )

            binding.codeText.text = code
            startCountdown(expiry)
            listenForCompanion(code, fcmToken)
        }
    }

    private fun listenForCompanion(code: String, carerFcmToken: String) {
        db.child("pairing").child("codes").child(code).child("companionFcmToken")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val companionToken = snapshot.getValue(String::class.java) ?: return
                    getSharedPreferences("carer_prefs", MODE_PRIVATE).edit()
                        .putString("companion_fcm_token", companionToken)
                        .apply()
                    db.child("pairing").child("codes").child(code).removeValue()
                    startActivity(Intent(this@PairingActivity, MainActivity::class.java))
                    finish()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun startCountdown(expiry: Long) {
        val remaining = expiry - System.currentTimeMillis()
        countDownTimer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(ms: Long) {
                binding.timerText.text = "${ms / 1000}s"
            }
            override fun onFinish() {
                db.child("pairing").child("codes").child(code).removeValue()
                binding.codeText.text = getString(R.string.code_expired)
                binding.timerText.text = ""
            }
        }.start()
    }

    private fun ensureAuth(onReady: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) { onReady(); return }
        auth.signInAnonymously().addOnSuccessListener { onReady() }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
