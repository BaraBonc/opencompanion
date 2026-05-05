package com.opencompanion.carer.firebase

import android.content.Context
import com.google.firebase.database.FirebaseDatabase

object RemoteCheckIn {

    fun trigger(context: Context) {
        val companionToken = context.getSharedPreferences("carer_prefs", Context.MODE_PRIVATE)
            .getString("companion_fcm_token", null) ?: return

        // Write a trigger message to Firebase that the companion app listens for.
        // The companion app's CompanionMessagingService handles delivery.
        val db = FirebaseDatabase.getInstance().reference
        db.child("triggers").child(companionToken.take(20)).setValue(
            mapOf(
                "type" to "carer_initiated",
                "greeting" to "Zoli szeretne beszélni veled",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}
