package dev.azora.sdk.library.core

import kotlinx.serialization.Serializable

/**
 * The manifest of an Azora library bundle (`library.json` at the bundle root).
 *
 * A library is a versioned, data-driven capability package installed under
 * `~/.azora/libraries/<id>/<version>/`. Unlike plugins (which are compiled
 * Kotlin loaded into the Studio process), libraries are read declaratively:
 * the Studio only interprets their manifest — project templates are file
 * trees copied on project creation, and run targets are shell commands.
 *
 * The first library of this kind is the Azora Engine (native game engine &
 * app framework written in the Azora language), which ships "App" and
 * "Game" templates plus its compiler/runtime toolchain.
 */
@Serializable
data class LibraryManifest(
    /** Stable identifier, e.g. "azora-engine". Also the install directory name. */
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    /** Free-form kind tag, e.g. "engine". */
    val type: String = "library",
    val templates: List<LibraryTemplateSpec> = emptyList(),
)

/**
 * A project template contributed by a library.
 *
 * @property id Globally stable template id persisted on created projects (e.g. "azora-engine-game").
 * @property path Template file tree, relative to the bundle root (e.g. "templates/game").
 *   Files are copied into the new project with `__AZORA_*__` placeholders substituted.
 * @property runTargets Shell-command run targets surfaced in the Studio's run controls.
 */
@Serializable
data class LibraryTemplateSpec(
    val id: String,
    val label: String,
    val description: String = "",
    val path: String,
    val accentColor: String = "#FF9C27B0",
    val runTargets: List<LibraryRunTargetSpec> = emptyList(),
)

/**
 * A run target of a library template. Always executed as an OS shell command
 * in [workingDir] (relative to the project root), with output streamed to the
 * Studio console.
 */
@Serializable
data class LibraryRunTargetSpec(
    val id: String,
    val label: String,
    val command: String,
    val workingDir: String = ".",
)

/**
 * An installed library as tracked by the library manager.
 *
 * @property installPath Absolute path of the installed bundle
 *   (`~/.azora/libraries/<id>/<version>`).
 */
data class InstalledLibrary(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val type: String,
    val installPath: String,
    val templates: List<LibraryTemplateSpec>,
)

/** Placeholders substituted into template files when a project is created. */
object LibraryTemplatePlaceholders {
    const val PROJECT_NAME = "__AZORA_PROJECT_NAME__"
    const val PACKAGE_NAME = "__AZORA_PACKAGE_NAME__"
    const val LIBRARY_ID = "__AZORA_LIBRARY_ID__"
    const val LIBRARY_VERSION = "__AZORA_LIBRARY_VERSION__"
}
