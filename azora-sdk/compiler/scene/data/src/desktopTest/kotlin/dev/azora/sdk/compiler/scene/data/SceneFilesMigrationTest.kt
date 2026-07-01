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
}
