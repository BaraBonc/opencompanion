package com.opencompanion.companion.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.opencompanion.companion.R
import com.opencompanion.companion.databinding.ActivityMainBinding
import com.opencompanion.companion.scheduler.ProactiveCheckInScheduler
import com.opencompanion.companion.service.DailySummaryWorker
import com.opencompanion.companion.service.WakeWordService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.statusText.setText(R.string.status_listening)

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startServices()
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startServices()
        } else {
            binding.statusText.setText(R.string.status_permission_denied)
        }
    }

    private fun startServices() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, WakeWordService::class.java)
        )
        ProactiveCheckInScheduler.schedule(this)
        DailySummaryWorker.schedule(this)
    }
}
