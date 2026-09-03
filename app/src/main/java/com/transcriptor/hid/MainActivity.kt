package com.transcriptor.hid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.transcriptor.hid.audio.PttAudioEngine
import com.transcriptor.hid.motion.GyroAirMouseEngine
import com.transcriptor.hid.service.BluetoothConnectionWatchdog
import com.transcriptor.hid.service.BluetoothHidTransport
import com.transcriptor.hid.service.HidDeviceService
import com.transcriptor.hid.ui.MainScreen
import com.transcriptor.hid.ui.MainViewModel
import com.transcriptor.hid.ui.theme.TranscriptorTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startHidService()
        } else {
            Toast.makeText(
                this,
                "Bluetooth permissions are required for HID keyboard emulation.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as TranscriptorApp
                val pttEngine = PttAudioEngine(app, lifecycleScope)
                val gyroEngine = GyroAirMouseEngine(app, app.hidTransport, lifecycleScope)
                val btTransport = app.hidTransport as? BluetoothHidTransport
                val watchdog = btTransport?.let { BluetoothConnectionWatchdog(it, lifecycleScope) }
                return MainViewModel(
                    settingsRepository = app.settingsRepository,
                    presetRepository = app.presetRepository,
                    textRewriter = app.textRewriter,
                    keystrokeDispatcher = app.keystrokeDispatcher,
                    hidTransport = app.hidTransport,
                    snippetRepository = app.snippetRepository,
                    macroRepository = app.macroRepository,
                    pairedHostRepository = app.pairedHostRepository,
                    pttAudioEngine = pttEngine,
                    gyroAirMouseEngine = gyroEngine,
                    connectionWatchdog = watchdog
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleApiKeyIntent(intent)
        checkAndRequestPermissions()

        setContent {
            TranscriptorTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestDiscoverability = { makeDeviceDiscoverable() },
                    onOpenBluetoothSettings = { openBluetoothSettings() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleApiKeyIntent(intent)
    }

    private fun handleApiKeyIntent(intent: Intent?) {
        val apiKey = intent?.getStringExtra("set_api_key")
        if (!apiKey.isNullOrBlank()) {
            val app = application as TranscriptorApp
            lifecycleScope.launch {
                app.settingsRepository.setApiKey(apiKey.trim())
                Toast.makeText(this@MainActivity, "Gemini API Key saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (requiredPermissions.isNotEmpty()) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            startHidService()
        }
    }

    private fun startHidService() {
        val serviceIntent = Intent(this, HidDeviceService::class.java).apply {
            action = HidDeviceService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun makeDeviceDiscoverable() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        }
        try {
            startActivity(discoverableIntent)
        } catch (_: Exception) {
            Toast.makeText(this, "Could not launch Bluetooth discoverability.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBluetoothSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, "Could not open Bluetooth settings.", Toast.LENGTH_SHORT).show()
        }
    }
}
