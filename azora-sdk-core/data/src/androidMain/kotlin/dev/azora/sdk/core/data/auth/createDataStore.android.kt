package dev.azora.sdk.core.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Creates a Jetpack [DataStore] instance backed by the app's private files directory.
 *
 * @param context An Android [Context] (typically the application context).
 * @return A configured [DataStore] instance.
 */
fun createDataStore(context: Context): DataStore<Preferences> = createDataStore {
    context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
}
