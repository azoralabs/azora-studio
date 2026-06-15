package org.azora.studio.settings

import org.azora.sdk.core.project.domain.ProjectSettings
import kotlinx.serialization.json.*

private const val KEY_SCENE_STUDIO_USE_KMP = "sceneStudioUseKmp"

val ProjectSettings.sceneStudioUseKmp: Boolean
    get() = (extras[KEY_SCENE_STUDIO_USE_KMP] as? JsonPrimitive)?.booleanOrNull ?: false

fun ProjectSettings.withSceneStudioUseKmp(useKmp: Boolean): ProjectSettings {
    val newExtras = JsonObject(extras.toMutableMap().apply {
        put(KEY_SCENE_STUDIO_USE_KMP, JsonPrimitive(useKmp))
    })
    return copy(extras = newExtras)
}
