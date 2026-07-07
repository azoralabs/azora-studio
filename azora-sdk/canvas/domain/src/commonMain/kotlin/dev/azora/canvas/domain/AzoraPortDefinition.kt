package dev.azora.canvas.domain

import dev.azora.canvas.domain.model.node.AzoraNodeDataType
import dev.azora.canvas.domain.type.AzoraNodeType

/**
 * Single source of truth for the ports a given [AzoraNodeType] exposes.
 *
 * The presentation layer reads this to lay out exec triangles and data circles, and the
 * interpreter reads it to resolve port names when evaluating inputs and following outputs. Keeping
 * everything here ensures that adding a new node type only requires one edit to wire it up.
 *
 * The four functions form a fixed contract:
 * - [execInputCount] - number of execution input ports (0 for pure data nodes, typically 1 otherwise).
 * - [execOutputs] - names of execution output ports, in port-index order.
 * - [execOutputsForNode] - wraps [execOutputs] but expands dynamic outputs (currently only `MATCH`,
 *   which derives `case_0..case_n` plus `default` from its `caseCount` property).
 * - [dataInputs] - `(name, type)` pairs for data input ports, in display order.
 * - [dataOutputs] - `(name, type)` pairs for data output ports, in display order.
 *
 * Some node types declare empty input lists despite having dynamic ports (e.g. `FUNCTION_CALL`,
 * `DATA_CLASS_CREATE`); those are computed at the ViewModel level using the referenced
 * function/data class definition.
 */
object AzoraPortDefinition {

    /** Number of exec input ports for [type]. Zero for pure-data nodes; otherwise typically 1. */
    fun execInputCount(type: AzoraNodeType): Int = when (type) {
        AzoraNodeType.START -> 0
        // Pure data nodes have no exec ports
        AzoraNodeType.GET_VARIABLE,
        AzoraNodeType.GET_CONSTANT,
        AzoraNodeType.ADD,
        AzoraNodeType.SUBTRACT,
        AzoraNodeType.MULTIPLY,
        AzoraNodeType.DIVIDE,
        AzoraNodeType.MODULO,
        AzoraNodeType.NEGATE,
        AzoraNodeType.EQUAL,
        AzoraNodeType.NOT_EQUAL,
        AzoraNodeType.GREATER_THAN,
        AzoraNodeType.LESS_THAN,
        AzoraNodeType.GREATER_OR_EQUAL,
        AzoraNodeType.LESS_OR_EQUAL,
        AzoraNodeType.AND,
        AzoraNodeType.OR,
        AzoraNodeType.NOT,
        AzoraNodeType.CAST,
        AzoraNodeType.AZ_EXPR,
        AzoraNodeType.PARAM_GET,
        AzoraNodeType.ENUM_DEF,
        AzoraNodeType.ENUM_VALUE,
        AzoraNodeType.DATA_CLASS_DEF,
        AzoraNodeType.DATA_CLASS_CREATE,
        AzoraNodeType.DATA_CLASS_GET_FIELD,
        // Game Builder - pure data nodes
        AzoraNodeType.SCENE,
        AzoraNodeType.ENTITY,
        AzoraNodeType.GET_COMPONENT,
        AzoraNodeType.TRANSFORM,
        AzoraNodeType.SPRITE_RENDERER,
        AzoraNodeType.CAMERA,
        AzoraNodeType.PHYSICS_BODY,
        AzoraNodeType.COLLIDER,
        AzoraNodeType.SCRIPT,
        // Tilemap - pure data nodes
        AzoraNodeType.GET_TILE,
        AzoraNodeType.MAP_SIZE,
        AzoraNodeType.RANDOM_INT -> 0
        else -> 1
    }

    /**
     * Static names of exec output ports for [type], indexed by port number. For nodes whose outputs
     * depend on configuration (currently `MATCH`), use [execOutputsForNode] instead.
     */
    fun execOutputs(type: AzoraNodeType): List<String> = when (type) {
        AzoraNodeType.START -> listOf("exec")
        AzoraNodeType.PRINT -> listOf("exec")
        AzoraNodeType.SET_VARIABLE -> listOf("exec")
        AzoraNodeType.IF -> listOf("true", "false")
        AzoraNodeType.LOOP -> listOf("body", "completed")
        AzoraNodeType.WHILE -> listOf("body", "completed")
        AzoraNodeType.FOR_RANGE -> listOf("body", "completed")
        AzoraNodeType.AZ_CALL -> listOf("exec")
        AzoraNodeType.AZ_CODE -> listOf("exec")
        AzoraNodeType.AZ_EXPR -> emptyList()
        AzoraNodeType.PARAM_GET -> emptyList()
        AzoraNodeType.MATCH -> listOf("default") // plus dynamic case outputs
        AzoraNodeType.DELAY -> listOf("exec")
        AzoraNodeType.FUNCTION_DEF -> listOf("exec")
        AzoraNodeType.FUNCTION_CALL -> listOf("exec")
        AzoraNodeType.FUNCTION_RETURN -> emptyList()
        AzoraNodeType.DATA_CLASS_SET_FIELD -> listOf("exec")
        // Pure data nodes have no exec outputs
        AzoraNodeType.GET_VARIABLE,
        AzoraNodeType.GET_CONSTANT,
        AzoraNodeType.ADD,
        AzoraNodeType.SUBTRACT,
        AzoraNodeType.MULTIPLY,
        AzoraNodeType.DIVIDE,
        AzoraNodeType.MODULO,
        AzoraNodeType.NEGATE,
        AzoraNodeType.EQUAL,
        AzoraNodeType.NOT_EQUAL,
        AzoraNodeType.GREATER_THAN,
        AzoraNodeType.LESS_THAN,
        AzoraNodeType.GREATER_OR_EQUAL,
        AzoraNodeType.LESS_OR_EQUAL,
        AzoraNodeType.AND,
        AzoraNodeType.OR,
        AzoraNodeType.NOT,
        AzoraNodeType.CAST,
        AzoraNodeType.ENUM_DEF,
        AzoraNodeType.ENUM_VALUE,
        AzoraNodeType.DATA_CLASS_DEF,
        AzoraNodeType.DATA_CLASS_CREATE,
        AzoraNodeType.DATA_CLASS_GET_FIELD -> emptyList()
        // Game Builder
        AzoraNodeType.SPAWN_ENTITY -> listOf("exec")
        AzoraNodeType.DESTROY_ENTITY -> listOf("exec")
        AzoraNodeType.ADD_COMPONENT -> listOf("exec")
        AzoraNodeType.REMOVE_COMPONENT -> listOf("exec")
        AzoraNodeType.SYSTEM_UPDATE -> listOf("exec")
        AzoraNodeType.RENDER_SYSTEM -> listOf("exec")
        AzoraNodeType.PHYSICS_SYSTEM -> listOf("exec")
        AzoraNodeType.INPUT_SYSTEM -> listOf("exec")
        AzoraNodeType.ON_START -> listOf("exec")
        AzoraNodeType.ON_UPDATE -> listOf("exec")
        AzoraNodeType.INPUT_EVENT -> listOf("pressed", "released")
        AzoraNodeType.ON_COLLISION -> listOf("enter", "exit")
        AzoraNodeType.TIMER -> listOf("tick", "completed")
        AzoraNodeType.SCENE,
        AzoraNodeType.ENTITY,
        AzoraNodeType.GET_COMPONENT,
        AzoraNodeType.TRANSFORM,
        AzoraNodeType.SPRITE_RENDERER,
        AzoraNodeType.CAMERA,
        AzoraNodeType.PHYSICS_BODY,
        AzoraNodeType.COLLIDER,
        AzoraNodeType.SCRIPT -> emptyList()
        // Audio
        AzoraNodeType.PLAY_NOTE -> listOf("exec")
        AzoraNodeType.SET_BPM -> listOf("exec")
        AzoraNodeType.SET_VOLUME -> listOf("exec")
        AzoraNodeType.REST -> listOf("exec")
        // Tilemap
        AzoraNodeType.SET_TILE -> listOf("exec")
        AzoraNodeType.FILL_RECT -> listOf("exec")
        AzoraNodeType.CLEAR_MAP -> listOf("exec")
        AzoraNodeType.GET_TILE,
        AzoraNodeType.MAP_SIZE,
        AzoraNodeType.RANDOM_INT -> emptyList()
    }

    /**
     * Like [execOutputs] but resolves dynamic outputs from a node's [properties].
     *
     * For `MATCH` this expands the `caseCount` property into `case_0`, `case_1`, ..., followed by
     * a final `default` branch (defaulting to 2 cases when the property is missing or malformed).
     */
    fun execOutputsForNode(type: AzoraNodeType, properties: Map<String, String>): List<String> {
        if (type == AzoraNodeType.MATCH) {
            val caseCount = properties["caseCount"]?.toIntOrNull() ?: 2
            return (0 until caseCount).map { "case_$it" } + "default"
        }
        return execOutputs(type)
    }

    /**
     * Static `(name, type)` data input ports for [type] in display order.
     *
     * Some types (`FUNCTION_CALL`, `DATA_CLASS_CREATE`) return an empty list here even though they
     * have inputs at runtime - those are derived from the referenced function/data class definition
     * by the ViewModel layer, since this module doesn't have access to that context.
     *
     * @param properties Reserved for future per-node configuration. Currently unused but kept in the
     *   signature so callers can pass [AzoraNodeModel.properties] consistently with [dataOutputs].
     */
    fun dataInputs(type: AzoraNodeType, properties: Map<String, String>): List<Pair<String, AzoraNodeDataType>> = when (type) {
        AzoraNodeType.START -> emptyList()
        AzoraNodeType.GET_VARIABLE -> emptyList()
        AzoraNodeType.GET_CONSTANT -> emptyList()
        AzoraNodeType.SET_VARIABLE -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.PRINT -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.IF -> listOf("condition" to AzoraNodeDataType.BOOLEAN)
        AzoraNodeType.LOOP -> listOf("count" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.WHILE -> listOf("condition" to AzoraNodeDataType.BOOLEAN)
        AzoraNodeType.FOR_RANGE -> listOf(
            "from" to AzoraNodeDataType.INTEGER,
            "to" to AzoraNodeDataType.INTEGER
        )
        // Dynamic argument inputs derived from the `argCount` property.
        AzoraNodeType.AZ_CALL -> {
            val argCount = properties["argCount"]?.toIntOrNull() ?: 0
            (0 until argCount).map { "arg_$it" to AzoraNodeDataType.ANY }
        }
        AzoraNodeType.AZ_EXPR -> emptyList()
        AzoraNodeType.AZ_CODE -> emptyList()
        AzoraNodeType.PARAM_GET -> emptyList()
        AzoraNodeType.MATCH -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.DELAY -> listOf("milliseconds" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.ADD,
        AzoraNodeType.SUBTRACT,
        AzoraNodeType.MULTIPLY,
        AzoraNodeType.DIVIDE,
        AzoraNodeType.MODULO -> listOf("a" to AzoraNodeDataType.ANY, "b" to AzoraNodeDataType.ANY)
        AzoraNodeType.NEGATE -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.EQUAL,
        AzoraNodeType.NOT_EQUAL,
        AzoraNodeType.GREATER_THAN,
        AzoraNodeType.LESS_THAN,
        AzoraNodeType.GREATER_OR_EQUAL,
        AzoraNodeType.LESS_OR_EQUAL -> listOf("a" to AzoraNodeDataType.ANY, "b" to AzoraNodeDataType.ANY)
        AzoraNodeType.AND,
        AzoraNodeType.OR -> listOf("a" to AzoraNodeDataType.BOOLEAN, "b" to AzoraNodeDataType.BOOLEAN)
        AzoraNodeType.NOT -> listOf("value" to AzoraNodeDataType.BOOLEAN)
        AzoraNodeType.CAST -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.FUNCTION_DEF -> emptyList()
        AzoraNodeType.FUNCTION_CALL -> {
            // Dynamic inputs based on function parameters - handled at ViewModel level
            emptyList()
        }
        AzoraNodeType.FUNCTION_RETURN -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.ENUM_DEF -> emptyList()
        AzoraNodeType.ENUM_VALUE -> emptyList()
        AzoraNodeType.DATA_CLASS_DEF -> emptyList()
        AzoraNodeType.DATA_CLASS_CREATE -> {
            // Dynamic inputs based on class fields - handled at ViewModel level
            emptyList()
        }
        AzoraNodeType.DATA_CLASS_GET_FIELD -> listOf("instance" to AzoraNodeDataType.DATA_CLASS)
        AzoraNodeType.DATA_CLASS_SET_FIELD -> listOf(
            "instance" to AzoraNodeDataType.DATA_CLASS,
            "value" to AzoraNodeDataType.ANY
        )
        // Game Builder
        AzoraNodeType.SCENE -> emptyList()
        AzoraNodeType.ENTITY -> listOf("name" to AzoraNodeDataType.STRING)
        AzoraNodeType.SPAWN_ENTITY -> listOf("name" to AzoraNodeDataType.STRING)
        AzoraNodeType.DESTROY_ENTITY -> listOf("entityId" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.ADD_COMPONENT -> listOf(
            "entityId" to AzoraNodeDataType.INTEGER,
            "component" to AzoraNodeDataType.ANY
        )
        AzoraNodeType.GET_COMPONENT -> listOf("entityId" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.REMOVE_COMPONENT -> listOf("entityId" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.TRANSFORM -> listOf(
            "posX" to AzoraNodeDataType.REAL,
            "posY" to AzoraNodeDataType.REAL,
            "posZ" to AzoraNodeDataType.REAL,
            "rotX" to AzoraNodeDataType.REAL,
            "rotY" to AzoraNodeDataType.REAL,
            "rotZ" to AzoraNodeDataType.REAL,
            "scaleX" to AzoraNodeDataType.REAL,
            "scaleY" to AzoraNodeDataType.REAL,
            "scaleZ" to AzoraNodeDataType.REAL
        )
        AzoraNodeType.SPRITE_RENDERER -> listOf(
            "texturePath" to AzoraNodeDataType.STRING,
            "width" to AzoraNodeDataType.REAL,
            "height" to AzoraNodeDataType.REAL,
            "color" to AzoraNodeDataType.STRING
        )
        AzoraNodeType.CAMERA -> listOf(
            "fov" to AzoraNodeDataType.REAL,
            "near" to AzoraNodeDataType.REAL,
            "far" to AzoraNodeDataType.REAL,
            "orthoSize" to AzoraNodeDataType.REAL
        )
        AzoraNodeType.PHYSICS_BODY -> listOf(
            "mass" to AzoraNodeDataType.REAL,
            "gravity" to AzoraNodeDataType.BOOLEAN,
            "kinematic" to AzoraNodeDataType.BOOLEAN
        )
        AzoraNodeType.COLLIDER -> listOf(
            "width" to AzoraNodeDataType.REAL,
            "height" to AzoraNodeDataType.REAL,
            "isTrigger" to AzoraNodeDataType.BOOLEAN
        )
        AzoraNodeType.SCRIPT -> listOf("scriptName" to AzoraNodeDataType.STRING)
        AzoraNodeType.SYSTEM_UPDATE -> listOf("deltaTime" to AzoraNodeDataType.REAL)
        AzoraNodeType.RENDER_SYSTEM -> emptyList()
        AzoraNodeType.PHYSICS_SYSTEM -> listOf("deltaTime" to AzoraNodeDataType.REAL)
        AzoraNodeType.INPUT_SYSTEM -> emptyList()
        AzoraNodeType.INPUT_EVENT -> listOf("key" to AzoraNodeDataType.STRING)
        AzoraNodeType.ON_COLLISION -> listOf("entityId" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.TIMER -> listOf(
            "duration" to AzoraNodeDataType.REAL,
            "repeat" to AzoraNodeDataType.BOOLEAN
        )
        AzoraNodeType.ON_START -> emptyList()
        AzoraNodeType.ON_UPDATE -> emptyList()
        // Audio
        AzoraNodeType.PLAY_NOTE -> listOf(
            "note" to AzoraNodeDataType.STRING,
            "duration" to AzoraNodeDataType.INTEGER,
            "velocity" to AzoraNodeDataType.INTEGER
        )
        AzoraNodeType.SET_BPM -> listOf("bpm" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.SET_VOLUME -> listOf("volume" to AzoraNodeDataType.REAL)
        AzoraNodeType.REST -> listOf("duration" to AzoraNodeDataType.INTEGER)
        // Tilemap
        AzoraNodeType.SET_TILE -> listOf(
            "x" to AzoraNodeDataType.INTEGER,
            "y" to AzoraNodeDataType.INTEGER,
            "colorIndex" to AzoraNodeDataType.INTEGER
        )
        AzoraNodeType.GET_TILE -> listOf(
            "x" to AzoraNodeDataType.INTEGER,
            "y" to AzoraNodeDataType.INTEGER
        )
        AzoraNodeType.FILL_RECT -> listOf(
            "x1" to AzoraNodeDataType.INTEGER,
            "y1" to AzoraNodeDataType.INTEGER,
            "x2" to AzoraNodeDataType.INTEGER,
            "y2" to AzoraNodeDataType.INTEGER,
            "colorIndex" to AzoraNodeDataType.INTEGER
        )
        AzoraNodeType.CLEAR_MAP -> emptyList()
        AzoraNodeType.MAP_SIZE -> emptyList()
        AzoraNodeType.RANDOM_INT -> listOf(
            "min" to AzoraNodeDataType.INTEGER,
            "max" to AzoraNodeDataType.INTEGER
        )
    }

    /**
     * Static `(name, type)` data output ports for [type] in display order.
     *
     * Most output types are fixed by [type] alone, but `CAST` reads its `toType` property to report
     * the runtime-selected target type so the editor colors the output port correctly.
     */
    fun dataOutputs(type: AzoraNodeType, properties: Map<String, String>): List<Pair<String, AzoraNodeDataType>> = when (type) {
        AzoraNodeType.START -> emptyList()
        AzoraNodeType.GET_VARIABLE -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.GET_CONSTANT -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.SET_VARIABLE -> emptyList()
        AzoraNodeType.PRINT -> emptyList()
        AzoraNodeType.IF -> emptyList()
        AzoraNodeType.LOOP -> listOf("index" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.WHILE -> emptyList()
        AzoraNodeType.FOR_RANGE -> listOf("index" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.AZ_CALL -> listOf("result" to AzoraNodeDataType.ANY)
        AzoraNodeType.AZ_EXPR -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.AZ_CODE -> emptyList()
        AzoraNodeType.PARAM_GET -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.MATCH -> emptyList()
        AzoraNodeType.DELAY -> emptyList()
        AzoraNodeType.ADD,
        AzoraNodeType.SUBTRACT,
        AzoraNodeType.MULTIPLY,
        AzoraNodeType.DIVIDE,
        AzoraNodeType.MODULO -> listOf("result" to AzoraNodeDataType.ANY)
        AzoraNodeType.NEGATE -> listOf("result" to AzoraNodeDataType.ANY)
        AzoraNodeType.EQUAL,
        AzoraNodeType.NOT_EQUAL,
        AzoraNodeType.GREATER_THAN,
        AzoraNodeType.LESS_THAN,
        AzoraNodeType.GREATER_OR_EQUAL,
        AzoraNodeType.LESS_OR_EQUAL -> listOf("result" to AzoraNodeDataType.BOOLEAN)
        AzoraNodeType.AND,
        AzoraNodeType.OR -> listOf("result" to AzoraNodeDataType.BOOLEAN)
        AzoraNodeType.NOT -> listOf("result" to AzoraNodeDataType.BOOLEAN)
        AzoraNodeType.CAST -> {
            val toType = properties["toType"]?.let { name ->
                AzoraNodeDataType.entries.find { it.name == name }
            } ?: AzoraNodeDataType.ANY
            listOf("result" to toType)
        }
        AzoraNodeType.FUNCTION_DEF -> emptyList()
        AzoraNodeType.FUNCTION_CALL -> listOf("result" to AzoraNodeDataType.ANY)
        AzoraNodeType.FUNCTION_RETURN -> emptyList()
        AzoraNodeType.ENUM_DEF -> emptyList()
        AzoraNodeType.ENUM_VALUE -> listOf("value" to AzoraNodeDataType.ENUM)
        AzoraNodeType.DATA_CLASS_DEF -> emptyList()
        AzoraNodeType.DATA_CLASS_CREATE -> listOf("instance" to AzoraNodeDataType.DATA_CLASS)
        AzoraNodeType.DATA_CLASS_GET_FIELD -> listOf("value" to AzoraNodeDataType.ANY)
        AzoraNodeType.DATA_CLASS_SET_FIELD -> emptyList()
        // Game Builder
        AzoraNodeType.SCENE -> listOf("sceneId" to AzoraNodeDataType.STRING)
        AzoraNodeType.ENTITY -> listOf("entityId" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.SPAWN_ENTITY -> listOf("entityId" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.DESTROY_ENTITY -> emptyList()
        AzoraNodeType.ADD_COMPONENT -> emptyList()
        AzoraNodeType.GET_COMPONENT -> listOf("component" to AzoraNodeDataType.ANY)
        AzoraNodeType.REMOVE_COMPONENT -> emptyList()
        AzoraNodeType.TRANSFORM -> listOf("transform" to AzoraNodeDataType.ANY)
        AzoraNodeType.SPRITE_RENDERER -> listOf("renderer" to AzoraNodeDataType.ANY)
        AzoraNodeType.CAMERA -> listOf("camera" to AzoraNodeDataType.ANY)
        AzoraNodeType.PHYSICS_BODY -> listOf("body" to AzoraNodeDataType.ANY)
        AzoraNodeType.COLLIDER -> listOf("collider" to AzoraNodeDataType.ANY)
        AzoraNodeType.SCRIPT -> listOf("script" to AzoraNodeDataType.ANY)
        AzoraNodeType.SYSTEM_UPDATE -> emptyList()
        AzoraNodeType.RENDER_SYSTEM -> emptyList()
        AzoraNodeType.PHYSICS_SYSTEM -> emptyList()
        AzoraNodeType.INPUT_SYSTEM -> emptyList()
        AzoraNodeType.INPUT_EVENT -> listOf("keyCode" to AzoraNodeDataType.STRING)
        AzoraNodeType.ON_COLLISION -> listOf("otherId" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.TIMER -> listOf("elapsed" to AzoraNodeDataType.REAL)
        AzoraNodeType.ON_START -> emptyList()
        AzoraNodeType.ON_UPDATE -> listOf("deltaTime" to AzoraNodeDataType.REAL)
        // Audio
        AzoraNodeType.PLAY_NOTE,
        AzoraNodeType.SET_BPM,
        AzoraNodeType.SET_VOLUME,
        AzoraNodeType.REST -> emptyList()
        // Tilemap
        AzoraNodeType.SET_TILE -> emptyList()
        AzoraNodeType.GET_TILE -> listOf("colorIndex" to AzoraNodeDataType.INTEGER)
        AzoraNodeType.FILL_RECT -> emptyList()
        AzoraNodeType.CLEAR_MAP -> emptyList()
        AzoraNodeType.MAP_SIZE -> listOf(
            "width" to AzoraNodeDataType.INTEGER,
            "height" to AzoraNodeDataType.INTEGER
        )
        AzoraNodeType.RANDOM_INT -> listOf("value" to AzoraNodeDataType.INTEGER)
    }
}