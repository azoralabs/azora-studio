package dev.azora.sdk.core.domain.auth

/*import dev.azora.core.datashared.dto.user.response.*
import dev.azora.core.domain.util.*

/**
 * Defines the contract for authentication-related operations.
 *
 * This interface abstracts all core authentication workflows, including login,
 * registration, email verification, password management, and logout.
 *
 * All methods use [Res] or [EmptyRes] types for functional and
 * type-safe error handling, avoiding exceptions in favor of predictable results.
 */
interface AuthService {

    /**
     * Authenticates a user with the given [email] and [password].
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return A [Res] containing [LoginRequest] if the login succeeds,
     *         or a [DataError.Remote] if the operation fails.
     */
    suspend fun login(
        email: String,
        password: String
    ): Res<AuthResponse, DataError.Remote>

    /**
     * Registers a new user account with the given [email], [firstName], [lastName] and [password].
     *
     * Sends a POST request to `/auth/signup`.
     *
     * @param email The user's email address.
     * @param password The desired password.
     * @param firstName The user's first name.
     * @param lastName The user's last name.
     * @param phone The user's home city id.
     * @param profilePicture Optional Base64-encoded profile picture (default: null).
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        profilePicture: String?
    ): EmptyRes<DataError.Remote>

    /**
     * Sends a verification email to the provided [email].
     *
     * This is typically called after registration or when a user
     * requests a new verification email.
     *
     * @param email The email address to send the verification message to.
     * @return An [EmptyRes] representing success or containing
     *         a [DataError.Remote] on failure.
     */
    suspend fun resendVerificationEmail(
        email: String
    ): EmptyRes<DataError.Remote>

    /**
     * Verifies a user's email address using a verification [token].
     *
     * @param token The verification token received via email.
     * @return An [EmptyRes] indicating whether verification succeeded
     *         or containing a [DataError.Remote] on failure.
     */
    suspend fun verifyEmail(token: String): EmptyRes<DataError.Remote>

    /**
     * Initiates a password reset process for the given [email].
     *
     * Typically, this sends a password reset email to the user with
     * instructions or a token to complete the process.
     *
     * @param email The email address associated with the account.
     * @return An [EmptyRes] representing success or containing
     *         a [DataError.Remote] on failure.
     */
    suspend fun forgotPassword(email: String): EmptyRes<DataError.Remote>

    /**
     * Resets a user's password using a reset [token].
     *
     * @param newPassword The new password to set.
     * @param token The reset token received via email.
     * @return An [EmptyRes] representing success or containing
     *         a [DataError.Remote] on failure.
     */
    suspend fun resetPassword(
        newPassword: String,
        token: String
    ): EmptyRes<DataError.Remote>

    /**
     * Changes the password of the currently authenticated user.
     *
     * @param currentPassword The user's current password.
     * @param newPassword The new password to set.
     * @return An [EmptyRes] representing success or containing
     *         a [DataError.Remote] on failure.
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): EmptyRes<DataError.Remote>

    /**
     * Logs out the user by invalidating the provided [refreshToken].
     *
     * This ensures that both the access and refresh tokens are no longer valid,
     * effectively ending the user's session.
     *
     * @param refreshToken The refresh token to revoke.
     * @return An [EmptyRes] representing success or containing
     *         a [DataError.Remote] on failure.
     */
    suspend fun logout(refreshToken: String): EmptyRes<DataError.Remote>

    /**
     * Permanently deletes the currently authenticated user's account.
     *
     * Sends a DELETE request to `/users/me` with password confirmation.
     * On success, clears the authentication token to log out the user locally.
     *
     * This operation is irreversible and deletes all user data including:
     * - User profile
     * - All incidents created by the user
     * - All active sessions across devices
     *
     * @param password The user's current password for confirmation.
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    suspend fun deleteAccount(password: String): EmptyRes<DataError.Remote>

    /**
     * Updates the currently authenticated user's profile information.
     *
     * Sends a PATCH request to `/users/me` with the provided fields.
     * Performs a partial update - only provided fields are modified.
     *
     * @param firstName The user's updated first name (optional).
     * @param lastName The user's updated last name (optional).
     * @param profilePicture Updated Base64-encoded profile picture image data (optional).
     * @param removeProfilePicture Flag to remove existing profile picture (optional).
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        profilePicture: String?,
        removeProfilePicture: Boolean?
    ): EmptyRes<DataError.Remote>

    /**
     * Fetches the number of days since the user created their account.
     *
     * Sends a GET request to `/users/me/days-together` to retrieve the calculated
     * days between the user's registration date and the current date.
     *
     * @return A [Res] containing [DaysTogetherResponse] with the days count on success,
     *         or [DataError.Remote] on failure.
     */
    suspend fun getDaysTogether(): Res<DaysTogetherResponse, DataError.Remote>

    /**
     * Authenticates a user using a Google ID token.
     *
     * Sends a POST request to `/auth/google` with the Google ID token.
     * The backend will verify the token with Google's servers, extract user information,
     * and either create a new account or log in an existing user.
     *
     * @param idToken The Google ID token (JWT) obtained from Google Sign-In
     * @return A [Res] containing [AuthResponse] with tokens and user info on success,
     *         or [DataError.Remote] on failure.
     */
    suspend fun loginWithGoogle(idToken: String): Res<AuthResponse, DataError.Remote>
}*/