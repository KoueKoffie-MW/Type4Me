package com.transcriptor.hid

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.transcriptor.hid.ai.GeminiRemoteRewriter
import com.transcriptor.hid.ai.TextRewriter
import com.transcriptor.hid.data.DataStoreSettingsRepository
import com.transcriptor.hid.data.DefaultPresetRepository
import com.transcriptor.hid.data.PresetRepository
import com.transcriptor.hid.data.SettingsRepository
import com.transcriptor.hid.data.db.AppDatabase
import com.transcriptor.hid.engine.DefaultKeystrokeDispatcher
import com.transcriptor.hid.engine.GermanQwertzKeymap
import com.transcriptor.hid.engine.KeystrokeDispatcher
import com.transcriptor.hid.service.BluetoothHidTransport
import com.transcriptor.hid.service.HidTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "transcriptor_settings")

/**
 * Android Application singleton initializing core persistence, database seeding,
 * HID transport, and dependency graphs.
 */
class TranscriptorApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var presetRepository: PresetRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var textRewriter: TextRewriter
        private set

    lateinit var bluetoothHidTransport: BluetoothHidTransport
        private set

    lateinit var hidTransport: HidTransport
        private set

    lateinit var keystrokeDispatcher: KeystrokeDispatcher
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Room DB & Preset Repository
        database = AppDatabase.getInstance(this)
        presetRepository = DefaultPresetRepository(database.presetDao())

        // 2. Seed initial presets in background
        applicationScope.launch {
            presetRepository.ensureBuiltInPresetsSeeded()
        }

        // 3. Initialize DataStore Settings Repository
        settingsRepository = DataStoreSettingsRepository(dataStore)

        // 4. Initialize Google GenAI Rewriter with dynamic model, accent, and language pickup
        textRewriter = GeminiRemoteRewriter(
            apiKeyProvider = { settingsRepository.apiKey.first() },
            modelProvider = { settingsRepository.selectedModel.first() },
            accentProvider = { settingsRepository.speakerAccent.first() },
            languageProvider = { settingsRepository.spokenLanguage.first() }
        )

        // 5. Initialize Bluetooth HID Transport & Dispatcher
        val btTransport = BluetoothHidTransport(applicationContext)
        bluetoothHidTransport = btTransport
        hidTransport = btTransport
        keystrokeDispatcher = DefaultKeystrokeDispatcher(
            translator = GermanQwertzKeymap(),
            reportSender = { report -> hidTransport.sendKeyboardReport(report) }
        )
    }

    companion object {
        lateinit var instance: TranscriptorApp
            private set
    }
}
