package dev.azora.canvas.domain.type

/**
 * Classification of a data-flow port on a node.
 *
 * Distinct from [dev.azora.canvas.domain.model.node.AzoraNodeDataType], which carries the runtime
 * type information used by the script interpreter. This enum is the lighter-weight presentation/
 * persistence variant used to render port circles in the canvas; the presentation layer maps each
 * value to a fixed color so users can tell port compatibility at a glance.
 *
 * - [BOOL] - boolean values.
 * - [INTEGER] - integer numbers.
 * - [REAL] - floating-point numbers.
 * - [TEXT] - strings.
 * - [ENUM] - enum cases produced by `ENUM_VALUE` nodes.
 * - [DATA_CLASS] - instances of user-defined data classes.
 * - [ANY] - wildcard; accepts any other data type.
 */
enum class AzoraPortDataType {
    BOOL,
    INTEGER,
    REAL,
    TEXT,
    ENUM,
    DATA_CLASS,
    ANY
}
