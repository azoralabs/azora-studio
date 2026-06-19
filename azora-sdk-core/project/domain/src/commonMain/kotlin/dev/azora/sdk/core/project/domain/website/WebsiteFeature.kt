package dev.azora.sdk.core.project.domain.website

import kotlinx.serialization.Serializable

/**
 * Optional, toggleable site capabilities. Each enabled [WebsiteFeature] lets the generator emit
 * extra wiring (e.g. a color-mode toggle for [DARK_MODE], meta tags for [SEO]).
 */
@Serializable
enum class WebFeatureType(val title: String, val description: String) {
    DARK_MODE("Dark mode", "Light/dark color modes with a toggle"),
    SEO("SEO meta tags", "Title, description and Open Graph tags per page"),
    FORMS("Forms", "Form components with validation helpers"),
    AUTH("Authentication", "Login/session scaffolding"),
    ANALYTICS("Analytics", "Page-view tracking hook"),
    MARKDOWN("Markdown", "Render markdown content pages")
}

@Serializable
data class WebsiteFeature(
    val type: WebFeatureType,
    val enabled: Boolean = false,
    val config: Map<String, String> = emptyMap()
)
