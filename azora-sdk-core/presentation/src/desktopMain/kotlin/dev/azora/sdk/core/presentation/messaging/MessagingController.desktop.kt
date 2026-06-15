package dev.azora.sdk.core.presentation.messaging

import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

/**
 * Desktop implementation of [dev.azora.core.presentation.messaging.MessagingController].
 *
 * Email: opens system default mail client.
 * SMS: Opens Google Messages web or OS-specific URL opener.
 */
actual class MessagingController {

    /**
     * Attempts to open the system mail client using a mailto: URI.
     */
    actual fun openEmail(to: String, subject: String?, body: String?) {
        val uriString = buildString {
            append("mailto:")
            append(to)
            var first = true
            fun appendParam(key: String, value: String?) {
                if (!value.isNullOrBlank()) {
                    append(if (first) "?" else "&")
                    append(key)
                    append("=")
                    append(URLEncoder.encode(value, Charsets.UTF_8))
                    first = false
                }
            }
            appendParam("subject", subject)
            appendParam("body", body)
        }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().mail(URI(uriString))
        }
    }

    /**
     * Desktop cannot send SMS natively.
     * Instead, it opens Google Messages Web with the number pre-filled.
     */
    actual fun sendSms(phoneNumber: String, message: String) {
        val url = "https://messages.google.com/web/conversations/new?phone=$phoneNumber"
        try {
            val os = System.getProperty("os.name").lowercase()
            when {
                os.contains("win") -> Runtime.getRuntime()
                    .exec("rundll32 url.dll,FileProtocolHandler $url")
                os.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", url))
                else -> Runtime.getRuntime().exec(arrayOf("xdg-open", url))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}