package dev.azora.sdk.core.project.data.mapper

import dev.azora.local.database.entity.project.AzoraProjectEntity
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.ProjectSettings
import kotlinx.serialization.json.Json
import kotlin.time.Instant

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun AzoraProjectModel.toEntity() = AzoraProjectEntity(
    id = id,
    name = name,
    companyName = companyName,
    packageName = packageName,
    version = version,
    engineVersion = engineVersion,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds(),
    settingsJson = json.encodeToString(settings)
)

fun AzoraProjectEntity.toModel() = AzoraProjectModel(
    id = id,
    name = name,
    companyName = companyName,
    packageName = packageName,
    version = version,
    engineVersion = engineVersion,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) },
    settings = try {
        json.decodeFromString<ProjectSettings>(settingsJson)
    } catch (_: Exception) {
        ProjectSettings()
    }
)