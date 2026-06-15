@file:UseSerializers(InstantSerializer::class)

package dev.azora.sdk.core.project.domain

import dev.azora.sdk.core.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

@Serializable
data class AzoraProjectModel(
    val id: String,
    val name: String,
    val companyName: String,
    val packageName: String,
    val version: String,
    val engineVersion: String,
    val createdAt: Instant = defaultInstant,
    val updatedAt: Instant? = null,
    val settings: ProjectSettings = ProjectSettings(),
    @Transient val projectDirectoryPath: String = ""
)

/**
 * Project-specific settings stored with the project file.
 *
 * Core settings are defined directly. Module-specific settings (docking, canvas, etc.)
 * are stored in the [extras] map, allowing modules to extend settings without
 * coupling azora-sdk-core to those modules.
 *
 * Each module should provide extension functions to read/write their settings:
 * - Docking: `settings.dockLayout`, `settings.withDockLayout(layout)`
 * - Canvas: `settings.openAzoraNodesFiles`, `settings.withOpenAzoraNodesFiles(map)`
 * - Constants: `settings.globalConstants`, `settings.withGlobalConstants(list)`
 */
@Serializable
data class ProjectSettings(
    // Core editor settings
    val editorTooltipsEnabled: Boolean = true,
    val tooltipDelaySeconds: Int = 1,
    val preferredColorPicker: ColorPickerMode = ColorPickerMode.Triangle,
    val paletteColors: List<PaletteColor> = PaletteColor.defaults,
    // Extensible storage for module-specific settings
    val extras: JsonObject = JsonObject(emptyMap())
)