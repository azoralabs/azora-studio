package org.azora.studio.data.tilemap

import kotlinx.serialization.Serializable

@Serializable
data class TileMapModel(
    val width: Int = 16,
    val height: Int = 16,
    val tileSize: Int = 32,
    val layers: List<TileLayer> = emptyList()
)

@Serializable
data class TileLayer(
    val name: String = "Layer 0",
    val tiles: List<Int> = emptyList(),
    val visible: Boolean = true
)
