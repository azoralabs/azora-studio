package dev.azora.sdk.core.project.domain

import kotlinx.serialization.Serializable

/**
 * Starting template selected when creating a new project.
 *
 * @property label Human-readable name shown in the create-project UI.
 * @property description Short explanation of what the template scaffolds.
 * @property supportsOptionalServer Whether the optional "Include Ktor server" toggle
 *   applies. [SERVER] is itself a Ktor server, so the toggle is hidden for it.
 */
@Serializable
enum class ProjectTemplate(
    val label: String,
    val description: String,
    val supportsOptionalServer: Boolean = true
) {
    EMPTY("Empty", "Blank project with no predefined targets", supportsOptionalServer = false),
    MOBILE("Mobile", "Kotlin Multiplatform app for Android & iOS"),
    DESKTOP("Desktop", "Kotlin Multiplatform desktop (JVM) app"),
    WEB("Web", "Kotlin Multiplatform web (Wasm) app"),
    MULTIPLATFORM("Multiplatform", "KMP app targeting mobile, desktop & web"),
    WEBSITE("Website", "Kobweb website (static / server-rendered)"),
    SERVER("Server", "Ktor backend server only", supportsOptionalServer = false),
    AUDIO("Audio", "Strudel live-coding music & audio environment", supportsOptionalServer = false),
    PIXEL("Pixel", "2D image, vector & animation editor (Photoshop / Aseprite style)", supportsOptionalServer = false);
}
