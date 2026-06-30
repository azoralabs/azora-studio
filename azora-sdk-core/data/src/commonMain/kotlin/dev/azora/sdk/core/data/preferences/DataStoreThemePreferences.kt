package dev.azora.sdk.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.azora.sdk.core.domain.preferences.ThemePreference
import dev.azora.sdk.core.domain.preferences.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Desktop implementation of [ThemePreferences] that uses Jetpack DataStore for persistence.
 *
 * This class stores the user's selected [ThemePreference] and exposes a reactive [Flow]
 * for observing theme changes.
 *
 * @property dataStore The [DataStore] instance used for persistent key-value storage.
 */
class DataStoreThemePreferences(
    private val dataStore: DataStore<Preferences>
) : ThemePreferences {

    private val themePreferenceKey = stringPreferencesKey("theme_preference")

    /**
     * Observes the stored [ThemePreference] from [DataStore].
     *
     * If no value is found or an invalid value is stored, defaults to [ThemePreference.SYSTEM].
     *
     * @return A [Flow] emitting the current [ThemePreference].
     */
    override fun observeThemePreference() = dataStore
        .data
        .map { preferences ->
            val currentPreference = preferences[themePreferenceKey] ?: ThemePreference.SYSTEM.name
            try {
                ThemePreference.valueOf(currentPreference)
            } catch (_: Exception) {
                ThemePreference.SYSTEM
            }
        }

    /**
     * Updates the stored [ThemePreference] value in [DataStore].
     *
     * @param theme The new [ThemePreference] to save.
     */
    override suspend fun updateThemePreference(theme: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[themePreferenceKey] = theme.name
        }
    }
}