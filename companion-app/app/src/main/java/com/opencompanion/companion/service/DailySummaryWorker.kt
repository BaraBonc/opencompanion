package com.opencompanion.companion.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DailySummaryWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
        val db = FirebaseDatabase.getInstance().reference
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val snapshot = db.child("activity").child(uid).child(today).get().await()
        val events = snapshot.children.mapNotNull { it.getValue<Map<String, Any>>() }

        val wakeCount = events.count { it["type"] == "wake_word" }
        val checkInDone = events.any { it["type"] == "check_in" && it["completed"] == true }
        val emergencyCount = events.count { it["type"] == "emergency" }

        val lastActive = events.maxOfOrNull { it["timestamp"] as? Long ?: 0L }
        val lastActiveStr = lastActive?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "unknown"

        val summary = buildString {
            appendLine("Conversations today: $wakeCount")
            appendLine("Last active: $lastActiveStr")
            appendLine("Check-in: ${if (checkInDone) "completed" else "missed"}")
            appendLine("Emergency alerts: $emergencyCount")
        }.trim()

        val carerToken = db.child("pairing").child(uid).child("carerFcmToken")
            .get().await().getValue<String>() ?: return Result.success()

        db.child("summaries").child(uid).child(today).setValue(
            mapOf(
                "summary" to summary,
                "timestamp" to System.currentTimeMillis(),
                "carerToken" to carerToken
            )
        ).await()

        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_summary",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
