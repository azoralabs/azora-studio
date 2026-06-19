package dev.azora.sdk.core.project.domain.website

import kotlinx.serialization.Serializable

/**
 * A single page/route in the site.
 *
 * @property route SPA route, e.g. `/` or `/about`.
 * @property title Document/`<title>` text and nav label source.
 * @property isHome Whether this is the index route.
 * @property root The page's visual component tree (drives codegen + the Phase 2 renderer).
 * @property stateKeys Page-local string state variables (key -> initial value) referenced by
 *   inputs and [SetStateAction]/[CallApiAction] results.
 * @property logic Codegen-facing logic IR for this page's event handlers.
 * @property logicGraphRef Optional path to the authored AzoraNodes (`.azn`) graph backing [logic]
 *   (wired in Phase 2; the generator only reads [logic]).
 */
@Serializable
data class WebsitePage(
    val id: String = randomComponentId(),
    val name: String,
    val route: String,
    val title: String,
    val isHome: Boolean = false,
    val root: WebComponent = WebColumn(),
    val stateKeys: Map<String, String> = emptyMap(),
    val logic: WebLogicGraph = WebLogicGraph(),
    val logicGraphRef: String? = null
)

/**
 * The complete, serializable description of a website built in Azora Studio.
 *
 * Persisted inside the project's [ProjectSettings.extras][dev.azora.sdk.core.project.domain.ProjectSettings]
 * (see `WebsiteSettingsExtensions`) and consumed by the Kobweb generator and the Studio website
 * panels. This is the single source of truth for the whole website workflow.
 */
@Serializable
data class WebsiteModel(
    val pages: List<WebsitePage> = emptyList(),
    val navigation: NavigationModel = NavigationModel(),
    val apiBaseUrl: String = "",
    val apiEndpoints: List<ApiEndpoint> = emptyList(),
    val features: List<WebsiteFeature> = emptyList(),
    val theme: WebTheme = WebTheme()
) {
    val homePage: WebsitePage? get() = pages.firstOrNull { it.isHome } ?: pages.firstOrNull()

    fun isFeatureEnabled(type: WebFeatureType): Boolean =
        features.any { it.type == type && it.enabled }

    fun endpoint(id: String): ApiEndpoint? = apiEndpoints.firstOrNull { it.id == id }

    companion object {
        /**
         * A ready-to-run starter site: a Home and an About page, a top navigation bar, one example
         * API endpoint, and a logic handler that exercises navigation — so a freshly created
         * Website project generates a non-empty, multi-page, buildable Kobweb app.
         */
        fun default(projectName: String): WebsiteModel {
            val brand = projectName.ifBlank { "Azora Site" }

            val getStartedHandler = WebEventHandler(
                name = "goToAbout",
                event = WebEventType.CLICK,
                actions = listOf(
                    LogAction("Get started clicked"),
                    NavigateAction("/about")
                )
            )

            val homePage = WebsitePage(
                name = "Home",
                route = "/",
                title = brand,
                isHome = true,
                logic = WebLogicGraph(handlers = listOf(getStartedHandler)),
                root = WebColumn(
                    modifier = WebModifier(fillMaxWidth = true, padding = 48, gap = 16),
                    arrangement = WebArrangement.CENTER,
                    children = listOf(
                        WebText(
                            text = "Welcome to $brand",
                            modifier = WebModifier(fontSize = 40, fontWeight = WebFontWeight.BOLD)
                        ),
                        WebText(
                            text = "Built visually with Azora Studio.",
                            modifier = WebModifier(fontSize = 18, textColor = "#9CA3AF")
                        ),
                        WebButton(
                            label = "Get started",
                            onClickHandlerId = getStartedHandler.id
                        )
                    )
                )
            )

            val aboutPage = WebsitePage(
                name = "About",
                route = "/about",
                title = "About · $brand",
                root = WebColumn(
                    modifier = WebModifier(fillMaxWidth = true, padding = 48, gap = 12),
                    children = listOf(
                        WebText(
                            text = "About",
                            modifier = WebModifier(fontSize = 32, fontWeight = WebFontWeight.BOLD)
                        ),
                        WebText(text = "This page was generated from the Azora website model."),
                        WebLink(text = "Back home", href = "/")
                    )
                )
            )

            return WebsiteModel(
                pages = listOf(homePage, aboutPage),
                navigation = NavigationModel(
                    style = NavStyle.TOP_BAR,
                    brand = brand,
                    items = listOf(
                        NavigationItem(label = "Home", route = "/"),
                        NavigationItem(label = "About", route = "/about")
                    )
                ),
                apiBaseUrl = "https://api.example.com",
                apiEndpoints = listOf(
                    ApiEndpoint(
                        name = "getStatus",
                        method = HttpMethod.GET,
                        path = "/status",
                        responseType = "String"
                    )
                ),
                features = WebFeatureType.entries.map { type ->
                    WebsiteFeature(type = type, enabled = type == WebFeatureType.DARK_MODE)
                },
                theme = WebTheme()
            )
        }
    }
}
