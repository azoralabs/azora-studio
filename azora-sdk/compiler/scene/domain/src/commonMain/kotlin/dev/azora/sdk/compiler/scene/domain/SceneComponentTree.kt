package dev.azora.sdk.compiler.scene.domain

import dev.azora.canvas.domain.AzoraNodeSlot
import dev.azora.canvas.domain.AzoraSlotGraph
import dev.azora.canvas.domain.AzoraSlotNodeAdapter

/**
 * Operations over a scene's flat node pool ([SceneDocument.nodes]). The pool/slot algorithms (resolve,
 * set/add/remove slot, add/remove node, reachable traversal) delegate to the generic SDK
 * [AzoraSlotGraph]; this object only contributes the [SceneComponent]-specific bits: which variants are
 * containers, how to read/replace their [SceneSlot]s, type labels, and the palette factory.
 *
 * Containers reference children by id through ordered slots, so the same node may be referenced by
 * several slots. A node is "free-floating" when no slot reachable from the root references it.
 */
object SceneComponentTree {

    /** Bridges the generic [AzoraSlotGraph] to [SceneComponent] (maps [SceneSlot] ↔ [AzoraNodeSlot]).
     *  [withSlots] merges by slot id so slot-only edits preserve each slot's reroutePoints (the generic
     *  [AzoraNodeSlot] carries only id/childId). */
    private val slotAdapter = object : AzoraSlotNodeAdapter<SceneComponent> {
        override fun id(node: SceneComponent): String = node.id
        override fun slots(node: SceneComponent): List<AzoraNodeSlot> =
            slotsOf(node).map { AzoraNodeSlot(it.id, it.childId) }
        override fun isContainer(node: SceneComponent): Boolean = this@SceneComponentTree.isContainer(node)
        override fun withSlots(node: SceneComponent, slots: List<AzoraNodeSlot>): SceneComponent {
            val byId = slotsOf(node).associateBy { it.id }
            return this@SceneComponentTree.withSlots(node, slots.map { s ->
                SceneSlot(s.id, s.childId, byId[s.id]?.reroutePoints ?: emptyList())
            })
        }
        override fun newSlotId(): String = randomSlotId()
        override fun withId(node: SceneComponent, id: String): SceneComponent = node.withNodeId(id)
        override fun newNodeId(): String = randomComponentId()
    }

    /** Copy of [this] with a different id (used by duplication). */
    private fun SceneComponent.withNodeId(newId: String): SceneComponent = when (this) {
        is SceneColumn -> copy(id = newId); is SceneRow -> copy(id = newId); is SceneBox -> copy(id = newId)
        is SceneText -> copy(id = newId); is SceneButton -> copy(id = newId); is SceneImage -> copy(id = newId)
        is SceneLink -> copy(id = newId); is SceneInput -> copy(id = newId); is SceneSpacer -> copy(id = newId)
    }

    private fun graph(nodes: List<SceneComponent>): AzoraSlotGraph<SceneComponent> = AzoraSlotGraph(nodes, slotAdapter)

    fun byId(nodes: List<SceneComponent>, id: String): SceneComponent? = graph(nodes).node(id)

    fun isContainer(c: SceneComponent): Boolean = c is SceneColumn || c is SceneRow || c is SceneBox

    fun slotsOf(c: SceneComponent): List<SceneSlot> = when (c) {
        is SceneColumn -> c.slots
        is SceneRow -> c.slots
        is SceneBox -> c.slots
        else -> emptyList()
    }

    fun withSlots(c: SceneComponent, slots: List<SceneSlot>): SceneComponent = when (c) {
        is SceneColumn -> c.copy(slots = slots)
        is SceneRow -> c.copy(slots = slots)
        is SceneBox -> c.copy(slots = slots)
        else -> c
    }

    /** Replaces the node with `id` via [transform], leaving the rest of the pool untouched. */
    fun replaceNode(nodes: List<SceneComponent>, id: String, transform: (SceneComponent) -> SceneComponent): List<SceneComponent> =
        graph(nodes).replaceNode(id, transform).nodes

    /** Wires slot `slotId` on container `containerId` to reference `childId` (the core of drag-connect
     *  and reuse: the node isn't moved, so other slots can still reference it). */
    fun setSlotChild(nodes: List<SceneComponent>, containerId: String, slotId: String, childId: String?): List<SceneComponent> =
        graph(nodes).setSlotChild(containerId, slotId, childId).nodes

    /** Appends a new empty slot to container `containerId` (the `+` affordance). */
    fun addSlot(nodes: List<SceneComponent>, containerId: String): List<SceneComponent> =
        graph(nodes).addSlot(containerId).nodes

    /** Removes slot `slotId` from container `containerId` (right-click delete; remaining slots keep order). */
    fun removeSlot(nodes: List<SceneComponent>, containerId: String, slotId: String): List<SceneComponent> =
        graph(nodes).removeSlot(containerId, slotId).nodes

    /** Appends a new node to the pool (free-floating until a slot references it). */
    fun addNode(nodes: List<SceneComponent>, node: SceneComponent): List<SceneComponent> =
        graph(nodes).addNode(node).nodes

    /** Removes node `id` from the pool and clears any slot references pointing at it (they become empty). */
    fun removeNode(nodes: List<SceneComponent>, id: String): List<SceneComponent> =
        graph(nodes).removeNode(id).nodes

    /** Resolves a container's slots to their referenced nodes in pool order (duplicates kept, so a node
     *  referenced by N slots appears N times). Missing refs are skipped. */
    fun slotChildren(nodes: List<SceneComponent>, c: SceneComponent): List<SceneComponent> =
        graph(nodes).slotChildren(c)

    /** Every node reachable from [rootId] via slots, each once (deduped, cycle-safe). */
    fun reachableFrom(nodes: List<SceneComponent>, rootId: String): List<SceneComponent> =
        graph(nodes).reachableFrom(rootId)

    /** Duplicates node [id] (same content; a container copy shares the original's children) into a
     *  free-floating pool node. Returns the new pool + the new node's id (null if [id] not found). */
    fun duplicate(nodes: List<SceneComponent>, id: String): Pair<List<SceneComponent>, String?> {
        val (g, newId) = graph(nodes).duplicate(id)
        return g.nodes to newId
    }

    // --- Reroute points (waypoints on a slot's link) — scene-specific, live on the slot. ---

    fun addReroute(
        nodes: List<SceneComponent>, containerId: String, slotId: String,
        point: SceneReroutePoint, insertIndex: Int
    ): List<SceneComponent> = replaceNode(nodes, containerId) { c ->
        if (!isContainer(c)) c else withSlots(c, slotsOf(c).map { s ->
            if (s.id != slotId) s else {
                val pts = s.reroutePoints.toMutableList()
                pts.add(insertIndex.coerceIn(0, pts.size), point)
                s.copy(reroutePoints = pts)
            }
        })
    }

    fun removeReroute(nodes: List<SceneComponent>, containerId: String, slotId: String, rerouteId: String): List<SceneComponent> =
        replaceNode(nodes, containerId) { c ->
            if (!isContainer(c)) c else withSlots(c, slotsOf(c).map { s ->
                if (s.id != slotId) s else s.copy(reroutePoints = s.reroutePoints.filterNot { it.id == rerouteId })
            })
        }

    fun moveReroute(
        nodes: List<SceneComponent>, containerId: String, slotId: String,
        rerouteId: String, dx: Float, dy: Float
    ): List<SceneComponent> = replaceNode(nodes, containerId) { c ->
        if (!isContainer(c)) c else withSlots(c, slotsOf(c).map { s ->
            if (s.id != slotId) s else s.copy(reroutePoints = s.reroutePoints.map { p ->
                if (p.id != rerouteId) p else p.copy(x = p.x + dx, y = p.y + dy)
            })
        })
    }

    fun typeLabel(c: SceneComponent): String = when (c) {
        is SceneColumn -> "Column"
        is SceneRow -> "Row"
        is SceneBox -> "Box"
        is SceneText -> "Text"
        is SceneButton -> "Button"
        is SceneImage -> "Image"
        is SceneLink -> "Link"
        is SceneInput -> "Input"
        is SceneSpacer -> "Spacer"
    }

    fun summary(c: SceneComponent): String = when (c) {
        is SceneText -> c.text
        is SceneButton -> c.label
        is SceneLink -> "${c.text} → ${c.href}"
        is SceneImage -> c.alt.ifBlank { c.src }
        is SceneInput -> c.placeholder
        is SceneColumn -> "${slotsOf(c).count { it.childId != null }} child(ren)"
        is SceneRow -> "${slotsOf(c).count { it.childId != null }} child(ren)"
        is SceneBox -> "${slotsOf(c).count { it.childId != null }} child(ren)"
        is SceneSpacer -> ""
    }

    fun create(kind: ComponentKind): SceneComponent = when (kind) {
        ComponentKind.COLUMN -> SceneColumn()
        ComponentKind.ROW -> SceneRow()
        ComponentKind.BOX -> SceneBox()
        ComponentKind.TEXT -> SceneText(text = "Text")
        ComponentKind.BUTTON -> SceneButton(label = "Button")
        ComponentKind.IMAGE -> SceneImage(src = "", alt = "image")
        ComponentKind.LINK -> SceneLink(text = "Link", href = "/")
        ComponentKind.INPUT -> SceneInput(placeholder = "Enter…")
        ComponentKind.SPACER -> SceneSpacer()
    }
}

/** The palette of component types that can be added on a tree canvas. */
enum class ComponentKind(val label: String) {
    COLUMN("Column"), ROW("Row"), BOX("Box"), TEXT("Text"),
    BUTTON("Button"), IMAGE("Image"), LINK("Link"), INPUT("Input"), SPACER("Spacer")
}
