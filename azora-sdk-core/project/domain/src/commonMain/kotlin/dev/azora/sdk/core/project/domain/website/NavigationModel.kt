package dev.azora.sdk.core.project.domain.website

import kotlinx.serialization.Serializable

@Serializable
enum class NavStyle { TOP_BAR, SIDEBAR }

/**
 * One entry in the site navigation, linking a [label] to a [route] (which should match a
 * [WebsitePage.route]).
 */
@Serializable
data class NavigationItem(
    val id: String = randomComponentId(),
    val label: String,
    val route: String
)

/**
 * Site-wide navigation. The generator emits a reusable navigation composable from [items] and
 * [style], shared across every generated page.
 */
@Serializable
data class NavigationModel(
    val style: NavStyle = NavStyle.TOP_BAR,
    val brand: String = "",
    val items: List<NavigationItem> = emptyList()
)
