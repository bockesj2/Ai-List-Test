package com.example.ailisttest.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {
    companion object {
        private val DEBUG_MODE_KEY = booleanPreferencesKey("debug_mode")
        private val CONFIGURE_MODE_KEY = booleanPreferencesKey("configure_mode")
        private val POLLING_ENABLED_KEY = booleanPreferencesKey("polling_enabled")
        private val PLC_PRESET_KEY = stringPreferencesKey("app_plc_preset")
        private val BYTE_ORDER_KEY = stringPreferencesKey("app_byte_order")
        private val DATA_TYPE_PLC_PRESET_KEY = stringPreferencesKey("data_type_plc_preset")
        private val TAG_FILE_FORMAT_KEY = stringPreferencesKey("tag_file_format")
    }

    val isDebugMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEBUG_MODE_KEY] ?: false
    }

    suspend fun setDebugMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEBUG_MODE_KEY] = enabled
        }
    }

    val isConfigureMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CONFIGURE_MODE_KEY] ?: true
    }

    suspend fun setConfigureMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONFIGURE_MODE_KEY] = enabled
        }
    }

    val isPollingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[POLLING_ENABLED_KEY] ?: false
    }

    suspend fun setPollingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[POLLING_ENABLED_KEY] = enabled
        }
    }

    val plcPreset: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PLC_PRESET_KEY] ?: "Click Plus PLC"
    }

    suspend fun setPlcPreset(preset: String) {
        context.dataStore.edit { preferences ->
            preferences[PLC_PRESET_KEY] = preset
        }
    }

    val byteOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BYTE_ORDER_KEY] ?: "flip words"
    }

    suspend fun setByteOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[BYTE_ORDER_KEY] = order
        }
    }

    val dataTypePlcPreset: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DATA_TYPE_PLC_PRESET_KEY] ?: "Click Plus PLC"
    }

    suspend fun setDataTypePlcPreset(preset: String) {
        context.dataStore.edit { preferences ->
            preferences[DATA_TYPE_PLC_PRESET_KEY] = preset
        }
    }

    val tagFileFormat: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TAG_FILE_FORMAT_KEY] ?: "Click Plus"
    }

    suspend fun setTagFileFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[TAG_FILE_FORMAT_KEY] = format
        }
    }
}
