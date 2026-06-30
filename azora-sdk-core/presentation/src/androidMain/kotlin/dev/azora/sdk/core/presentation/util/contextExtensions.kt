package dev.azora.sdk.core.presentation.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Unwraps the [Activity] from a (potentially wrapped) [Context], or null if none is found.
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
