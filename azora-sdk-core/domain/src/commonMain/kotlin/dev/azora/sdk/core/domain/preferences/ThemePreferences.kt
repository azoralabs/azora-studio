package dev.azora.sdk.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Defines a contract for reading and updating the user's theme preference.
 *
 * Implementations of this interface can store the theme setting
 * using different persistence mechanisms (e.g., DataStore, SharedPreferences, or a database).
 */
interface ThemePreferences {

    /**
     * Observes the current [ThemePreference] as a [Flow].
     *
     * This allows reactive updates when the theme preference changes,
     * enabling the UI to automatically respond to user or system changes.
     *
     * @return A cold [Flow] emitting the latest [ThemePreference].
     */
    fun observeThemePreference(): Flow<ThemePreference>

    /**
     * Updates the stored theme preference to the given [theme].
     *
     * This method should persist the value so that it can be restored
     * when the app is reopened.
     *
     * @param theme The new [ThemePreference] to apply.
     */
    suspend fun updateThemePreference(theme: ThemePreference)
}