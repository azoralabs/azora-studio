package dev.azora.sdk.core.presentation.messaging

/**
 * Cross-platform controller for launching email and SMS apps on the user device.
 *
 * You should not instantiate this class directly.
 * Use [createMessagingController] instead, which provides the platform-correct instance.
 */
expect class MessagingController {

    /**
     * Opens the device email application and pre-fills the composed email.
     *
     * @param to recipient email address
     * @param subject optional email subject
     * @param body optional email body
     */
    fun openEmail(to: String, subject: String? = null, body: String? = null)

    /**
     * Opens the SMS application pre-filled with the given message.
     *
     * @param phoneNumber destination phone number
     * @param message the default SMS body
     */
    fun sendSms(phoneNumber: String, message: String)
}