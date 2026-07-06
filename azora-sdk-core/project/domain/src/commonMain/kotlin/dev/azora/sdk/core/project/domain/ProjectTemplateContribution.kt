package dev.azora.sdk.core.project.domain

import dev.azora.sdk.core.io.FileSystem

/** Builtin template id always available (no plugin required). Every other id is plugin-contributed. */
const val BUILTIN_TEMPLATE_ID_EMPTY = "empty"

/**
 * How the host should execute a contributed run target.
 * - [GRADLE]: invoke [ProjectRunTarget.gradleTask] as a normal (possibly blocking) Gradle task.
 * - [ANDROID]: [ProjectRunTarget.gradleTask] is the install task; the host enumerates Android
 *   devices/emulators, installs, and launches.
 * - [IOS]: the host enumerates iOS simulators and prints guidance (no automated install).
 */
enum class ProjectRunTargetKind { GRADLE, ANDROID, IOS, COMMAND }

/**
 * A runnable target a template contributes (e.g. "Run Website" → Kobweb dev server, "Run" → app).
 *
 * Plugins own their build/run logic: [gradleTask] is invoked by the host to run it. When [stopTask]
 * is set, [gradleTask] is expected to launch a detached, long-running process (e.g. a dev server)
 * that is later shut down with [stopTask]; otherwise [gradleTask] is a blocking task the host kills.
 *
 * [ProjectRunTargetKind.COMMAND] targets instead run [command] via the OS shell in [workingDir]
 * (for non-Gradle stacks such as npm/Vite); the host streams the output and kills the process tree
 * on Stop.
 */
data class ProjectRunTarget(
    val id: String,
    val label: String,
    val gradleTask: String = "",
    val stopTask: String? = null,
    val kind: ProjectRunTargetKind = ProjectRunTargetKind.GRADLE,
    /** [ProjectRunTargetKind.COMMAND]: shell command launched in [workingDir]. */
    val command: String? = null,
    /** [ProjectRunTargetKind.COMMAND]: working directory relative to the project root. */
    val workingDir: String? = null,
)

/**
 * Generates the source/build files for one project template when a project of that template is
 * created (or regenerated). Implementations live in plugins; the host never references them
 * directly — it resolves them through a [ProjectTemplateGeneratorResolver] backed by the plugin manager.
 */
interface ProjectTemplateGenerator {

    /**
     * Scaffold the template into [projectPath] using [fileSystem].
     *
     * @param project The project being created (template-specific defaults already seeded).
     * @param projectPath Absolute/relative root directory of the project.
     * @param fileSystem The host file system used to write files.
     */
    suspend fun generate(project: AzoraProjectModel, projectPath: String, fileSystem: FileSystem)
}

/**
 * A project template contributed by a plugin (or the builtin host template).
 *
 * @property id Stable template identifier stored on the project (e.g. "website"). Persisted with the
 *   project, so it must not change across versions.
 * @property runTargets Build/run targets the host surfaces in its run controls.
 * @property supportsOptionalServer Whether the "Include backend server" toggle applies to this template.
 */
data class ProjectTemplateContribution(
    val id: String,
    val label: String,
    val description: String,
    val accentColor: String = "#FF9C27B0",
    val iconXml: String? = null,
    val supportsOptionalServer: Boolean = false,
    val generator: ProjectTemplateGenerator,
    val runTargets: List<ProjectRunTarget> = emptyList(),
    /**
     * Variant grouping: contributions sharing a [groupId] are presented as ONE
     * template card whose variants are picked from a dropdown (e.g. the Azora
     * Engine "Game" template with Tetris / Runner / Shapes / Empty variants).
     * [groupLabel]/[groupDescription] describe the card; [variantLabel] is the
     * dropdown entry; [isDefaultVariant] marks the initially selected variant.
     */
    val groupId: String? = null,
    val groupLabel: String? = null,
    val groupDescription: String? = null,
    val variantLabel: String? = null,
    val isDefaultVariant: Boolean = false,
)

/**
 * Resolves the [ProjectTemplateGenerator] for a given template id, or `null` if none contributes it.
 *
 * Defined in the domain layer so the data-layer repository can depend on it without coupling to the
 * plugin SDK. The host provides the implementation (builtin templates + the plugin manager).
 */
fun interface ProjectTemplateGeneratorResolver {
    suspend fun generatorFor(templateId: String): ProjectTemplateGenerator?
}
