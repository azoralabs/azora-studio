package dev.azora.sdk.color

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class HueWheelWithTriangleTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun mouseDragOverTriangleEmitsContinuousUpdates() {
        val updates = mutableListOf<Pair<Float, Float>>()

        rule.setContent {
            HueWheelWithTriangle(
                hue = 0f,
                saturation = 0.5f,
                brightness = 0.75f,
                onHueChanged = {},
                onSaturationBrightnessChanged = { s, b -> updates += s to b },
                modifier = Modifier.size(180.dp).testTag("wheel")
            )
        }

        // Press at the center of the wheel (inside the triangle) and drag in small steps
        rule.onNodeWithTag("wheel").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(12f, 6f))
            moveBy(Offset(12f, 6f))
            moveBy(Offset(12f, 6f))
            moveBy(Offset(-20f, -10f))
            moveBy(Offset(-20f, -10f))
            release()
        }
        rule.waitForIdle()

        assertTrue(
            "expected continuous saturation/brightness updates during drag, got ${updates.size}: $updates",
            updates.size >= 3
        )
    }

    @Test
    fun mouseDragOverTriangleWorksWithStateFeedbackInsideScrollableColumn() {
        // Mimics the settings screen: picker inside a scrollable column with a
        // clear-focus clickable root, and drag callbacks fed back into the
        // picker's saturation/brightness parameters (as ColorPicker does)
        val updates = mutableListOf<Pair<Float, Float>>()

        rule.setContent {
            var sat by remember { mutableStateOf(0.5f) }
            var bright by remember { mutableStateOf(0.75f) }

            Column(
                modifier = Modifier
                    .size(400.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .verticalScroll(rememberScrollState())
            ) {
                HueWheelWithTriangle(
                    hue = 0f,
                    saturation = sat,
                    brightness = bright,
                    onHueChanged = {},
                    onSaturationBrightnessChanged = { s, b ->
                        sat = s
                        bright = b
                        updates += s to b
                    },
                    modifier = Modifier.size(180.dp).testTag("wheel")
                )
            }
        }

        rule.onNodeWithTag("wheel", useUnmergedTree = true).performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(12f, 6f))
            moveBy(Offset(12f, 6f))
            moveBy(Offset(12f, 6f))
            moveBy(Offset(-20f, -10f))
            moveBy(Offset(-20f, -10f))
            release()
        }
        rule.waitForIdle()

        assertTrue(
            "expected continuous updates during drag, got ${updates.size}: $updates",
            updates.size >= 3
        )
    }

    @Test
    fun mouseDragOverWheelRingEmitsContinuousHueUpdates() {
        val hueUpdates = mutableListOf<Float>()

        rule.setContent {
            HueWheelWithTriangle(
                hue = 0f,
                saturation = 0.5f,
                brightness = 0.75f,
                onHueChanged = { hueUpdates += it },
                onSaturationBrightnessChanged = { _, _ -> },
                modifier = Modifier.size(180.dp).testTag("wheel")
            )
        }

        // Press on the ring (right edge, between inner and outer radius) and drag along it
        rule.onNodeWithTag("wheel").performMouseInput {
            moveTo(Offset(center.x + 82f, center.y))
            press()
            moveBy(Offset(-2f, 14f))
            moveBy(Offset(-2f, 14f))
            moveBy(Offset(-4f, 14f))
            release()
        }
        rule.waitForIdle()

        assertTrue(
            "expected continuous hue updates during drag, got ${hueUpdates.size}: $hueUpdates",
            hueUpdates.size >= 2
        )
    }
}
