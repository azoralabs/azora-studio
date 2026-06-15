package dev.azora.sdk.core.domain.util

/**
 * Represents structured data-related errors, categorized into remote,
 * local, and connection types.
 *
 * This sealed interface allows handling of different failure domains
 * (such as API, storage, or connection) in a type-safe manner.
 */
sealed interface DataError : Error {

    /**
     * Defines errors that can occur during remote (network or API) operations.
     */
    enum class Remote : DataError {

        /** The request was malformed or invalid. */
        BAD_REQUEST,

        /** The request took too long to complete. */
        REQUEST_TIMEOUT,

        /** The user is not authorized to perform the operation. */
        UNAUTHORIZED,

        /** Access to the requested resource is forbidden. */
        FORBIDDEN,

        /** The requested resource was not found. */
        NOT_FOUND,

        /** A conflict occurred, such as a duplicate resource. */
        CONFLICT,

        /** The client made too many requests in a short time. */
        TOO_MANY_REQUESTS,

        /** No internet connection is available. */
        NO_INTERNET,

        /** The payload size exceeds the server’s limit. */
        PAYLOAD_TOO_LARGE,

        /** A server-side error occurred. */
        SERVER_ERROR,

        /** The server is currently unavailable. */
        SERVICE_UNAVAILABLE,

        /** An error occurred during (de)serialization of data. */
        SERIALIZATION,

        /** An unexpected or unknown remote error occurred. */
        UNKNOWN
    }

    /**
     * Defines errors that can occur during local data operations,
     * such as storage or caching.
     */
    enum class Local : DataError {

        /** There is not enough disk space to complete the operation. */
        DISK_FULL,

        /** The requested local data was not found. */
        NOT_FOUND,

        /** An unknown local error occurred. */
        UNKNOWN
    }

    /**
     * Defines errors related to real-time or persistent connections.
     */
    enum class Connection : DataError {

        /** No active connection is established. */
        NOT_CONNECTED,

        /** Sending a message over a connection failed. */
        MESSAGE_SEND_FAILED
    }
}