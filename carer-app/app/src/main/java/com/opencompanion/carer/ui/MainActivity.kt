package com.opencompanion.carer.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.opencompanion.carer.R
import com.opencompanion.carer.databinding.ActivityMainBinding
import com.opencompanion.carer.firebase.RemoteCheckIn

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pairButton.setOnClickListener {
            startActivity(Intent(this, PairingActivity::class.java))
        }

        binding.checkInFab.setOnClickListener {
            RemoteCheckIn.trigger(this)
        }
    }
}
