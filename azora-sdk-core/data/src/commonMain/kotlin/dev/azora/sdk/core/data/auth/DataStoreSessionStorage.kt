package dev.azora.sdk.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import dev.azora.sdk.core.domain.auth.SessionStorage
import dev.azora.shared.dto.user.response.AuthResponse
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

/**
 * DataStore implementation of SessionStorage.
 *
 * Persists authentication information and user location using Android DataStore
 * with JSON serialization for complex objects.
 *
 * @property dataStore The DataStore instance for preference storage
 */
class DataStoreSessionStorage(
    private val dataStore: DataStore<Preferences>
) : dev.azora.sdk.core.domain.auth.SessionStorage {

    private val authKey = stringPreferencesKey("KEY_AUTH")
    private val localeKey = stringPreferencesKey("KEY_APP_LOCALE")
    private val daysTogetherKey = intPreferencesKey("KEY_DAYS_TOGETHER")

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Observes the current authentication state as a [Flow].
     *
     * When authentication state changes (e.g., login or logout),
     * new values are emitted automatically.
     *
     * @return A [Flow] emitting the current [AuthResponse], or `null` if no session exists.
     */
    override fun observeAuthState() = dataStore.data.map { preferences ->
        val serializedJson = preferences[authKey]
        serializedJson?.let {
            json.decodeFromString<AuthResponse>(it)
        }
    }

    /**
     * Persists or clears authentication info in [DataStore].
     *
     * @param state The [AuthResponse] to save, or `null` to clear the session.
     */
    override suspend fun setAuthState(state: AuthResponse?) {
        if (state == null) {
            dataStore.edit {
                it.remove(authKey)
            }
            return
        }

        val serialized = json.encodeToString(state)
        dataStore.edit { prefs ->
            prefs[authKey] = serialized
        }
    }

    /**
     * Retrieves the currently stored application locale from [DataStore].
     *
     * @return The saved locale as a [String] (ISO 639-1 code), or `null` if no locale has been
     * stored yet.
     */
    override suspend fun getLocale(): String? {
        val prefs = dataStore.data.first()
        return prefs[localeKey]
    }

    /**
     * Persists or clears the application locale in [DataStore].
     *
     * @param locale The locale to save as a [String] (ISO 639-1 code), or `null` to remove the
     * saved locale.
     */
    override suspend fun setLocale(locale: String?) {
        dataStore.edit { prefs ->
            if (locale == null) prefs.remove(localeKey)
            else prefs[localeKey] = locale
        }
    }

    /**
     * Observes changes to the stored application locale.
     *
     * Emits updates whenever the locale is set or cleared in [DataStore].
     *
     * @return A [Flow] emitting the current locale as a [String] (ISO 639-1 code), or `null`
     * if not set.
     */
    override fun observeLocale() =
        dataStore.data.map { prefs -> prefs[localeKey] }

    /**
     * Retrieves the currently cached days together value from [DataStore].
     *
     * @return The cached days together as an [Int], or `null` if not cached yet.
     */
    override suspend fun getDaysTogether(): Int? {
        val prefs = dataStore.data.first()
        return prefs[daysTogetherKey]
    }

    /**
     * Caches or clears the days together value in [DataStore].
     *
     * @param days The days together value to cache as an [Int], or `null` to remove the cached value.
     */
    override suspend fun setDaysTogether(days: Int?) {
        dataStore.edit { prefs ->
            if (days == null) prefs.remove(daysTogetherKey)
            else prefs[daysTogetherKey] = days
        }
    }

    /**
     * Observes changes to the cached days together value.
     *
     * Emits updates whenever the value is set or cleared in [DataStore].
     *
     * @return A [Flow] emitting the current days together value as an [Int], or `null` if not cached.
     */
    override fun observeDaysTogether() =
        dataStore.data.map { prefs -> prefs[daysTogetherKey] }
}