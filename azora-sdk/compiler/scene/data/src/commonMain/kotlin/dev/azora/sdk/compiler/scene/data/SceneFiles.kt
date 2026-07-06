package dev.azora.sdk.compiler.scene.data

import dev.azora.sdk.compiler.scene.domain.SceneDocument
import dev.azora.sdk.core.io.FileReadResult
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.io.ListResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.math.roundToInt

/**
 * Reads/writes `.azn` scene documents anywhere in a project.
 *
 * `.azn` files may live **anywhere** in the project — folders are only a human-readable convention,
 * not a requirement. Documents are therefore discovered by scanning the whole project tree and
 * classifying each file by its in-doc [SceneDocument.type] discriminator, never by location. The
 * discriminator *values* (and any folder/file-name conventions built on top of this object) are
 * owned by the plugin that contributes the scene type.
 *
 * A file's base name (sans extension) is each scene's identity: instance references and generated
 * file/import names all key off it.
 */
object SceneFiles {

    /** Extension of scene files. */
    const val EXT = ".azn"

    /** Conventional output directory for generated apps (pruned during discovery). */
    const val GENERATED_DIR = "generated"

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** Sanitizes [name] into a safe scene file base name. */
    fun fileName(name: String): String =
        name.trim().map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }
            .joinToString("").trim('_').ifBlank { "Untitled" }

    /** Default JSON content for a brand-new scene document of [type]. Pass [withRootContainer] for
     *  page/component-like docs so there's a root container to wire nodes into. */
    fun newDocJson(type: String, withRootContainer: Boolean): String =
        json.encodeToString(
            if (withRootContainer) SceneDocument.withRoot(type = type)
            else SceneDocument(type = type)
        )

    /** Absolute paths of every `.azn` file anywhere under the project (folders are conventional,
     *  not enforced). Descends manually so heavy/build dirs ([GENERATED_DIR], `node_modules`,
     *  `build`, `.git`, …) are pruned before being listed — important because this runs on a poll. */
    suspend fun listAllScenePaths(fs: FileSystem, projectPath: String): List<String> {
        val out = mutableListOf<String>()
        val skipDirs = setOf(GENERATED_DIR, "node_modules", "build", ".git", ".gradle", ".idea")
        suspend fun walk(dir: String) {
            when (val r = fs.listDirectory(dir)) {
                is ListResult.Success -> r.files.forEach { f ->
                    when {
                        f.isDirectory -> if (f.name !in skipDirs && !f.name.startsWith(".")) walk(f.path)
                        f.name.endsWith(EXT) -> out += f.path
                    }
                }
                is ListResult.Error -> {}
            }
        }
        walk(projectPath)
        return out
    }

    suspend fun read(fs: FileSystem, path: String): SceneDocument? {
        // The file's base name is the scene's identity (instance references and generated
        // file/import names all key off it). The in-doc `name` is empty for documents created
        // via newDocJson until they're opened and saved, so fall back to the base name here —
        // otherwise such scenes render as blank rows in instance dropdowns and collide in
        // generation.
        val baseName = path.substringAfterLast('/').removeSuffix(EXT)
        return when (val r = fs.readFromFile(path)) {
            is FileReadResult.Success -> runCatching {
                json.decodeFromString<SceneDocument>(normalizeNumericJson(migrateLegacyJson(r.content)))
                    .let { if (it.name.isBlank()) it.copy(name = baseName) else it }
            }.getOrNull()
            is FileReadResult.Error -> null
        }
    }

    suspend fun write(fs: FileSystem, path: String, doc: SceneDocument) {
        fs.writeToFile(path, json.encodeToString(doc))
    }

    /** Every document of [type] in the project, wherever its file lives. */
    suspend fun loadByType(fs: FileSystem, projectPath: String, type: String): List<SceneDocument> =
        listAllScenePaths(fs, projectPath).mapNotNull { read(fs, it) }.filter { it.type == type }

    /** File paths of documents of [type] only — for file-picker dropdowns. */
    suspend fun pathsByType(fs: FileSystem, projectPath: String, type: String): List<String> =
        listAllScenePaths(fs, projectPath).filter { path -> read(fs, path)?.type == type }

    /** Absolute path of the document of [type] identified by [name] (its in-doc name or file base
     *  name), wherever it lives in the project — scenes are discovered by scanning, not by their
     *  folder. Null if no such document exists. */
    suspend fun findPathByTypeAndName(fs: FileSystem, projectPath: String, type: String, name: String): String? =
        listAllScenePaths(fs, projectPath).firstOrNull { path ->
            val doc = read(fs, path)
            doc != null && doc.type == type &&
                (doc.name == name || path.substringAfterLast('/').removeSuffix(EXT) == name)
        }

    /**
     * Makes externally-written `.azn` JSON parseable where numbers don't match the strict schema.
     * `.azn` files are edited by tools other than Studio (AI agents, scripts), which tend to write
     * CSS-style numbers: `"opacity": 0.85` (the schema wants an integer percent 0–100) or fractional
     * px values (`"fontSize": 16.5`). Instead of failing the whole document over one such field —
     * which blanks the editor and drops the page from previews — this pass rewrites, inside
     * `modifier` objects only:
     * - `opacity` written as a fraction ≤ 1 → integer percent (`0.85` → `85`);
     * - other integer-typed fields written with a fractional part → rounded.
     * Values written as integers are left untouched (an editor-written `opacity: 85` stays 85), and
     * canvas position floats live outside `modifier` so they keep full precision.
     */
    internal fun normalizeNumericJson(content: String): String {
        val parsed = runCatching { json.parseToJsonElement(content) }.getOrNull() ?: return content
        return normalizeElement(parsed).toString()
    }

    private val modifierIntKeys =
        setOf("width", "height", "padding", "gap", "fontSize", "cornerRadius", "borderWidth")

    private fun normalizeElement(el: JsonElement): JsonElement = when (el) {
        is JsonObject -> buildJsonObject {
            el.forEach { (k, v) ->
                put(k, if (k == "modifier" && v is JsonObject) normalizeModifier(v) else normalizeElement(v))
            }
        }
        is JsonArray -> buildJsonArray { el.forEach { add(normalizeElement(it)) } }
        else -> el
    }

    /** True when the primitive was written as a non-integer number literal (`0.85`, `16.5`, `1e2`). */
    private fun JsonPrimitive.isFractionalNumber(): Boolean =
        !isString && (content.contains('.') || content.contains('e') || content.contains('E'))

    private fun roundedInt(prim: JsonPrimitive): JsonElement =
        prim.content.toDoubleOrNull()?.let { JsonPrimitive(it.roundToInt()) } ?: prim

    private fun normalizeModifier(mod: JsonObject): JsonObject = buildJsonObject {
        mod.forEach { (k, v) ->
            val prim = v as? JsonPrimitive
            put(k, when {
                k == "opacity" && prim != null && prim.isFractionalNumber() -> {
                    val d = prim.content.toDoubleOrNull()
                    when {
                        d == null -> v
                        d <= 1.0 -> JsonPrimitive((d * 100).roundToInt())
                        else -> JsonPrimitive(d.roundToInt())
                    }
                }
                k in modifierIntKeys && prim != null && prim.isFractionalNumber() -> roundedInt(prim)
                k == "corners" && v is JsonObject -> buildJsonObject {
                    v.forEach { (corner, cv) ->
                        put(corner, if (cv is JsonObject) buildJsonObject {
                            cv.forEach { (axis, av) ->
                                val ap = av as? JsonPrimitive
                                put(axis, if (ap != null && ap.isFractionalNumber()) roundedInt(ap) else av)
                            }
                        } else cv)
                    }
                }
                else -> v
            })
        }
    }

    /**
     * Converts a legacy `.azn` JSON (parent-child `root` tree + `freeNodes`, inline `children`) into
     * the pool model (`nodes` + `rootId`, containers hold `slots` referencing child ids). No-op for
     * files already in the new shape (no `"root"` key). Operates on the JSON DOM so we don't need a
     * duplicate Kotlin model of the old layout.
     */
    internal fun migrateLegacyJson(content: String): String {
        val parsed = runCatching { json.parseToJsonElement(content) }.getOrNull() ?: return content
        val obj = parsed.jsonObject
        val rootEl = obj["root"] ?: return content // already pool-shaped
        val containerTypes = setOf("column", "row", "box")
        val collected = LinkedHashMap<String, JsonObject>()

        fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.content

        fun visit(nodeEl: JsonElement) {
            val node = nodeEl.jsonObject
            val id = node["id"].str() ?: return
            if (collected.containsKey(id)) return
            if (node["type"].str() in containerTypes) {
                val children = (node["children"] as? JsonArray) ?: emptyList()
                val emptySlotIds = (node["emptySlotIds"] as? JsonArray) ?: emptyList()
                val slots = buildJsonArray {
                    children.forEachIndexed { i, child ->
                        child.jsonObject["id"].str()?.let { cid ->
                            add(buildJsonObject {
                                put("id", JsonPrimitive("s_${id}_$i"))
                                put("childId", JsonPrimitive(cid))
                            })
                        }
                    }
                    emptySlotIds.forEach { sid ->
                        sid.str()?.let { sidStr ->
                            add(buildJsonObject {
                                put("id", JsonPrimitive(sidStr))
                                put("childId", JsonNull)
                            })
                        }
                    }
                }
                collected[id] = buildJsonObject {
                    node.forEach { (k, v) -> if (k != "children" && k != "emptySlotIds") put(k, v) }
                    put("slots", slots)
                }
                children.forEach { visit(it) }
            } else {
                collected[id] = node
            }
        }

        visit(rootEl)
        (obj["freeNodes"] as? JsonArray)?.forEach { visit(it) }
        val rootId = rootEl.jsonObject["id"].str() ?: ""

        return buildJsonObject {
            obj.forEach { (k, v) -> if (k != "root" && k != "freeNodes") put(k, v) }
            put("nodes", buildJsonArray { collected.values.forEach { add(it) } })
            put("rootId", JsonPrimitive(rootId))
        }.toString()
    }
}
