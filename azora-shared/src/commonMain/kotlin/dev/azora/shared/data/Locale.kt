package dev.azora.shared.data

/**
 * Represents the supported application locales.
 *
 * @property code ISO 639-1 language code used internally and for localization.
 * @property language Native display name of the language, used in UI.
 */
enum class Locale(
    val code: String,
    val language: String
) {

    /**
     * German language
     */
    DE("de", "Deutsch"),

    /**
     * English language
     */
    EN("en", "English"),

    /**
     * French language
     */
    FR("fr", "Français"),

    /**
     * Hungarian language
     */
    HU("hu", "Magyar"),

    /**
     * Romanian language
     */
    RO("ro", "Română")
}