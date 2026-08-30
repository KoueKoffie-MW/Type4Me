package com.transcriptor.hid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.transcriptor.hid.engine.KeyLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Repository interface for managing user preferences and application settings.
 */
interface SettingsRepository {
    /**
     * Active keyboard layout for host keystroke translation (German QWERTZ or US QWERTY).
     */
    val keyLayout: Flow<KeyLayout>

    /**
     * Inter-character pacing delay in milliseconds (recommended: 5-10ms).
     */
    val typingDelayMs: Flow<Long>

    /**
     * Flag indicating whether real-time delta-diff mode is active (vs buffered burst mode).
     */
    val liveDiffModeEnabled: Flow<Boolean>

    /**
     * Google Gemini API key for cloud text rewriting.
     */
    val apiKey: Flow<String?>

    /**
     * Selected Gemini model identifier (e.g. "gemini-3.7-flash", "gemini-3.5-flash-lite").
     */
    val selectedModel: Flow<String>

    /**
     * ID of the currently selected prompt preset.
     */
    val activePresetId: Flow<Long>

    suspend fun setKeyLayout(layout: KeyLayout)
    suspend fun setTypingDelayMs(delayMs: Long)
    suspend fun setLiveDiffModeEnabled(enabled: Boolean)
    suspend fun setApiKey(apiKey: String)
    suspend fun setSelectedModel(model: String)
    suspend fun setActivePresetId(id: Long)

    companion object {
        val DEFAULT_KEY_LAYOUT = KeyLayout.GERMAN_QWERTZ
        const val DEFAULT_TYPING_DELAY_MS = 8L
        const val DEFAULT_LIVE_DIFF_ENABLED = false
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
        const val DEFAULT_ACTIVE_PRESET_ID = 1L
    }
}

/**
 * Implementation of [SettingsRepository] backed by AndroidX DataStore Preferences.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val KEY_LAYOUT = stringPreferencesKey("key_layout")
        val TYPING_DELAY_MS = longPreferencesKey("typing_delay_ms")
        val LIVE_DIFF_MODE = booleanPreferencesKey("live_diff_mode")
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val ACTIVE_PRESET_ID = longPreferencesKey("active_preset_id")
    }

    override val keyLayout: Flow<KeyLayout> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val layoutName = preferences[PreferencesKeys.KEY_LAYOUT]
            if (layoutName != null) {
                runCatching { KeyLayout.valueOf(layoutName) }.getOrDefault(SettingsRepository.DEFAULT_KEY_LAYOUT)
            } else {
                SettingsRepository.DEFAULT_KEY_LAYOUT
            }
        }

    override val typingDelayMs: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.TYPING_DELAY_MS] ?: SettingsRepository.DEFAULT_TYPING_DELAY_MS
        }

    override val liveDiffModeEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LIVE_DIFF_MODE] ?: SettingsRepository.DEFAULT_LIVE_DIFF_ENABLED
        }

    override val apiKey: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.API_KEY]
        }

    override val selectedModel: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_MODEL] ?: SettingsRepository.DEFAULT_MODEL
        }

    override val activePresetId: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.ACTIVE_PRESET_ID] ?: SettingsRepository.DEFAULT_ACTIVE_PRESET_ID
        }

    override suspend fun setKeyLayout(layout: KeyLayout) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_LAYOUT] = layout.name
        }
    }

    override suspend fun setTypingDelayMs(delayMs: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TYPING_DELAY_MS] = delayMs
        }
    }

    override suspend fun setLiveDiffModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIVE_DIFF_MODE] = enabled
        }
    }

    override suspend fun setApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = apiKey
        }
    }

    override suspend fun setSelectedModel(model: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MODEL] = model
        }
    }

    override suspend fun setActivePresetId(id: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_PRESET_ID] = id
        }
    }
}

/**
 * In-memory implementation of [SettingsRepository] for fast, deterministic unit testing.
 */
class InMemorySettingsRepository(
    initialKeyLayout: KeyLayout = SettingsRepository.DEFAULT_KEY_LAYOUT,
    initialTypingDelayMs: Long = SettingsRepository.DEFAULT_TYPING_DELAY_MS,
    initialLiveDiffEnabled: Boolean = SettingsRepository.DEFAULT_LIVE_DIFF_ENABLED,
    initialApiKey: String? = null,
    initialModel: String = SettingsRepository.DEFAULT_MODEL,
    initialActivePresetId: Long = SettingsRepository.DEFAULT_ACTIVE_PRESET_ID
) : SettingsRepository {

    private val _keyLayout = MutableStateFlow(initialKeyLayout)
    override val keyLayout: Flow<KeyLayout> = _keyLayout.asStateFlow()

    private val _typingDelayMs = MutableStateFlow(initialTypingDelayMs)
    override val typingDelayMs: Flow<Long> = _typingDelayMs.asStateFlow()

    private val _liveDiffModeEnabled = MutableStateFlow(initialLiveDiffEnabled)
    override val liveDiffModeEnabled: Flow<Boolean> = _liveDiffModeEnabled.asStateFlow()

    private val _apiKey = MutableStateFlow(initialApiKey)
    override val apiKey: Flow<String?> = _apiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(initialModel)
    override val selectedModel: Flow<String> = _selectedModel.asStateFlow()

    private val _activePresetId = MutableStateFlow(initialActivePresetId)
    override val activePresetId: Flow<Long> = _activePresetId.asStateFlow()

    override suspend fun setKeyLayout(layout: KeyLayout) {
        _keyLayout.value = layout
    }

    override suspend fun setTypingDelayMs(delayMs: Long) {
        _typingDelayMs.value = delayMs
    }

    override suspend fun setLiveDiffModeEnabled(enabled: Boolean) {
        _liveDiffModeEnabled.value = enabled
    }

    override suspend fun setApiKey(apiKey: String) {
        _apiKey.value = apiKey
    }

    override suspend fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    override suspend fun setActivePresetId(id: Long) {
        _activePresetId.value = id
    }
}
