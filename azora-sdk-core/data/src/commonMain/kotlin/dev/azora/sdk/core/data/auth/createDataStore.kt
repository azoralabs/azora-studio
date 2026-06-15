package dev.azora.sdk.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import okio.Path.Companion.toPath

/**
 * Creates a platform-agnostic DataStore instance for preferences.
 *
 * This function uses the preferences DataStore API to create a persistent
 * key-value storage backed by a Protocol Buffers file. The file location
 * is determined by the provided [producePath] lambda, allowing each platform
 * to specify its own storage directory.
 *
 * Platform implementations should provide the appropriate storage path:
 * - Android: Context.filesDir or similar
 * - iOS: Documents or Library directory
 * - Desktop: User's home directory or app data folder
 *
 * @param producePath Lambda that returns the absolute file path where the DataStore file should be created
 * @return A configured [DataStore] instance for reading and writing preferences
 */
fun createDataStore(producePath: () -> String) = PreferenceDataStoreFactory.createWithPath {
    producePath().toPath()
}

/**
 * The filename used for the DataStore preferences file.
 *
 * This Protocol Buffers file stores all app preferences including:
 * - Authentication tokens
 * - User location preferences
 * - App locale settings
 * - Other session data
 */
internal const val DATA_STORE_FILE_NAME = "prefs.preferences_pb"