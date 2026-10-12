package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "amoled_notes_prefs")

enum class EditorFontSize(val label: String, val sizeSp: Float, val lineHeightSp: Float) {
    SMALL("Small", 14f, 22f),
    MEDIUM("Medium", 17f, 26f),
    LARGE("Large", 21f, 32f)
}

class UserPreferences(private val context: Context) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        private val KEY_DEFAULT_FOLDER = stringPreferencesKey("default_folder")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_BIOMETRICS_ENABLED = booleanPreferencesKey("biometrics_enabled")
        private val KEY_AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")

        fun hashPin(pin: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
            else -> ThemeMode.AMOLED // AMOLED black by default
        }
    }

    val fontSizeFlow: Flow<EditorFontSize> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_FONT_SIZE]) {
            EditorFontSize.SMALL.name -> EditorFontSize.SMALL
            EditorFontSize.LARGE.name -> EditorFontSize.LARGE
            else -> EditorFontSize.MEDIUM
        }
    }

    val defaultFolderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_FOLDER] ?: "Default"
    }

    val pinHashFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_HASH]
    }

    val biometricsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRICS_ENABLED] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setFontSize(size: EditorFontSize) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size.name }
    }

    suspend fun setDefaultFolder(folder: String) {
        context.dataStore.edit { it[KEY_DEFAULT_FOLDER] = folder }
    }

    suspend fun setPin(pin: String?) {
        context.dataStore.edit { prefs ->
            if (pin != null) {
                prefs[KEY_PIN_HASH] = hashPin(pin)
            } else {
                prefs.remove(KEY_PIN_HASH)
            }
        }
    }

    suspend fun setBiometricsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRICS_ENABLED] = enabled }
    }
}
