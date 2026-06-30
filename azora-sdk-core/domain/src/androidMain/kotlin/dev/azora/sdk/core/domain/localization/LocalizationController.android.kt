package dev.azora.sdk.core.domain.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Android implementation of [LocalizationController].
 *
 * Updates the JVM default locale and, when an application [Context] has been supplied via
 * [initialize], applies the locale to that context's resource configuration so newly created
 * resources resolve against the chosen language. Already-laid-out UI is not refreshed
 * automatically — recreate the activity or recompose to reflect the change.
 */
actual class LocalizationController actual constructor() {

    private var appContext: Context? = null

    /**
     * Stores the application [Context] used to update the resource configuration.
     *
     * @param context An Android [Context] (typically the application context), or null for a no-op.
     */
    actual fun initialize(context: Any?): LocalizationController {
        appContext = (context as? Context)?.applicationContext
        return this
    }

    /**
     * Updates the JVM default locale and the application resources' configuration locale.
     *
     * @param languageCode ISO 639-1 language code (e.g. "en", "fr")
     */
    actual fun update(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        appContext?.let { ctx ->
            val resources = ctx.resources
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
