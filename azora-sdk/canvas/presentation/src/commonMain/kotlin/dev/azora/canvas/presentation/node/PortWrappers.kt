package dev.azora.canvas.presentation.node

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp

/**
 * Layout slot for left-aligned input port rows inside an [AzoraNode]'s header content.
 *
 * Stacks its [ports] vertically with a small leading padding so port icons line up with the
 * x position the canvas uses to anchor links. Pair with [OutputPortsWrapper] on the opposite side
 * of the node body.
 */
@Composable
fun InputPortsWrapper(
    ports: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(start = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ports()
    }
}

/**
 * Layout slot for right-aligned output port rows inside an [AzoraNode]'s header content.
 *
 * Mirror of [InputPortsWrapper] with end-aligned content and trailing padding.
 */
@Composable
fun OutputPortsWrapper(
    ports: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(end = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        ports()
    }
}