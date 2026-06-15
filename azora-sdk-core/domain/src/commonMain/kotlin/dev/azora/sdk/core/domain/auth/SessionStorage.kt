package dev.azora.sdk.core.domain.auth

import dev.azora.shared.dto.user.response.AuthResponse
import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic storage interface for session-related user data.
 *
 * Provides reactive and suspending APIs for persisting and observing
 * authentication, location, and locale-related information.
 *
 * Implementations can use platform-specific storage solutions such as:
 * - Android: DataStore or SharedPreferences
 * - iOS: UserDefaults or CoreData
 * - Desktop/JVM: file-based storage or in-memory storage for testing
 */
interface SessionStorage {

    /**
     * Observes the current [AuthResponse] as a [Flow].
     *
     * This allows consumers to react to authentication state changes
     * (e.g., login, logout, token refresh) in real time.
     *
     * @return A cold [Flow] emitting the latest [AuthResponse] or `null`
     *         if no session is currently active.
     */
    fun observeAuthState(): Flow<AuthResponse?>

    /**
     * Persists or clears the provided [AuthResponse].
     *
     * Passing `null` should clear the stored session, effectively logging the user out.
     *
     * @param state The [AuthResponse] to store, or `null` to clear it.
     */
    suspend fun setAuthState(state: AuthResponse?)

    /**
     * Retrieves the stored application locale.
     *
     * @return Locale as a [String] (ISO 639-1 code), or `null` if not set
     */
    suspend fun getLocale(): String?

    /**
     * Persists the selected application locale.
     *
     * @param locale Locale as a [String] (ISO 639-1 code), or `null` to clear
     */
    suspend fun setLocale(locale: String?)

    /**
     * Observes changes to the stored application locale.
     *
     * Emits updates whenever the locale changes.
     *
     * @return [Flow] emitting the current locale or `null` if not set
     */
    fun observeLocale(): Flow<String?>

    /**
     * Retrieves the cached days together value.
     *
     * @return Days together as an [Int], or `null` if not cached
     */
    suspend fun getDaysTogether(): Int?

    /**
     * Caches the days together value.
     *
     * @param days Days together as an [Int], or `null` to clear the cache
     */
    suspend fun setDaysTogether(days: Int?)

    /**
     * Observes changes to the cached days together value.
     *
     * Emits updates whenever the value changes.
     *
     * @return [Flow] emitting the current days together value or `null` if not cached
     */
    fun observeDaysTogether(): Flow<Int?>
}