package dev.azora.sdk.core.presentation.mapper

import azorastudio.azora_sdk_core.presentation.generated.resources.*
import dev.azora.sdk.core.domain.util.DataError
import dev.azora.sdk.core.presentation.util.UiText

/**
 * Maps a [DataError] type to a user-friendly [UiText] message.
 *
 * This function centralizes error handling by converting domain-level
 * data errors into localized strings that can be easily displayed in the UI.
 *
 * ### Example
 * ```
 * val errorText = result.error.toUiText().asString()
 * ```
 *
 * @return A [UiText.Resource] corresponding to the specific error.
 */
fun DataError.toUiText() = UiText.Resource(
    when (this) {
        DataError.Local.DISK_FULL -> Res.string.error_disk_full
        DataError.Local.NOT_FOUND -> Res.string.error_not_found
        DataError.Local.UNKNOWN -> Res.string.error_unknown
        DataError.Remote.BAD_REQUEST -> Res.string.error_bad_request
        DataError.Remote.REQUEST_TIMEOUT -> Res.string.error_request_timeout
        DataError.Remote.UNAUTHORIZED -> Res.string.error_unauthorized
        DataError.Remote.FORBIDDEN -> Res.string.error_forbidden
        DataError.Remote.NOT_FOUND -> Res.string.error_not_found
        DataError.Remote.CONFLICT -> Res.string.error_conflict
        DataError.Remote.TOO_MANY_REQUESTS -> Res.string.error_too_many_requests
        DataError.Remote.NO_INTERNET -> Res.string.error_no_internet
        DataError.Remote.PAYLOAD_TOO_LARGE -> Res.string.error_payload_too_large
        DataError.Remote.SERVER_ERROR -> Res.string.error_server
        DataError.Remote.SERVICE_UNAVAILABLE -> Res.string.error_service_unavailable
        DataError.Remote.SERIALIZATION -> Res.string.error_serialization
        DataError.Remote.UNKNOWN -> Res.string.error_unknown
        DataError.Connection.NOT_CONNECTED -> Res.string.error_no_internet
        DataError.Connection.MESSAGE_SEND_FAILED -> Res.string.error_unable_to_send_message
    }
)