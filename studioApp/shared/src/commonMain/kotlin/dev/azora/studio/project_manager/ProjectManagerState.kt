package dev.azora.studio.project_manager

import dev.azora.sdk.core.presentation.util.*
import dev.azora.sdk.core.project.domain.BUILTIN_TEMPLATE_ID_EMPTY

/**
 * A project template the user can pick when creating a project.
 *
 * [templateId] is a stable string id (e.g. "empty", "website") — the builtin "empty" is always
 * available; everything else is contributed by an installed plugin.
 */
data class AvailableTemplate(
    val templateId: String,
    val label: String,
    val description: String,
    val pluginId: String? = null,
    val supportsOptionalServer: Boolean = false,
    /**
     * Selectable variants of this template (e.g. Game → Tetris / Temple Run /
     * Shapes / Empty). When non-empty, [templateId] is the default variant and
     * the create-project dialog shows a dropdown; the chosen variant's id is
     * what gets stored on the project.
     */
    val variants: List<TemplateVariant> = emptyList(),
) {
    /** True when [id] selects this card (directly or via one of its variants). */
    fun matches(id: String): Boolean =
        templateId == id || variants.any { it.templateId == id }

    companion object {
        /** The single builtin template, always available. */
        val EMPTY = AvailableTemplate(
            templateId = BUILTIN_TEMPLATE_ID_EMPTY,
            label = "Empty",
            description = "Blank project with no predefined targets",
            pluginId = null
        )
    }
}

/** One dropdown entry of an [AvailableTemplate] with variants. */
data class TemplateVariant(
    val templateId: String,
    val label: String,
    val description: String,
)

data class ProjectManagerState(
    val projectName: FieldState<String> = FieldState.textField("NewProject"),
    val companyName: FieldState<String> = FieldState.textField("MyCompany"),
    val packageName: FieldState<String> = FieldState.textField("com.mycompany.newproject"),
    val template: String = BUILTIN_TEMPLATE_ID_EMPTY,
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