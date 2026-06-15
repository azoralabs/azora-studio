package dev.azora.canvas.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Layout constants for ports inside node bodies.
 *
 * The canvas renders ports inline with header content (Unreal Blueprint style) rather than on a
 * separate gutter, so the canvas needs to know up-front where each port's center will fall in
 * order to anchor links accurately before the ports report their measured positions.
 *
 * The constants here describe a vertical stack of fixed-height port rows. Most values are
 * expressed in dp; use [toPixels] (or the [rememberPortLayoutPx] helper) to get a density-resolved
 * [AzoraPortLayoutPx] when drawing.
 *
 * Three node shapes are pre-tabulated with their own top-padding offsets because they have
 * different header chrome:
 * - **ScreenNode** - plain header, ports start lower.
 * - **IfNode** - taller header (12 dp), fixed body width, used for branching control.
 * - **RootNode** - title-only, smallest header.
 */
object AzoraPortLayout {

    /** Default width of node body */
    const val NODE_BODY_WIDTH_DP = 160f

    /** Port size */
    const val PORT_SIZE_DP = 16f

    /** Port row height: port (16dp) + vertical padding (2dp top + 2dp bottom) = 20dp */
    const val PORT_ROW_HEIGHT_DP = 20f

    /** Horizontal padding: wrapper padding (4dp) + half port size (8dp) */
    const val PORT_HORIZONTAL_PADDING_DP = 12f

    // ScreenNode layout: Row with 8dp vertical padding + port row with 2dp vertical padding
    /** ScreenNode: vertical padding from top to first port center */
    const val SCREEN_PORT_TOP_PADDING_DP = 18f  // 8dp Row padding + 2dp port row padding + 8dp half port

    // IfNode layout: 12dp header + Row with 4dp vertical padding + port row with 2dp vertical padding
    /** IfNode: vertical padding from top to first port center */
    const val IF_PORT_TOP_PADDING_DP = 26f  // 12dp header + 4dp Row padding + 2dp port row padding + 8dp half port
    /** IfNode: fixed width */
    const val IF_NODE_WIDTH_DP = 180f

    // RootNode layout: Text with 10dp vertical padding + port row with 2dp vertical padding
    /** RootNode: vertical padding from top to first port center */
    const val ROOT_PORT_TOP_PADDING_DP = 20f  // 10dp text padding + 2dp port row padding + 8dp half port

    /**
     * Calculate INPUT port center X position.
     * Port center is at wrapper padding (4dp) + half port size (8dp) = 12dp from left edge.
     */
    fun getInputPortCenterX(): Float = PORT_HORIZONTAL_PADDING_DP

    /**
     * Calculate OUTPUT port center X position for a given node body width.
     * Port center is at nodeWidth - 12dp from left edge.
     */
    fun getOutputPortCenterX(nodeBodyWidth: Float): Float = nodeBodyWidth - PORT_HORIZONTAL_PADDING_DP

    /** Y center (dp) of the [index]-th output port on a ScreenNode, measured from the node's top edge. */
    fun getScreenOutputPortY(index: Int): Float =
        SCREEN_PORT_TOP_PADDING_DP + (index * PORT_ROW_HEIGHT_DP)

    /** Y center (dp) of the single input port on a ScreenNode. */
    fun getScreenInputPortY(): Float = SCREEN_PORT_TOP_PADDING_DP

    /** Y center (dp) of the [index]-th output port on an IfNode (true=0, false=1). */
    fun getIfOutputPortY(index: Int): Float =
        IF_PORT_TOP_PADDING_DP + (index * PORT_ROW_HEIGHT_DP)

    /** Y center (dp) of the single input port on an IfNode. */
    fun getIfInputPortY(): Float = IF_PORT_TOP_PADDING_DP

    /** Y center (dp) of the [index]-th output port on a RootNode. */
    fun getRootOutputPortY(index: Int): Float =
        ROOT_PORT_TOP_PADDING_DP + (index * PORT_ROW_HEIGHT_DP)

    // Root node layout constants
    /** X position of root output port: nodeWidth - PORT_HORIZONTAL_PADDING_DP */
    const val ROOT_OUTPUT_PORT_X_DP = 92f  // 100dp body - 8dp padding
    /** Y position of root output port */
    const val ROOT_OUTPUT_PORT_Y_DP = 12f  // 4dp padding + 8dp half port
    /** Y start position for MENU root outputs */
    const val MENU_ROOT_OUTPUT_START_Y_DP = 12f
    /** Spacing between MENU root outputs */
    const val MENU_ROOT_OUTPUT_SPACING_DP = 20f

    /**
     * Resolve every dp constant in this object to pixels for the given [density]. Prefer
     * [rememberPortLayoutPx] inside composables - it caches the result per density change.
     */
    fun toPixels(density: Density): AzoraPortLayoutPx = with(density) {
        AzoraPortLayoutPx(
            density = density,
            portHorizontalPadding = PORT_HORIZONTAL_PADDING_DP.dp.toPx(),
            portRowHeight = PORT_ROW_HEIGHT_DP.dp.toPx(),
            portSize = PORT_SIZE_DP.dp.toPx(),
            screenPortTopPadding = SCREEN_PORT_TOP_PADDING_DP.dp.toPx(),
            ifPortTopPadding = IF_PORT_TOP_PADDING_DP.dp.toPx(),
            ifNodeWidth = IF_NODE_WIDTH_DP.dp.toPx(),
            rootPortTopPadding = ROOT_PORT_TOP_PADDING_DP.dp.toPx(),
            rootOutputPortX = ROOT_OUTPUT_PORT_X_DP.dp.toPx(),
            rootOutputPortY = ROOT_OUTPUT_PORT_Y_DP.dp.toPx(),
            menuRootOutputStartY = MENU_ROOT_OUTPUT_START_Y_DP.dp.toPx(),
            menuRootOutputSpacing = MENU_ROOT_OUTPUT_SPACING_DP.dp.toPx()
        )
    }
}

/**
 * Density-resolved counterpart to [AzoraPortLayout]. All values are in pixels.
 *
 * Construct via [AzoraPortLayout.toPixels] or [rememberPortLayoutPx]. The wrapped [density] is
 * retained so methods that need an extra dp value at call time (e.g. [getOutputPortCenterX]) can
 * convert without re-reading `LocalDensity`.
 */
data class AzoraPortLayoutPx(
    val density: Density,
    val portHorizontalPadding: Float,
    val portRowHeight: Float,
    val portSize: Float,
    // Node-type-specific top padding
    val screenPortTopPadding: Float,
    val ifPortTopPadding: Float,
    val ifNodeWidth: Float,
    val rootPortTopPadding: Float,
    // Root node layout
    val rootOutputPortX: Float,
    val rootOutputPortY: Float,
    val menuRootOutputStartY: Float,
    val menuRootOutputSpacing: Float
) {
    /** X center (px) of an input port; mirrors [AzoraPortLayout.getInputPortCenterX]. */
    fun getInputPortCenterX(): Float = portHorizontalPadding

    /**
     * X center (px) of an output port for a node whose body is [nodeBodyWidthDp] wide. The width
     * is taken in dp because nodes size themselves dynamically (see
     * [dev.azora.canvas.domain.model.node.AzoraNodeModel.calculateWidth]).
     */
    fun getOutputPortCenterX(nodeBodyWidthDp: Float): Float = with(density) {
        AzoraPortLayout.getOutputPortCenterX(nodeBodyWidthDp).dp.toPx()
    }

    /** Y center (px) of the [index]-th output port on a ScreenNode. */
    fun getScreenOutputPortY(index: Int): Float =
        screenPortTopPadding + (index * portRowHeight)

    /** Y center (px) of a ScreenNode's input port. */
    fun getScreenInputPortY(): Float = screenPortTopPadding

    /** Y center (px) of the [index]-th output port on an IfNode (true=0, false=1). */
    fun getIfOutputPortY(index: Int): Float =
        ifPortTopPadding + (index * portRowHeight)

    /** Y center (px) of an IfNode's input port. */
    fun getIfInputPortY(): Float = ifPortTopPadding

    /** X center (px) of an IfNode's output port; uses the fixed [AzoraPortLayout.IF_NODE_WIDTH_DP]. */
    fun getIfOutputPortCenterX(): Float = with(density) {
        AzoraPortLayout.getOutputPortCenterX(AzoraPortLayout.IF_NODE_WIDTH_DP).dp.toPx()
    }

    /** Y center (px) of the [index]-th output port on a RootNode. */
    fun getRootOutputPortY(index: Int): Float =
        rootPortTopPadding + (index * portRowHeight)
}

/**
 * Composable wrapper around [AzoraPortLayout.toPixels] that caches the result by current
 * [LocalDensity]. Use this from canvas composables instead of resolving manually.
 */
@Composable
fun rememberPortLayoutPx(): AzoraPortLayoutPx {
    val density = LocalDensity.current
    return remember(density) { AzoraPortLayout.toPixels(density) }
}
