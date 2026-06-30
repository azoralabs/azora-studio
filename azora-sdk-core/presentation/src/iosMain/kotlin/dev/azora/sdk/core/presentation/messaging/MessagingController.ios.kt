package dev.azora.sdk.core.presentation.messaging

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication

/**
 * iOS implementation of [MessagingController] using `mailto:` and `sms:` URL schemes opened via
 * [UIApplication].
 */
actual class MessagingController {

    actual fun openEmail(to: String, subject: String?, body: String?) {
        val params = buildList {
            if (!subject.isNullOrBlank()) add("subject=${subject.urlEncoded()}")
            if (!body.isNullOrBlank()) add("body=${body.urlEncoded()}")
        }
        val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        openUrl("mailto:$to$query")
    }

    actual fun sendSms(phoneNumber: String, message: String) {
        // The iOS sms: scheme does not support a prefilled body via URL.
        openUrl("sms:$phoneNumber")
    }

    private fun openUrl(urlString: String) {
        val url = NSURL.URLWithString(urlString) ?: return
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(url)) {
            app.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
        }
    }

    private fun String.urlEncoded(): String =
        (this as NSString).stringByAddingPercentEncodingWithAllowedCharacters(
            NSCharacterSet.URLQueryAllowedCharacterSet
        ) ?: this
}
