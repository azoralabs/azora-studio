package dev.azora.sdk.core.project.domain.website

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Codegen-facing logic intermediate representation (IR).
 *
 * This IR is deliberately independent of the AzoraNodes (`.azn`) canvas model so that
 * `project/domain` stays decoupled from the canvas module. Phase 2 maps an authored `.azn`
 * [graph][dev.azora.canvas.domain.model.AzoraGraphModel] into this IR in a layer that may depend on
 * canvas; the generator only ever sees [WebLogicGraph].
 */
@Serializable
data class WebLogicGraph(
    val handlers: List<WebEventHandler> = emptyList()
) {
    fun handler(id: String?): WebEventHandler? =
        if (id == null) null else handlers.firstOrNull { it.id == id }
}

@Serializable
enum class WebEventType { CLICK, SUBMIT, LOAD }

/**
 * A named sequence of [actions][WebAction] triggered by an [event]. Components reference a handler
 * by [id] (e.g. [WebButton.onClickHandlerId]).
 */
@Serializable
data class WebEventHandler(
    val id: String = randomComponentId(),
    val name: String = "handler",
    val event: WebEventType = WebEventType.CLICK,
    val actions: List<WebAction> = emptyList()
)

/** A single step performed when a [WebEventHandler] fires. */
@Serializable
sealed interface WebAction

/** Navigate the SPA router to [route]. */
@Serializable
@SerialName("navigate")
data class NavigateAction(val route: String) : WebAction

/**
 * Call a generated API endpoint by [endpointId]. The decoded response is bound to
 * [resultStateKey] when provided (the key must be declared in [WebsitePage.stateKeys]).
 */
@Serializable
@SerialName("callApi")
data class CallApiAction(
    val endpointId: String,
    val resultStateKey: String? = null
) : WebAction

/** Assign a literal [value] to a page state variable [stateKey]. */
@Serializable
@SerialName("setState")
data class SetStateAction(
    val stateKey: String,
    val value: String
) : WebAction

/** Write [message] to the browser console. */
@Serializable
@SerialName("log")
data class LogAction(val message: String) : WebAction
