package dev.azora.sdk.compiler.scene.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Visual styling applied to a [SceneComponent].
 *
 * Values are primitive and target-agnostic so the same model can drive any backend (an in-Studio
 * preview, a React generator, a Compose Multiplatform generator, …). Lengths are logical pixels,
 * colors are `#RRGGBB` hex strings; `null`/default fields are simply not emitted by generators.
 */
@Serializable
data class SceneModifier(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val padding: Int? = null,
    val gap: Int? = null,
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val fontSize: Int? = null,
    val fontWeight: SceneFontWeight = SceneFontWeight.NORMAL,
    val textAlign: SceneTextAlign = SceneTextAlign.START,
    /** Per-corner, optionally-elliptical border radius. Legacy uniform radius is read from
     *  [cornerRadius] (kept for backward-compat with older docs); new edits write [corners]. */
    val corners: SceneCornerRadius? = null,
    /** Legacy uniform border radius in logical px (older docs). Prefer [corners]. */
    val cornerRadius: Int? = null,
    /** Border width in logical px (paired with [borderColor]); null = no border. */
    val borderWidth: Int? = null,
    /** Border color as `#RRGGBB` (paired with [borderWidth]). */
    val borderColor: String? = null,
    /** Where the border is drawn relative to the box edge. */
    val borderPosition: SceneBorderPosition = SceneBorderPosition.OUTSIDE,
    /** Opacity as a percentage 0–100; null = fully opaque. */
    val opacity: Int? = null,
    /** Keys of modifiers the user explicitly added (so a card stays even when its value equals the
     *  default, and survives reload). Removed only via the card's × (which also resets the value). */
    val active: List<String> = emptyList()
)

/** One corner of a border radius: [x] = horizontal radius, [y] = vertical radius (logical px). 0 = sharp. */
@Serializable
data class SceneCorner(val x: Int = 0, val y: Int = 0)

/** The four border radii (top-left, top-right, bottom-right, bottom-left), each optionally elliptical. */
@Serializable
data class SceneCornerRadius(
    val topLeft: SceneCorner = SceneCorner(),
    val topRight: SceneCorner = SceneCorner(),
    val bottomRight: SceneCorner = SceneCorner(),
    val bottomLeft: SceneCorner = SceneCorner()
) {
    /** True if every corner is sharp (0/0) — the radius can be omitted from output. */
    fun isZero(): Boolean =
        topLeft.x == 0 && topLeft.y == 0 && topRight.x == 0 && topRight.y == 0 &&
            bottomRight.x == 0 && bottomRight.y == 0 && bottomLeft.x == 0 && bottomLeft.y == 0

    companion object {
        /** Uniform circular radius (same [r] on every corner, x = y). */
        fun uniform(r: Int): SceneCornerRadius =
            SceneCornerRadius(SceneCorner(r, r), SceneCorner(r, r), SceneCorner(r, r), SceneCorner(r, r))
    }
}

/** Where a border is drawn relative to the box edge. */
@Serializable
enum class SceneBorderPosition { INSIDE, OUTSIDE, CENTER }

@Serializable
enum class SceneFontWeight { NORMAL, MEDIUM, SEMI_BOLD, BOLD }

@Serializable
enum class SceneTextAlign { START, CENTER, END }

@Serializable
enum class SceneArrangement { START, CENTER, END, SPACE_BETWEEN }

/** A reroute (waypoint) point on a slot's link, in canvas-local (pre-pan) coordinates — like node
 *  positions. Lets the user bend a link by adding diamonds along it. */
@Serializable
data class SceneReroutePoint(val id: String = randomSlotId(), val x: Float = 0f, val y: Float = 0f)

/**
 * A node in a scene's visual component tree. Nodes live in a flat per-scene pool
 * ([SceneDocument.nodes]); containers reference their children by id through ordered [SceneSlot]s,
 * so the same node can be reused in multiple slots. Backends (preview, generators) walk from
 * [SceneDocument.rootId], resolving slots against the pool.
 */
@Serializable
sealed interface SceneComponent {
    val id: String
    val modifier: SceneModifier
}

/**
 * One ordered out-slot on a container (Unreal array-pin style). [childId] references a node in the
 * scene pool, or `null` for an empty slot added via `+`. Multiple slots (across containers) may
 * reference the same [childId] — the node renders once per reference.
 */
@Serializable
data class SceneSlot(
    val id: String = randomSlotId(),
    val childId: String? = null,
    /** Waypoints on this slot's link, letting the user bend the connection line. */
    val reroutePoints: List<SceneReroutePoint> = emptyList()
)

@Serializable
@SerialName("column")
data class SceneColumn(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier(),
    val arrangement: SceneArrangement = SceneArrangement.START,
    val slots: List<SceneSlot> = emptyList()
) : SceneComponent

@Serializable
@SerialName("row")
data class SceneRow(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier(),
    val arrangement: SceneArrangement = SceneArrangement.START,
    val slots: List<SceneSlot> = emptyList()
) : SceneComponent

@Serializable
@SerialName("box")
data class SceneBox(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier(),
    val slots: List<SceneSlot> = emptyList()
) : SceneComponent

@Serializable
@SerialName("text")
data class SceneText(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier(),
    val text: String = "Text"
) : SceneComponent

@Serializable
@SerialName("button")
data class SceneButton(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier(),
    val label: String = "Button",
    /** Id of a click handler to run, if any (logic emission is not implemented yet). */
    val onClickHandlerId: String? = null
) : SceneComponent

@Serializable
@SerialName("image")
data class SceneImage(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier(),
    val src: String = "",
    val alt: String = ""
) : SceneComponent

@Serializable
@SerialName("link")
data class SceneLink(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier(),
    val text: String = "Link",
    val href: String = "/"
) : SceneComponent

@Serializable
@SerialName("input")
data class SceneInput(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier(),
    val placeholder: String = "",
    /** Optional state key bound to this input. */
    val stateKey: String? = null
) : SceneComponent

@Serializable
@SerialName("spacer")
data class SceneSpacer(
    override val id: String = randomComponentId(),
    override val modifier: SceneModifier = SceneModifier()
) : SceneComponent

/** Generates a short, collision-resistant id for a component. */
fun randomComponentId(): String = "c_" + kotlin.random.Random.nextLong().toString(36)

/** Generates a short id for an empty out-slot on a container (distinct from component ids). */
fun randomSlotId(): String = "s_" + kotlin.random.Random.nextLong().toString(36)
