package dev.azora.sdk.docking.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a node in the dock layout tree.
 *
 * The docking system uses a tree structure where each node can be one of:
 * - [Split]: Divides space between two child nodes (horizontal or vertical)
 * - [TabGroup]: Groups multiple panels as tabs in a single area
 * - [Leaf]: Contains a single panel
 *
 * Example tree structure:
 * ```
 *        Split (VERTICAL)
 *       /              \
 *   Split (HORIZONTAL)  Leaf (console)
 *   /        \
 * Leaf      TabGroup
 * (explorer) (editor1, editor2)
 * ```
 *
 * @see DockLayout for the complete layout configuration
 * @see DockLayoutOperations for manipulating the tree
 */
@Serializable
sealed interface DockNode {

    /** Unique identifier for this node. Used for targeting operations. */
    val id: String

    /**
     * A split node that divides space between two child nodes.
     *
     * @property id Unique identifier for this node
     * @property orientation Direction of the split (HORIZONTAL or VERTICAL)
     * @property first The first child node (left for HORIZONTAL, top for VERTICAL)
     * @property second The second child node (right for HORIZONTAL, bottom for VERTICAL)
     * @property ratio The proportion of space given to [first] (0.0 to 1.0)
     *
     * @see DockOrientation
     */
    @Serializable
    @SerialName("split")
    data class Split(
        override val id: String,
        val orientation: DockOrientation,
        val first: DockNode,
        val second: DockNode,
        val ratio: Float = 0.5f
    ) : DockNode

    /**
     * A tab group that displays multiple panels as tabs.
     *
     * Only one panel is visible at a time, selected by [activeTabIndex].
     *
     * @property id Unique identifier for this node
     * @property panels List of panel IDs in tab order
     * @property activeTabIndex Index of the currently selected tab (0-based)
     */
    @Serializable
    @SerialName("tabGroup")
    data class TabGroup(
        override val id: String,
        val panels: List<String>,
        val activeTabIndex: Int = 0
    ) : DockNode {
        /** The panel ID of the currently active tab, or null if empty. */
        val activePanel: String?
            get() = panels.getOrNull(activeTabIndex)
    }

    /**
     * A leaf node containing a single panel.
     *
     * This is the simplest node type and represents a single panel
     * without any tabs or splits.
     *
     * @property id Unique identifier for this node
     * @property panelId The ID of the panel contained in this leaf
     */
    @Serializable
    @SerialName("leaf")
    data class Leaf(
        override val id: String,
        val panelId: String
    ) : DockNode
}

/**
 * Finds a node by its ID in the tree.
 *
 * @param nodeId The ID of the node to find
 * @return The node with the matching ID, or null if not found
 */
fun DockNode.findNode(nodeId: String): DockNode? {
    if (id == nodeId) return this
    return when (this) {
        is DockNode.Split -> first.findNode(nodeId) ?: second.findNode(nodeId)
        is DockNode.TabGroup -> null
        is DockNode.Leaf -> null
    }
}

/**
 * Finds the node containing a specific panel.
 *
 * @param panelId The ID of the panel to find
 * @return The node containing the panel (TabGroup or Leaf), or null if not found
 */
fun DockNode.findNodeByPanelId(panelId: String): DockNode? {
    return when (this) {
        is DockNode.Split -> first.findNodeByPanelId(panelId) ?: second.findNodeByPanelId(panelId)
        is DockNode.TabGroup -> if (panels.contains(panelId)) this else null
        is DockNode.Leaf -> if (this.panelId == panelId) this else null
    }
}

/**
 * Collects all panel IDs contained in this node and its children.
 *
 * @return A set of all panel IDs in the subtree
 */
fun DockNode.collectPanelIds(): Set<String> {
    return when (this) {
        is DockNode.Split -> first.collectPanelIds() + second.collectPanelIds()
        is DockNode.TabGroup -> panels.toSet()
        is DockNode.Leaf -> setOf(panelId)
    }
}

/**
 * Applies a transformation to a node with the specified ID.
 *
 * @param nodeId The ID of the node to transform
 * @param transform The transformation function to apply
 * @return A new tree with the transformation applied
 */
fun DockNode.updateNode(nodeId: String, transform: (DockNode) -> DockNode): DockNode {
    if (id == nodeId) return transform(this)
    return when (this) {
        is DockNode.Split -> copy(
            first = first.updateNode(nodeId, transform),
            second = second.updateNode(nodeId, transform)
        )
        else -> this
    }
}

/**
 * Removes a panel from the tree, cleaning up empty nodes.
 *
 * - If a TabGroup becomes empty, it's removed
 * - If a TabGroup has one panel left, it becomes a Leaf
 * - If a Split loses one child, the remaining child replaces it
 *
 * @param panelId The ID of the panel to remove
 * @return The modified tree, or null if the tree becomes empty
 */
fun DockNode.removePanel(panelId: String): DockNode? {
    return when (this) {
        is DockNode.Split -> {
            val newFirst = first.removePanel(panelId)
            val newSecond = second.removePanel(panelId)
            when {
                newFirst == null && newSecond == null -> null
                newFirst == null -> newSecond
                newSecond == null -> newFirst
                else -> copy(first = newFirst, second = newSecond)
            }
        }
        is DockNode.TabGroup -> {
            val newPanels = panels.filter { it != panelId }
            when {
                newPanels.isEmpty() -> null
                newPanels.size == 1 -> DockNode.Leaf(id = id, panelId = newPanels.first())
                else -> copy(
                    panels = newPanels,
                    activeTabIndex = activeTabIndex.coerceIn(0, newPanels.size - 1)
                )
            }
        }
        is DockNode.Leaf -> if (this.panelId == panelId) null else this
    }
}