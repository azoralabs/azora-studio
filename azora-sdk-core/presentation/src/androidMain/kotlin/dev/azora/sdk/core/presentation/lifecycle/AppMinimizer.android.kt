package dev.azora.sdk.core.presentation.lifecycle

import android.app.Activity

/**
 * Android implementation of [AppMinimizer].
 *
 * Minimizes the app by moving its task to the back, mimicking the Home button. An [Activity]
 * must be bound via [attach] (done by [rememberAppMinimizer]) before [minimize] has any effect.
 */
actual class AppMinimizer actual constructor() {

    private var activity: Activity? = null

    /** Binds the [Activity] whose task will be sent to the background. */
    fun attach(activity: Activity?) {
        this.activity = activity
    }

    actual fun minimize() {
        activity?.moveTaskToBack(true)
    }
}
