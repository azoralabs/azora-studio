package dev.azora.canvas.domain.type

import kotlinx.serialization.Serializable

/**
 * Identifies the kind of node placed on the canvas and groups it for the node-picker.
 *
 * Each entry combines:
 * - [label] — human-readable name displayed in the node header and palette.
 * - [category] — palette section used to group related nodes (e.g. "Math", "Logic").
 *
 * Node types fall into two broad families that affect how [AzoraPortDefinition] generates ports:
 * - **Exec nodes** — have execution input/output ports and run side-effects in order
 *   (e.g. [START], [PRINT], [IF], [LOOP], [SET_VARIABLE]).
 * - **Pure data nodes** — have no exec ports and are evaluated on demand to produce a value
 *   (e.g. [GET_VARIABLE], [ADD], [EQUAL], [DATA_CLASS_GET_FIELD]).
 *
 * The Game Builder, Audio and Tilemap groups extend the base scripting set with domain-specific nodes.
 */
@Serializable
enum class AzoraNodeType(
    /** Human-readable name shown in node headers and the palette. */
    val label: String,
    /** Palette section used to group related node types. */
    val category: String
) {
    // Flow
    START("Start", "Flow"),
    // Variables
    GET_VARIABLE("Get Variable", "Variables"),
    SET_VARIABLE("Set Variable", "Variables"),
    GET_CONSTANT("Get Constant", "Variables"),
    // Output
    PRINT("Print", "Output"),
    // Control Flow
    IF("If", "Control Flow"),
    LOOP("Loop", "Control Flow"),
    MATCH("Match", "Control Flow"),
    DELAY("Delay", "Control Flow"),
    // Arithmetic
    ADD("Add", "Math"),
    SUBTRACT("Subtract", "Math"),
    MULTIPLY("Multiply", "Math"),
    DIVIDE("Divide", "Math"),
    MODULO("Modulo", "Math"),
    NEGATE("Negate", "Math"),
    // Comparison
    EQUAL("Equal", "Comparison"),
    NOT_EQUAL("Not Equal", "Comparison"),
    GREATER_THAN("Greater Than", "Comparison"),
    LESS_THAN("Less Than", "Comparison"),
    GREATER_OR_EQUAL("Greater Or Equal", "Comparison"),
    LESS_OR_EQUAL("Less Or Equal", "Comparison"),
    // Logic
    AND("And", "Logic"),
    OR("Or", "Logic"),
    NOT("Not", "Logic"),
    // Cast
    CAST("Cast", "Cast"),
    // Functions
    FUNCTION_DEF("Function", "Functions"),
    FUNCTION_CALL("Call Function", "Functions"),
    FUNCTION_RETURN("Return", "Functions"),
    // Enums
    ENUM_DEF("Enum", "Enums"),
    ENUM_VALUE("Enum Value", "Enums"),
    // Data Classes
    DATA_CLASS_DEF("Data Class", "Data Classes"),
    DATA_CLASS_CREATE("Create", "Data Classes"),
    DATA_CLASS_GET_FIELD("Get Field", "Data Classes"),
    DATA_CLASS_SET_FIELD("Set Field", "Data Classes"),

    // Game Builder - ECS
    SCENE("Scene", "Game ECS"),
    ENTITY("Entity", "Game ECS"),
    SPAWN_ENTITY("Spawn Entity", "Game ECS"),
    DESTROY_ENTITY("Destroy Entity", "Game ECS"),
    ADD_COMPONENT("Add Component", "Game ECS"),
    GET_COMPONENT("Get Component", "Game ECS"),
    REMOVE_COMPONENT("Remove Component", "Game ECS"),

    // Game Builder - Components
    TRANSFORM("Transform", "Game Components"),
    SPRITE_RENDERER("Sprite Renderer", "Game Components"),
    CAMERA("Camera", "Game Components"),
    PHYSICS_BODY("Physics Body", "Game Components"),
    COLLIDER("Collider", "Game Components"),
    SCRIPT("Script", "Game Components"),

    // Game Builder - Systems
    SYSTEM_UPDATE("System Update", "Game Systems"),
    RENDER_SYSTEM("Render System", "Game Systems"),
    PHYSICS_SYSTEM("Physics System", "Game Systems"),
    INPUT_SYSTEM("Input System", "Game Systems"),

    // Game Builder - Events
    INPUT_EVENT("Input Event", "Game Events"),
    ON_COLLISION("On Collision", "Game Events"),
    TIMER("Timer", "Game Events"),
    ON_START("On Start", "Game Events"),
    ON_UPDATE("On Update", "Game Events"),

    // Audio
    PLAY_NOTE("Play Note", "Audio"),
    SET_BPM("Set BPM", "Audio"),
    SET_VOLUME("Set Volume", "Audio"),
    REST("Rest", "Audio"),

    // Tilemap
    SET_TILE("Set Tile", "Tilemap"),
    GET_TILE("Get Tile", "Tilemap"),
    FILL_RECT("Fill Rect", "Tilemap"),
    CLEAR_MAP("Clear Map", "Tilemap"),
    MAP_SIZE("Map Size", "Tilemap"),
    RANDOM_INT("Random Int", "Tilemap"),
}