package dev.azora.sdk.core.project.domain.website

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Visual styling applied to a [WebComponent].
 *
 * Values are intentionally primitive and framework-agnostic so the same model can drive both the
 * Kobweb code generator and the in-Studio virtual renderer (Phase 2). Lengths are expressed in CSS
 * pixels and colors as `#RRGGBB` hex strings; `null`/default fields are simply not emitted.
 */
@Serializable
data class WebModifier(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val padding: Int? = null,
    val gap: Int? = null,
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val fontSize: Int? = null,
    val fontWeight: WebFontWeight = WebFontWeight.NORMAL,
    val textAlign: WebTextAlign = WebTextAlign.START
)

@Serializable
enum class WebFontWeight { NORMAL, MEDIUM, SEMI_BOLD, BOLD }

@Serializable
enum class WebTextAlign { START, CENTER, END }

@Serializable
enum class WebArrangement { START, CENTER, END, SPACE_BETWEEN }

/**
 * A node in a page's visual component tree.
 *
 * Each variant is a small, serializable description of a widget. The generator walks this tree to
 * emit Kobweb/Silk composables, and the virtual renderer (Phase 2) walks the same tree to draw a
 * WYSIWYG preview — keeping both paths driven by one source of truth.
 */
@Serializable
sealed interface WebComponent {
    val id: String
    val modifier: WebModifier
}

@Serializable
@SerialName("column")
data class WebColumn(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier(),
    val arrangement: WebArrangement = WebArrangement.START,
    val children: List<WebComponent> = emptyList()
) : WebComponent

@Serializable
@SerialName("row")
data class WebRow(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier(),
    val arrangement: WebArrangement = WebArrangement.START,
    val children: List<WebComponent> = emptyList()
) : WebComponent

@Serializable
@SerialName("box")
data class WebBox(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier(),
    val children: List<WebComponent> = emptyList()
) : WebComponent

@Serializable
@SerialName("text")
data class WebText(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier(),
    val text: String = "Text"
) : WebComponent

@Serializable
@SerialName("button")
data class WebButton(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier(),
    val label: String = "Button",
    /** Id of a [WebEventHandler] in the page's [WebLogicGraph] to run on click, if any. */
    val onClickHandlerId: String? = null
) : WebComponent

@Serializable
@SerialName("image")
data class WebImage(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier(),
    val src: String = "",
    val alt: String = ""
) : WebComponent

@Serializable
@SerialName("link")
data class WebLink(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier(),
    val text: String = "Link",
    val href: String = "/"
) : WebComponent

@Serializable
@SerialName("input")
data class WebInput(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier(),
    val placeholder: String = "",
    /** State key bound to this input (declared in [WebsitePage.stateKeys]). */
    val stateKey: String? = null
) : WebComponent

@Serializable
@SerialName("spacer")
data class WebSpacer(
    override val id: String = randomComponentId(),
    override val modifier: WebModifier = WebModifier()
) : WebComponent

/** Generates a short, collision-resistant id for a component. */
fun randomComponentId(): String = "c_" + kotlin.random.Random.nextLong().toString(36)
