package com.opencompanion.companion.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.preference.PreferenceManager
import com.opencompanion.companion.firebase.FirebaseLogger
import java.util.Calendar
import java.util.Locale

class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val greeting = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("check_in_greeting", "Szia! Minden rendben van?")
            ?: "Szia! Minden rendben van?"
        ProactiveCheckInScheduler.triggerCheckIn(context, greeting)
        ProactiveCheckInScheduler.scheduleNext(context)
    }
}

object ProactiveCheckInScheduler {

    private const val REQUEST_CODE = 1001
    private const val PREF_HOUR = "check_in_hour"
    private const val PREF_MINUTE = "check_in_minute"

    fun schedule(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val hour = prefs.getInt(PREF_HOUR, 10)
        val minute = prefs.getInt(PREF_MINUTE, 0)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent(context)
        )
    }

    fun scheduleNext(context: Context) = schedule(context)

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context))
    }

    fun triggerCheckIn(context: Context, greeting: String) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        fireAssist(context)
                        FirebaseLogger().logCheckIn(completed = true)
                        tts?.shutdown()
                    }
                    override fun onError(utteranceId: String?) {
                        tts?.shutdown()
                    }
                })
                tts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "check_in")
            }
        }
    }

    private fun fireAssist(context: Context) {
        val intent = Intent(Intent.ACTION_ASSIST).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CheckInReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
