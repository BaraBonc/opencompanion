package com.opencompanion.companion.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseLogger {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private val deviceId: String
        get() = auth.currentUser?.uid ?: "unknown"

    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    init {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }

    fun logWakeWord() = log("wake_word", emptyMap())

    fun logCheckIn(completed: Boolean) = log("check_in", mapOf("completed" to completed))

    fun logEmergency(phrase: String) = log("emergency", mapOf("phrase" to phrase))

    fun logCarerInitiated() = log("carer_initiated", emptyMap())

    private fun log(type: String, details: Map<String, Any>) {
        val event = mutableMapOf<String, Any>(
            "type" to type,
            "timestamp" to System.currentTimeMillis()
        )
        event.putAll(details)
        db.child("activity").child(deviceId).child(today).push().setValue(event)
    }
}
