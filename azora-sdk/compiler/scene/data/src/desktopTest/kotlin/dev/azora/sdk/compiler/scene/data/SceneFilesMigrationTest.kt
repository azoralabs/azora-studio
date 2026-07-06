package dev.azora.sdk.compiler.scene.data

import dev.azora.sdk.compiler.scene.domain.SceneColumn
import dev.azora.sdk.compiler.scene.domain.SceneDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Verifies the legacy (`root` tree + inline `children`) → pool (`nodes` + `rootId` + slot refs) migration. */
class SceneFilesMigrationTest {

    private val legacy = """
        {
          "type": "azora-website-page",
          "name": "Home",
          "route": "/",
          "root": {
            "type": "column",
            "id": "c_root",
            "modifier": { "fillMaxWidth": true, "padding": 48 },
            "arrangement": "START",
            "children": [
              { "type": "text", "id": "c_t1", "modifier": {}, "text": "hi" },
              { "type": "text", "id": "c_t2", "modifier": {}, "text": "bye" }
            ]
          },
          "positions": {},
          "instances": {},
          "nav": [],
          "settings": {}
        }
    """.trimIndent()

    @Test
    fun `legacy root tree migrates to a pool with ordered slots`() {
        val doc = SceneFiles.json.decodeFromString<SceneDocument>(SceneFiles.migrateLegacyJson(legacy))

        assertEquals("c_root", doc.rootId)
        // root + two text children, all flattened into the pool.
        assertEquals(3, doc.nodes.size)
        val root = doc.nodes.first { it.id == "c_root" } as SceneColumn
        assertEquals(2, root.slots.size)
        // Children preserve order as slot references.
        assertEquals("c_t1", root.slots[0].childId)
        assertEquals("c_t2", root.slots[1].childId)
        assertNotNull(doc.nodes.firstOrNull { it.id == "c_t1" })
        assertNotNull(doc.nodes.firstOrNull { it.id == "c_t2" })
    }

    @Test
    fun `legacy emptySlotIds become empty slots in order`() {
        val withEmpties = legacy.replace("\"children\": [", "\"emptySlotIds\": [\"s_a\", \"s_b\"], \"children\": [")
        val doc = SceneFiles.json.decodeFromString<SceneDocument>(SceneFiles.migrateLegacyJson(withEmpties))
        val root = doc.nodes.first { it.id == "c_root" } as SceneColumn
        // Two occupied (children) + two empty.
        assertEquals(4, root.slots.size)
        assertEquals("c_t1", root.slots[0].childId)
        assertEquals("c_t2", root.slots[1].childId)
        assertEquals("s_a", root.slots[2].id)
        assertEquals(null, root.slots[2].childId)
        assertEquals(null, root.slots[3].childId)
    }

    @Test
    fun `legacy freeNodes are folded into the pool`() {
        val withFree = legacy.replaceFirst(
            "\"positions\": {}",
            "\"freeNodes\": [{ \"type\": \"spacer\", \"id\": \"c_free\", \"modifier\": {} }], \"positions\": {}"
        )
        val doc = SceneFiles.json.decodeFromString<SceneDocument>(SceneFiles.migrateLegacyJson(withFree))
        assertNotNull(doc.nodes.firstOrNull { it.id == "c_free" }, "free node should be in the pool")
    }

    @Test
    fun `already pool-shaped json passes through unchanged`() {
        val pool = """
            { "type": "azora-website-page", "name": "H", "route": "/",
              "nodes": [], "rootId": "", "positions": {}, "instances": {}, "nav": [], "settings": {} }
        """.trimIndent()
        val out = SceneFiles.migrateLegacyJson(pool)
        assertFalse("\"root\":" in out, "pool-shaped input must not gain a root key, got:\n$out")
        assertTrue("\"rootId\":" in out)
    }

    // AI agents/scripts write CSS-style numbers into .azn modifiers (opacity 0.85, fontSize 16.5).
    // The schema wants ints (opacity is a percent), and one bad field used to fail the whole doc.
    @Test
    fun `externally written fractional numbers are normalized to schema ints`() {
        val agentWritten = """
            { "type": "azora-website-page", "name": "Home", "route": "/",
              "nodes": [
                { "type": "column", "id": "c_root", "modifier": {}, "arrangement": "START",
                  "slots": [ { "id": "s_1", "childId": "c_t1", "reroutePoints": [] } ] },
                { "type": "text", "id": "c_t1",
                  "modifier": { "opacity": 0.85, "fontSize": 16.5, "padding": 12.0, "textColor": "#FFFFFF" },
                  "text": "hi" }
              ],
              "rootId": "c_root",
              "positions": { "c_root": { "x": 10.5, "y": 4.25 } },
              "instances": {}, "nav": [], "settings": {} }
        """.trimIndent()

        val doc = SceneFiles.json.decodeFromString<SceneDocument>(
            SceneFiles.normalizeNumericJson(agentWritten)
        )
        val text = doc.nodes.first { it.id == "c_t1" }
        assertEquals(85, text.modifier.opacity)
        assertEquals(17, text.modifier.fontSize)
        assertEquals(12, text.modifier.padding)
        // Canvas positions live outside modifiers and keep their float precision.
        assertEquals(10.5f, doc.positions["c_root"]?.x)
    }

    @Test
    fun `integer opacity written by the editor is left untouched`() {
        val editorWritten = """
            { "type": "azora-website-page", "name": "H", "route": "/",
              "nodes": [ { "type": "text", "id": "c_t", "modifier": { "opacity": 85 }, "text": "x" } ],
              "rootId": "c_t", "positions": {}, "instances": {}, "nav": [], "settings": {} }
        """.trimIndent()
        val doc = SceneFiles.json.decodeFromString<SceneDocument>(
            SceneFiles.normalizeNumericJson(editorWritten)
        )
        assertEquals(85, doc.nodes.first().modifier.opacity)
    }
}
