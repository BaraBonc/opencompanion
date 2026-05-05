package com.opencompanion.companion.firebase

import android.speech.tts.TextToSpeech
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.opencompanion.companion.scheduler.ProactiveCheckInScheduler
import java.util.Locale

class CompanionMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "carer_initiated" -> handleCarerInitiated(message.data["greeting"])
        }
    }

    private fun handleCarerInitiated(greeting: String?) {
        val text = greeting ?: "Zoli szeretne beszélni veled"
        ProactiveCheckInScheduler.triggerCheckIn(this, text)
    }

    override fun onNewToken(token: String) {
        // Store updated FCM token in Firebase so carer app can reach this device
        val db = com.google.firebase.database.FirebaseDatabase.getInstance().reference
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.currentUser?.uid?.let { uid ->
            db.child("pairing").child(uid).child("companionFcmToken").setValue(token)
        }
    }
}
