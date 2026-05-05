package com.opencompanion.carer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.opencompanion.carer.R
import com.opencompanion.carer.databinding.ActivityAlertDetailBinding

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra("type") ?: "emergency"
        val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())

        binding.alertTypeText.text = when (type) {
            "emergency" -> getString(R.string.alert_emergency)
            "red_flag" -> getString(R.string.alert_red_flag)
            else -> getString(R.string.alert_generic)
        }
        binding.timestampText.text = java.text.SimpleDateFormat(
            "HH:mm", java.util.Locale.getDefault()
        ).format(java.util.Date(timestamp))

        binding.acknowledgeButton.setOnClickListener { finish() }
    }
}
