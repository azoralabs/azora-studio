package dev.azora.sdk.plugin.core

/**
 * A creatable `.azscene` document type contributed by a plugin, shown in the host's "New …" menu.
 *
 * @property type The document's `type` discriminator (written into the new file).
 * @property label Human-readable name shown in the menu (e.g. "Website Page").
 */
data class AzsceneTemplate(
    val type: String,
    val label: String
)
