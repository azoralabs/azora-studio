package dev.azora.sdk.core.data.auth

/*import dev.azora.core.data.networking.*
import dev.azora.core.datashared.dto.user.request.*
import dev.azora.core.datashared.dto.user.response.*
import dev.azora.core.datashared.networking.response.ApiResponse
import dev.azora.core.domain.auth.AuthService
import dev.azora.core.domain.util.*
import dev.azora.sdk.core.data.networking.delete
import dev.azora.sdk.core.data.networking.get
import dev.azora.sdk.core.data.networking.patch
import dev.azora.sdk.core.data.networking.post
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider

/**
 * Implementation of [AuthService] using Ktor for network communication.
 *
 * Handles all authentication-related API calls such as:
 * - Login / Register
 * - Email verification and resending verification links
 * - Password management (reset, change, forgot)
 * - Logout with token invalidation
 *
 * @property httpClient The configured [HttpClient] used for network requests.
 */
class KtorAuthService(
    private val httpClient: HttpClient
) : AuthService {

    /**
     * Logs in the user with the provided [email] and [password].
     *
     * Sends a POST request to `/auth/login` with the credentials.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return A [Res] containing the [AuthResponse] on success, or [DataError.Remote] on
     * failure.
     */
    override suspend fun login(
        email: String,
        password: String
    ) = httpClient.post<LoginRequest, ApiResponse<AuthResponse, Nothing>>(
        route = "/auth/login",
        body = LoginRequest(email, password)
    ).map { it.data }

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
    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        profilePicture: String?
    ): EmptyRes<DataError.Remote> = httpClient.post(
        route = "/auth/signup",
        body = CreateUserRequest(email, password, firstName, lastName, phone, profilePicture)
    )

    /**
     * Resends the email verification link to the specified [email].
     *
     * Sends a POST request to `/auth/resend-verification`.
     *
     * @param email The email address to resend the verification link to.
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    override suspend fun resendVerificationEmail(
        email: String
    ): EmptyRes<DataError.Remote> = httpClient.post(
        route = "/auth/resend-verification",
        body = EmailRequest(email),
    )

    /**
     * Verifies a user's email using the provided verification [token].
     *
     * Sends a GET request to `/auth/verify`.
     *
     * @param token The email verification token received by the user.
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    override suspend fun verifyEmail(
        token: String
    ): EmptyRes<DataError.Remote> = httpClient.get(
        route = "/auth/verify",
        queryParams = mapOf("token" to token)
    )

    /**
     * Sends a password reset email to the specified [email].
     *
     * Sends a POST request to `/auth/forgot-password`.
     *
     * @param email The email address associated with the user account.
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    override suspend fun forgotPassword(
        email: String
    ): EmptyRes<DataError.Remote> = httpClient.post<EmailRequest, Unit>(
        route = "/auth/forgot-password",
        body = EmailRequest(email)
    )

    /**
     * Resets a user's password using the provided reset [token] and [newPassword].
     *
     * Sends a POST request to `/auth/reset-password`.
     *
     * @param newPassword The new password to set.
     * @param token The password reset token received by the user.
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    override suspend fun resetPassword(
        newPassword: String,
        token: String
    ): EmptyRes<DataError.Remote> = httpClient.post(
        route = "/auth/reset-password",
        body = ResetPasswordRequest(
            newPassword = newPassword,
            token = token
        )
    )

    /**
     * Changes the password of the currently authenticated user.
     *
     * Sends a POST request to `/auth/change-password`.
     *
     * @param currentPassword The user's current password.
     * @param newPassword The new password to set.
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): EmptyRes<DataError.Remote> = httpClient.post(
        route = "/auth/change-password",
        body = ChangePasswordRequest(
            oldPassword = currentPassword,
            newPassword = newPassword
        )
    )

    /**
     * Logs out the currently authenticated user by invalidating the provided [refreshToken].
     *
     * Sends a POST request to `/auth/logout`. On success, clears the token from the
     * [HttpClient]'s [BearerAuthProvider] to ensure the session is terminated locally.
     *
     * @param refreshToken The refresh token to invalidate on the server.
     * @return An [EmptyRes] indicating success or failure with [DataError.Remote].
     */
    override suspend fun logout(
        refreshToken: String
    ): EmptyRes<DataError.Remote> = httpClient.post<RefreshRequest, Unit>(
        route = "/auth/logout",
        body = RefreshRequest(refreshToken)
    ).onSuccess {
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
    }

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
    override suspend fun deleteAccount(
        password: String
    ): EmptyRes<DataError.Remote> = httpClient.delete<DeleteAccountRequest, Unit>(
        route = "/users/me",
        body = DeleteAccountRequest(password)
    ).onSuccess {
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
    }

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
    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        profilePicture: String?,
        removeProfilePicture: Boolean?
    ): EmptyRes<DataError.Remote> = httpClient.patch<UpdateUserRequest, Unit>(
        route = "/users/me",
        body = UpdateUserRequest(
            firstName = firstName,
            lastName = lastName,
            profilePicture = profilePicture,
            removeProfilePicture = removeProfilePicture
        )
    )

    /**
     * Fetches the number of days since the user created their account.
     *
     * Sends a GET request to `/users/me/days-together` to retrieve the calculated
     * days between the user's registration date and the current date.
     *
     * @return A [Res] containing [DaysTogetherResponse] with the days count on success,
     *         or [DataError.Remote] on failure.
     */
    override suspend fun getDaysTogether(): Res<DaysTogetherResponse, DataError.Remote> =
        httpClient.get<ApiResponse<DaysTogetherResponse, Nothing>>(
            route = "/users/me/days-together"
        ).map { it.data }

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
    override suspend fun loginWithGoogle(idToken: String):
            Res<AuthResponse, DataError.Remote> =
        httpClient.post<GoogleAuthRequest, ApiResponse<AuthResponse, Nothing>>(
            route = "/auth/google",
            body = GoogleAuthRequest(idToken = idToken)
        ).map { it.data }
}*/