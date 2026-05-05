package com.opencompanion.companion.audio

import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaRecorder

class BluetoothScoManager(
    private val context: Context,
    private val onSourceChanged: () -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var scoActive = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED
                    )
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        audioManager.startBluetoothSco()
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        audioManager.stopBluetoothSco()
                        scoActive = false
                        onSourceChanged()
                    }
                }
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    val state = intent.getIntExtra(
                        AudioManager.EXTRA_SCO_AUDIO_STATE,
                        AudioManager.SCO_AUDIO_STATE_ERROR
                    )
                    if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        scoActive = true
                        onSourceChanged()
                    } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                        scoActive = false
                        onSourceChanged()
                    }
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun unregister() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        if (scoActive) audioManager.stopBluetoothSco()
    }

    fun currentAudioSource(): Int =
        if (scoActive) MediaRecorder.AudioSource.VOICE_COMMUNICATION
        else MediaRecorder.AudioSource.MIC
}
