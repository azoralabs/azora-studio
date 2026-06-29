package dev.azora.canvas.domain

/**
 * One ordered child-slot on a container node (Unreal array-pin style). [childId] references a node in
 * the graph's pool, or `null` for an empty slot the user added but hasn't connected. Several slots
 * (across containers) may reference the same [childId] — the node is then reused and renders once per
 * reference.
 */
data class AzoraNodeSlot(val id: String, val childId: String? = null)

/**
 * How [AzoraSlotGraph] talks to a host's node type [N] without knowing it. The host supplies identity,
 * slot access, an immutable slot-replacer, container detection, and a fresh-slot-id generator. Leaves
 * (non-containers) return `false` from [isContainer] and an empty slot list from [slots].
 */
interface AzoraSlotNodeAdapter<N> {
    fun id(node: N): String
    fun slots(node: N): List<AzoraNodeSlot>
    fun isContainer(node: N): Boolean
    /** A copy of [node] with [slots] (containers only; the graph never calls this on a leaf). */
    fun withSlots(node: N, slots: List<AzoraNodeSlot>): N
    /** A fresh, unique id for a new empty slot. */
    fun newSlotId(): String
    /** A copy of [node] carrying the same content/slots but identified by [id] (for duplication). */
    fun withId(node: N, id: String): N
    /** A fresh, unique id for a new (duplicated) node. */
    fun newNodeId(): String
}

/**
 * Pure operations over a pool-of-nodes-with-slots graph: containers reference children by id through
 * ordered slots, so a node may be reused in several slots. Fully decoupled from any editor's domain
 * types — construct with [nodes] + an [adapter] and call the operations. Immutable: every mutator
 * returns a new [AzoraSlotGraph] (use [.nodes] to read the updated pool).
 *
 * A node not referenced from the root is "free-floating" (staging only). Reachability is cycle-safe:
 * a slot may reference an ancestor without infinite recursion.
 *
 * @param nodes The flat node pool (every node lives here, keyed by id).
 * @param adapter Bridges the generic algorithms to the host's [N] type.
 */
class AzoraSlotGraph<N>(val nodes: List<N>, private val adapter: AzoraSlotNodeAdapter<N>) {

    /** The node with [id], or null. */
    fun node(id: String): N? = nodes.firstOrNull { adapter.id(it) == id }

    /** Resolves a container's slots to their referenced nodes in slot order. Duplicates are kept, so a
     *  node referenced by N slots appears N times (render-once-per-reference). Missing refs are skipped. */
    fun slotChildren(container: N): List<N> =
        adapter.slots(container).mapNotNull { s -> s.childId?.let { node(it) } }

    /** Every node reachable from [rootId] via slots, each exactly once (deduped), cycle-safe. The root
     *  itself is included only if another node references it; pass it to the caller separately if needed. */
    fun reachableFrom(rootId: String): List<N> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<N>()
        fun visit(container: N) {
            if (!adapter.isContainer(container)) return
            adapter.slots(container).forEach { slot ->
                val childId = slot.childId ?: return@forEach
                val child = node(childId) ?: return@forEach
                if (seen.add(adapter.id(child))) {
                    out += child
                    visit(child)
                }
            }
        }
        node(rootId)?.let { visit(it) }
        return out
    }

    /** True iff [id] is reachable from [rootId] via slots (path-based, cycle-safe). */
    fun containsReachable(rootId: String, id: String): Boolean {
        val visiting = HashSet<String>()
        fun visit(container: N): Boolean {
            if (!adapter.isContainer(container)) return false
            return adapter.slots(container).any { slot ->
                val childId = slot.childId
                childId != null && childId !in visiting &&
                    node(childId)?.let { child ->
                        when (childId) {
                            id -> true
                            else -> { visiting += childId; visit(child) }
                        }
                    } == true
            }
        }
        return node(rootId)?.let { visit(it) } == true
    }

    /** Returns a graph with the node [id] replaced by [transform](it) (no-op if absent). Useful for
     *  property edits the graph otherwise knows nothing about. */
    fun replaceNode(id: String, transform: (N) -> N): AzoraSlotGraph<N> =
        AzoraSlotGraph(nodes.map { if (adapter.id(it) == id) transform(it) else it }, adapter)

    /** Sets slot [slotId] on container [containerId] to reference [childId] (or null to clear). The node
     *  is not moved — other slots can still reference it, so this is the basis of reuse. */
    fun setSlotChild(containerId: String, slotId: String, childId: String?): AzoraSlotGraph<N> =
        replaceNode(containerId) { c ->
            if (adapter.isContainer(c)) adapter.withSlots(c, adapter.slots(c).map { s -> if (s.id == slotId) s.copy(childId = childId) else s }) else c
        }

    /** Appends a new empty slot to container [containerId]. */
    fun addSlot(containerId: String): AzoraSlotGraph<N> =
        replaceNode(containerId) { c ->
            if (adapter.isContainer(c)) adapter.withSlots(c, adapter.slots(c) + AzoraNodeSlot(adapter.newSlotId())) else c
        }

    /** Removes slot [slotId] from container [containerId]; remaining slots keep their order. The
     *  referenced node stays in the pool (may still be referenced elsewhere, or become free-floating). */
    fun removeSlot(containerId: String, slotId: String): AzoraSlotGraph<N> =
        replaceNode(containerId) { c ->
            if (adapter.isContainer(c)) adapter.withSlots(c, adapter.slots(c).filterNot { it.id == slotId }) else c
        }

    /** Appends [node] to the pool (free-floating until a slot references it). */
    fun addNode(node: N): AzoraSlotGraph<N> = AzoraSlotGraph(nodes + node, adapter)

    /** Removes node [id] from the pool and nulls any slot references pointing at it (they become empty). */
    fun removeNode(id: String): AzoraSlotGraph<N> =
        AzoraSlotGraph(
            nodes = nodes.filterNot { adapter.id(it) == id }
                .map { c -> if (adapter.isContainer(c)) adapter.withSlots(c, adapter.slots(c).map { s -> if (s.childId == id) s.copy(childId = null) else s }) else c },
            adapter = adapter
        )

    /**
     * Duplicates node [id] under a fresh id (same content; a duplicated container keeps its slot
     * `childId`s, i.e. it shares the original's children) and appends the copy to the pool as a
     * free-floating node. Returns the new graph paired with the new node's id (or `null` if [id] was
     * not found), so the host can select the duplicate.
     */
    fun duplicate(nodeId: String): Pair<AzoraSlotGraph<N>, String?> {
        val original = node(nodeId) ?: return this to null
        val newId = adapter.newNodeId()
        return AzoraSlotGraph(nodes + adapter.withId(original, newId), adapter) to newId
    }
}
