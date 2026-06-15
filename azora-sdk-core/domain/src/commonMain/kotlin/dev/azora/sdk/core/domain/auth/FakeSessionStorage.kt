package dev.azora.sdk.core.domain.auth

/*import dev.azora.core.datashared.dto.user.response.AuthResponse
import kotlinx.coroutines.flow.*

/**
 * No-op implementation of [SessionStorage].
 *
 * This storage intentionally performs no persistent operations and returns default/null values.
 * It is useful in the following scenarios:
 * - Unit and integration testing
 * - UI previews where dependency injection is required
 * - Environments where session storage is not desired or must be disabled
 * - Dependency injection as a default or fallback storage
 *
 * Since this implementation does nothing, it is safe to use across all platforms
 * without causing side effects or requiring platform-specific dependencies.
 */
class FakeSessionStorage : SessionStorage {

    /**
     * Returns an empty flow emitting null.
     *
     * @return Flow emitting null
     */
    override fun observeAuthState(): Flow<AuthResponse?> = flowOf(null)

    /**
     * Ignores the provided auth state.
     *
     * @param state The auth state (discarded)
     */
    override suspend fun setAuthState(state: AuthResponse?) {}

    /**
     * Returns null locale.
     *
     * @return null
     */
    override suspend fun getLocale(): String? = null

    /**
     * Ignores the provided locale.
     *
     * @param locale The locale (discarded)
     */
    override suspend fun setLocale(locale: String?) {}

    /**
     * Returns an empty flow emitting null.
     *
     * @return Flow emitting null
     */
    override fun observeLocale(): Flow<String?> = flowOf(null)

    /**
     * Returns null days together.
     *
     * @return null
     */
    override suspend fun getDaysTogether(): Int? = null

    /**
     * Ignores the provided days together value.
     *
     * @param days The days together value (discarded)
     */
    override suspend fun setDaysTogether(days: Int?) {}

    /**
     * Returns an empty flow emitting null.
     *
     * @return Flow emitting null
     */
    override fun observeDaysTogether(): Flow<Int?> = flowOf(null)
}*/