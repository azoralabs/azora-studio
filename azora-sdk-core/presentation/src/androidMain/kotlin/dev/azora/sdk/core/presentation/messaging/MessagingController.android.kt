package dev.azora.sdk.core.presentation.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Android implementation of [MessagingController] using `ACTION_SENDTO` intents.
 *
 * A [Context] must be supplied via [attach] (done by [createMessagingController]) before use.
 */
actual class MessagingController {

    private var context: Context? = null

    /** Binds the [Context] used to launch the mail/SMS chooser. */
    fun attach(context: Context) {
        this.context = context
    }

    actual fun openEmail(to: String, subject: String?, body: String?) {
        val ctx = context ?: return
        val query = buildString {
            val params = buildList {
                if (!subject.isNullOrBlank()) add("subject=" + Uri.encode(subject))
                if (!body.isNullOrBlank()) add("body=" + Uri.encode(body))
            }
            if (params.isNotEmpty()) append("?").append(params.joinToString("&"))
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to$query"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(intent) }
    }

    actual fun sendSms(phoneNumber: String, message: String) {
        val ctx = context ?: return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(intent) }
    }
}
