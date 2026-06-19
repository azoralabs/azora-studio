package dev.azora.studio.project_manager

import dev.azora.sdk.core.presentation.util.*
import dev.azora.sdk.core.project.domain.ProjectTemplate

/**
 * A project template the user can pick when creating a project.
 *
 * [template] is always one of the builtin [ProjectTemplate] enum entries (so the persisted project
 * model is untouched); [pluginId] is non-null when the template is contributed by an installed
 * plugin (otherwise it's the builtin Empty template).
 */
data class AvailableTemplate(
    val template: ProjectTemplate,
    val label: String,
    val description: String,
    val pluginId: String?
) {
    companion object {
        /** The single builtin template, always available. */
        val EMPTY = AvailableTemplate(
            template = ProjectTemplate.EMPTY,
            label = ProjectTemplate.EMPTY.label,
            description = ProjectTemplate.EMPTY.description,
            pluginId = null
        )
    }
}

data class ProjectManagerState(
    val projectName: FieldState<String> = FieldState.textField("NewProject"),
    val companyName: FieldState<String> = FieldState.textField("MyCompany"),
    val packageName: FieldState<String> = FieldState.textField("com.mycompany.newproject"),
    val template: ProjectTemplate = ProjectTemplate.EMPTY,
    /** Templates the user can currently pick: builtin Empty + any contributed by enabled plugins. */
    val availableTemplates: List<AvailableTemplate> = listOf(AvailableTemplate.EMPTY),
    val includeServer: Boolean = false,
    val platforms: PlatformSelection = PlatformSelection(),
    val creating: ProcessState = ProcessState(),
    val opening: ProcessState = ProcessState(),
    val deleting: ProcessState = ProcessState()
)

data class PlatformSelection(
    val android: Boolean = true,
    val ios: Boolean = true,
    val desktop: Boolean = false,
    val webWasm: Boolean = false,
    val webReact: Boolean = false,
    val server: Boolean = false
)