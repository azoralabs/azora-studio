package dev.azora.sdk.core.project.data.repository

import dev.azora.local.database.LocalDatabase
import dev.azora.sdk.core.domain.util.*
import dev.azora.sdk.core.project.data.mapper.*
import dev.azora.sdk.core.project.domain.SettingsModel
import dev.azora.sdk.core.project.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*

actual class LocalSettingsRepository(
    private val db: LocalDatabase
) : SettingsRepository {

    override fun observeSettings(projectId: String): Flow<SettingsModel> {
        return db.settingsDao.getSettingsFlow(projectId)
            .map { entity ->
                entity?.toModel() ?: SettingsModel.default(projectId)
            }
    }

    override suspend fun getSettings(projectId: String): Res<SettingsModel, DataError.Local> {
        return try {
            val entity = db.settingsDao.getSettings(projectId)
            val model = entity?.toModel() ?: SettingsModel.default(projectId)
            Res.Success(model)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun saveSettings(settings: SettingsModel): Res<Unit, DataError.Local> {
        return try {
            db.settingsDao.upsertSettings(settings.toEntity())
            Res.Success(Unit)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun deleteSettings(projectId: String): Res<Unit, DataError.Local> {
        return try {
            db.settingsDao.deleteSettings(projectId)
            Res.Success(Unit)
        } catch (_: Exception) {
            Res.Failure(DataError.Local.UNKNOWN)
        }
    }
}
