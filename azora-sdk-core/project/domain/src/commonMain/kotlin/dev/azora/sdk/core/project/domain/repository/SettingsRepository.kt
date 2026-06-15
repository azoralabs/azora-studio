package dev.azora.sdk.core.project.domain.repository

import dev.azora.sdk.core.domain.util.*
import dev.azora.sdk.core.project.domain.SettingsModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing project settings.
 */
interface SettingsRepository {

    /**
     * Observes settings changes for the given project.
     */
    fun observeSettings(projectId: String): Flow<SettingsModel>

    /**
     * Gets settings for the given project.
     */
    suspend fun getSettings(projectId: String): Res<SettingsModel, DataError.Local>

    /**
     * Saves settings for the given project.
     */
    suspend fun saveSettings(settings: SettingsModel): Res<Unit, DataError.Local>

    /**
     * Deletes settings for the given project.
     */
    suspend fun deleteSettings(projectId: String): Res<Unit, DataError.Local>
}
