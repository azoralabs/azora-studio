package org.azora.studio.assets

/**
 * Represents an item in the Assets panel tree view.
 */
sealed class AssetItem(
    open val path: String,
    open val name: String
) {
    /**
     * A folder that can contain other items.
     */
    data class Folder(
        override val path: String,
        override val name: String,
        val children: List<AssetItem> = emptyList()
    ) : AssetItem(path, name)

    /**
     * A file with an extension.
     */
    data class File(
        override val path: String,
        override val name: String,
        val extension: String
    ) : AssetItem(path, name)
}
