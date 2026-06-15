package dev.azora.sdk.core.data.auth

import androidx.datastore.core.DataStore
import dev.azora.sdk.core.data.util.appDataDirectory
import java.io.File

/**
 * Creates a Jetpack [DataStore] instance in a platform-appropriate directory.
 *
 * The storage file is placed in [appDataDirectory], which varies per OS.
 * The directory will be created automatically if it does not already exist.
 *
 * @return A configured [DataStore] instance.
 */
fun createDataStore() = createDataStore {
    val directory = appDataDirectory

    if (!directory.exists()) {
        directory.mkdirs()
    }

    File(directory, DATA_STORE_FILE_NAME).absolutePath
}