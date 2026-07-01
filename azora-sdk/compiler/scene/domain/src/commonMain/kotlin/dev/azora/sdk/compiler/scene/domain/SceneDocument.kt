package dev.azora.sdk.compiler.scene.domain

import kotlinx.serialization.Serializable

/** A position on the node-editor canvas, in canvas-local (pre-pan) coordinates. */
@Serializable
data class CanvasPoint(val x: Float = 0f, val y: Float = 0f)

/** One navigation entry (label + route). */
@Serializable
data class NavLink(val label: String, val route: String)

/**
 * Contents of an `.azn` scene file. The top-level [type] is the generic discriminator Studio reads
 * to route the file to the plugin that owns it — the discriminator *values* are plugin-owned, this
 * model never interprets them.
 *
 * - Page/component-like scenes: a flat pool of [nodes] (+ canvas [positions] and component
 *   [instances], keyed anchor-node-id → component name) with [rootId] naming the root container.
 *   Containers reference children by id through ordered slots (see [SceneSlot]); a node not
 *   referenced from [rootId] is free-floating and never reaches a generator/preview until a slot
 *   references it. Pages also carry a [route].
 * - Navigation scenes: the [nav] entries.
 * - Config scenes: string [settings] (e.g. `title`, `themeColor`).
 */
@Serializable
data class SceneDocument(
    val type: String = "",
    val name: String = "",
    val route: String = "/",
    val nodes: List<SceneComponent> = emptyList(),
    val rootId: String = "",
    val positions: Map<String, CanvasPoint> = emptyMap(),
    val instances: Map<String, String> = emptyMap(),
    val nav: List<NavLink> = emptyList(),
    val settings: Map<String, String> = emptyMap()
) {
    companion object {
        /** A new doc of [type] with a single root [SceneColumn] in the pool (referenced by
         *  [SceneDocument.rootId]); children are then added as slots referencing pool nodes. */
        fun withRoot(type: String, name: String = "", route: String = "/"): SceneDocument {
            val root = SceneColumn()
            return SceneDocument(type = type, name = name, route = route, nodes = listOf(root), rootId = root.id)
        }
    }
}
